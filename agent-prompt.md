# Agent Prompt: Implement Node-Edge Process Engine

## Context

I'm building a multi-tenant SaaS platform using Spring Boot + MongoDB for small/medium businesses. The platform lets business owners create domains, register users, manage applications, and build custom workflows via drag-and-drop.

The attached `workflow-architecture.md` contains the full architecture proposal. Read it completely before writing any code.

## Existing Tech Stack

- **Java 17+** with Spring Boot 3.x
- **MongoDB** as the primary database (Spring Data MongoDB)
- **JWT-based auth** with two principal types: `OWNER` and `DOMAIN_USER`
- **RBAC** with domain-level and app-level permissions
- **Existing entities**: `Domain`, `Owner`, `DomainUser`, `Application`, `DomainModel`, `DomainGroup`, `DomainGroupMember`
- **Existing services**: `PermissionService`, `DomainProvisioningService`, `ApplicationProvisioningService`

## What to Implement

Build the **Process Engine** described in the architecture doc. Here's the exact order:

---

### Step 1: Domain Models (MongoDB Documents)

Create the following document classes under the package `com.adaptive.process.model`:

#### 1.1 `ProcessDefinition.java`
- MongoDB document stored in `"process_definitions"` collection
- Fields: `id` (ObjectId), `domainId`, `appId`, `name`, `slug`, `description`, `version` (int, default 1), `status` (enum: DRAFT, PUBLISHED, ARCHIVED), `linkedModelIds` (List<ObjectId>), `nodes` (List<ProcessNode>), `edges` (List<ProcessEdge>), `settings` (ProcessSettings embedded), `createdBy`, `createdAt`, `updatedAt`
- Add compound unique index on `{domainId, appId, slug, version}`

#### 1.2 `ProcessNode.java` (embedded, not a separate collection)
- Fields: `id` (String, e.g. "node_1"), `type` (NodeType enum), `name`, `positionX`, `positionY`, `config` (Map<String, Object> — flexible per node type), `permissions` (NodePermissions embedded with `allowedRoles` List<String> and `allowedUserIds` List<ObjectId>)

#### 1.3 `NodeType.java` (enum)
- Values: `START`, `FORM_PAGE`, `DATA_VIEW`, `DATA_ACTION`, `CONDITION`, `APPROVAL`, `NOTIFICATION`, `END`

#### 1.4 `ProcessEdge.java` (embedded)
- Fields: `id` (String), `fromNodeId`, `toNodeId`, `label`, `conditionRef`

#### 1.5 `ProcessSettings.java` (embedded)
- Fields: `allowSaveDraft` (boolean), `requireAuth` (boolean)

#### 1.6 `ProcessInstance.java`
- MongoDB document stored in `"process_instances"` collection
- Fields: `id` (ObjectId), `processDefinitionId`, `processVersion`, `domainId`, `appId`, `status` (enum: ACTIVE, COMPLETED, CANCELLED, PAUSED), `currentNodeId`, `previousNodeId`, `data` (Map<String, Object>), `createdRecordIds` (List<CreatedRecord>), `assignedTo` (Assignment embedded), `history` (List<HistoryEntry>), `startedBy`, `startedAt`, `completedAt`, `draftData` (Map<String, Object>)
- Index on `{domainId, appId, status}`
- Index on `{processDefinitionId}`
- Index on `{assignedTo.userId, status}` for "my tasks" queries

#### 1.7 Embedded classes for ProcessInstance:
- `CreatedRecord`: `modelId`, `recordId`, `createdAt`
- `Assignment`: `userId`, `role`, `assignedAt`
- `HistoryEntry`: `nodeId`, `action` (String), `performedBy`, `performedAt`, `data` (Map<String, Object>), `comment`

---

### Step 2: Repositories

Create under `com.adaptive.process.repository`:

#### 2.1 `ProcessDefinitionRepository.java`
```java
public interface ProcessDefinitionRepository extends MongoRepository<ProcessDefinition, ObjectId> {
    Optional<ProcessDefinition> findByDomainIdAndAppIdAndSlugAndStatus(ObjectId domainId, ObjectId appId, String slug, ProcessStatus status);
    Optional<ProcessDefinition> findByDomainIdAndAppIdAndSlugAndVersion(ObjectId domainId, ObjectId appId, String slug, int version);
    List<ProcessDefinition> findByDomainIdAndAppId(ObjectId domainId, ObjectId appId);
    Optional<ProcessDefinition> findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(ObjectId domainId, ObjectId appId, String slug);
    boolean existsByDomainIdAndAppIdAndSlug(ObjectId domainId, ObjectId appId, String slug);
}
```

#### 2.2 `ProcessInstanceRepository.java`
```java
public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, ObjectId> {
    List<ProcessInstance> findByDomainIdAndAppId(ObjectId domainId, ObjectId appId);
    List<ProcessInstance> findByDomainIdAndAppIdAndStatus(ObjectId domainId, ObjectId appId, InstanceStatus status);
    List<ProcessInstance> findByAssignedToUserIdAndStatus(ObjectId userId, InstanceStatus status);
    long countByProcessDefinitionIdAndStatus(ObjectId definitionId, InstanceStatus status);
}
```

---

### Step 3: DTOs

Create under `com.adaptive.process.dto`:

- `CreateProcessRequest`: name, description, nodes (list), edges (list), settings, linkedModelIds
- `UpdateProcessRequest`: same as create but all optional
- `ProcessDefinitionResponse`: full definition with computed fields (nodeCount, edgeCount, isValid)
- `SubmitNodeRequest`: nodeId, formData (Map<String, Object>), action (String, for approval nodes), comment (optional)
- `ProcessInstanceResponse`: instance with currentNode config resolved (so frontend knows what to render)
- `NodeViewResponse`: current node's type, config, pre-filled data, available actions

---

### Step 4: Validation Service

Create `ProcessValidationService.java` under `com.adaptive.process.service`:

This service validates a ProcessDefinition before it can be published. It must check:

1. **Exactly one START node** exists
2. **At least one END node** exists
3. **START has exactly 1 outgoing edge, 0 incoming edges**
4. **END nodes have 0 outgoing edges, 1+ incoming edges**
5. **All nodes referenced in edges actually exist** in the nodes list
6. **All edges reference valid fromNodeId and toNodeId**
7. **Every non-START node is reachable from START** (BFS/DFS traversal)
8. **Every non-END node has a path to at least one END** (reverse traversal)
9. **CONDITION nodes**: every rule's `targetEdgeId` maps to an actual outgoing edge, and a `defaultEdgeId` exists
10. **FORM_PAGE nodes**: every element has a valid `binding.modelId` that exists in `linkedModelIds`
11. **DATA_ACTION nodes**: `modelId` exists in `linkedModelIds`, all `fieldMappings` reference valid sources
12. **No duplicate node IDs, no duplicate edge IDs**
13. **APPROVAL nodes have 2+ outgoing edges** (at least approve + reject paths)

Return a `ValidationResult` with `isValid` (boolean) and `errors` (List<String>).

---

### Step 5: Process Engine Service

Create `ProcessEngineService.java` — this is the core runtime engine:

#### 5.1 `startProcess(ObjectId definitionId, ObjectId userId)`
1. Load the PUBLISHED ProcessDefinition (throw if not found or not published)
2. Create new ProcessInstance with status=ACTIVE
3. Find the START node
4. Call `advanceToNext(instance, startNode.id)` to auto-advance past START
5. Save and return the instance with the first interactive node's view

#### 5.2 `submitNode(ObjectId instanceId, String nodeId, Map<String, Object> formData, String action, String comment, ObjectId userId)`
1. Load instance (throw if not ACTIVE)
2. Load definition by `processDefinitionId` + `processVersion`
3. Verify `instance.currentNodeId == nodeId` (throw if mismatch — means frontend is out of sync)
4. Verify user has permission for this node (check node.permissions against user's roles)
5. Switch on node type:
   - **FORM_PAGE**: validate `formData` against node's element validation rules. Merge into `instance.data` with key format `"nodeId.elementId"` → value
   - **APPROVAL**: record the action (approve/reject). Store in history with comment
   - **DATA_VIEW**: no data to collect, just record that user viewed it
6. Append HistoryEntry
7. Call `advanceToNext(instance, nodeId)` to move forward
8. Save and return updated instance

#### 5.3 `advanceToNext(ProcessInstance instance, String fromNodeId)` [PRIVATE]
1. Find outgoing edges from `fromNodeId`
2. If only one edge → follow it
3. If multiple edges → need context:
   - If `fromNode` is CONDITION → evaluate rules against `instance.data`, pick matching edge (or default)
   - If `fromNode` is APPROVAL → pick edge based on the action taken (match `edge.conditionRef` to action id)
4. Get the target node from the chosen edge
5. Switch on target node type:
   - **CONDITION**: evaluate and recurse `advanceToNext(instance, conditionNodeId)`
   - **DATA_ACTION**: execute the CRUD operation (see 5.4), record result, recurse
   - **NOTIFICATION**: fire notification async, recurse
   - **FORM_PAGE / APPROVAL / DATA_VIEW**: STOP — set `instance.currentNodeId = targetNode.id`, set `previousNodeId`
   - **END**: set `instance.status = COMPLETED`, set `completedAt`

#### 5.4 `executeDataAction(ProcessInstance instance, ProcessNode dataActionNode)` [PRIVATE]
1. Read config: operation (CREATE/UPDATE/DELETE), modelId, fieldMappings
2. For each fieldMapping, resolve the value:
   - `FORM_FIELD` source → look up `instance.data["sourceNodeId.elementId"]`
   - `STATIC` source → use the literal value
   - `CONTEXT` source → look up from instance context (e.g. "currentUserId", "currentDate")
3. Build the record document
4. Execute against the DomainModel's collection:
   - CREATE → insert document, store recordId in `instance.createdRecordIds`
   - UPDATE → find by recordId from context, update fields
   - DELETE → find by recordId, delete
5. Use the existing DomainModel service/repository to interact with data

#### 5.5 `evaluateCondition(ProcessNode conditionNode, Map<String, Object> instanceData)` [PRIVATE]
1. Read rules from config (ordered list)
2. For each rule, evaluate ALL conditions (AND logic):
   - Get field value from `instanceData`
   - Apply operator (EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, CONTAINS, IS_EMPTY, IS_NOT_EMPTY)
   - Handle type coercion (String to Number for comparisons)
3. First rule where all conditions pass → return its `targetEdgeId`
4. No match → return `defaultEdgeId`

#### 5.6 `getNodeView(ObjectId instanceId, ObjectId userId)`
1. Load instance + definition
2. Get current node
3. Build response based on node type:
   - FORM_PAGE → return element definitions, any pre-filled values from `instance.data` or `draftData`
   - DATA_VIEW → query the DomainModel collection with the node's filters, return paginated records
   - APPROVAL → return approval form config + summary of the business data being approved
   - END → return completion message

#### 5.7 `saveDraft(ObjectId instanceId, String nodeId, Map<String, Object> partialData, ObjectId userId)`
1. Verify instance is ACTIVE and currentNodeId matches
2. Merge partialData into `instance.draftData`
3. Save without advancing

---

### Step 6: Process Definition Service

Create `ProcessDefinitionService.java` for CRUD on definitions:

- `createProcess(...)`: Create DRAFT definition, auto-generate slug from name, validate basic structure
- `updateProcess(...)`: Only allowed if status=DRAFT. Replace nodes/edges/settings
- `publishProcess(...)`: Run full validation via ProcessValidationService. If valid, set status=PUBLISHED. If a previous version was PUBLISHED, archive it first. Increment version number
- `archiveProcess(...)`: Set status=ARCHIVED. Check no ACTIVE instances exist first (or warn)
- `getProcess(...)`: Return definition by slug (latest version or specific version)
- `listProcesses(...)`: Return all definitions for a domain+app, with pagination
- `deleteProcess(...)`: Only DRAFT definitions can be deleted. Hard delete.

---

### Step 7: REST Controllers

Create `ProcessDefinitionController.java` and `ProcessInstanceController.java` under `com.adaptive.process.controller`:

#### Definition endpoints (builder):
```
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes          → createProcess
GET    /adaptive/domains/{slug}/apps/{appSlug}/processes          → listProcesses
GET    /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}  → getProcess
PUT    /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}  → updateProcess
DELETE /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}  → deleteProcess
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}/publish  → publishProcess
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}/archive  → archiveProcess
```

#### Instance endpoints (runtime):
```
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}/start     → startProcess
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances                         → listInstances
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}            → getInstance
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/current-node → getNodeView
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/submit     → submitNode
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/save-draft → saveDraft
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/cancel     → cancelInstance
```

**Security annotations**: Use `@PreAuthorize` with the existing `PermissionService`:
- Definition CRUD → `DOMAIN_MANAGE_APPS` or new `APP_MANAGE_PROCESSES`
- Start process → `APP_EXECUTE` or new `APP_START_PROCESS`
- Submit/view instances → `APP_READ` minimum
- The engine should also check node-level permissions inside `submitNode()`

**Path variable resolution**: Each controller method should:
1. Resolve domain by `{slug}` using existing DomainRepository
2. Resolve app by `{appSlug}` within that domain using ApplicationRepository
3. Use domainId + appId for all subsequent queries

---

### Step 8: New Permissions

Add these to the existing permission enums:

**Domain level:**
- `DOMAIN_MANAGE_PROCESSES` — create/edit/delete/publish process definitions

**App level:**
- `APP_MANAGE_PROCESSES` — manage processes within a specific app
- `APP_START_PROCESS` — start new process instances
- `APP_VIEW_PROCESSES` — view running and completed instances

Update `DomainProvisioningService` to include `DOMAIN_MANAGE_PROCESSES` in the "Domain Admin" group's default permissions.

Update `ApplicationProvisioningService` to include the new APP permissions in the default app groups:
- App Admin → all 3 new permissions
- App Editor → `APP_MANAGE_PROCESSES` + `APP_START_PROCESS` + `APP_VIEW_PROCESSES`
- App Viewer → `APP_VIEW_PROCESSES` only

---

### Step 9: Form Validation Logic

Create `FormValidationService.java` to validate form submissions against FORM_PAGE element configs:

For each element in the node's config:
1. Check `required` — if true, value must be present and non-empty
2. Check `minLength` / `maxLength` for STRING types
3. Check `min` / `max` for NUMBER types
4. Check `pattern` (regex) for STRING types
5. Check `visibilityRule` — if the element is hidden (based on another field's value), skip its validation
6. Check that SELECT/RADIO/CHECKBOX values match the defined `options`
7. Return list of validation errors with field-level detail: `{elementId, message}`

---

### Step 10: Exception Handling

Create these custom exceptions under `com.adaptive.process.exception`:

- `ProcessNotFoundException` — definition or instance not found
- `ProcessValidationException` — definition failed validation (carries error list)
- `InvalidNodeSubmissionException` — submitted to wrong node, or invalid form data
- `ProcessAlreadyCompletedException` — tried to submit to a completed/cancelled instance
- `InsufficientProcessPermissionException` — user lacks node-level permission

Create a `@RestControllerAdvice` for process-related exceptions that returns proper error responses.

---

## Important Implementation Notes

1. **The `config` field on ProcessNode is `Map<String, Object>`** — this keeps it flexible per node type. The engine casts/parses it based on `NodeType`. You can optionally create typed config classes (FormPageConfig, ConditionConfig, etc.) and use a custom deserializer, but the Map approach is simpler for Phase 1.

2. **Data keys in ProcessInstance.data** use the format `"nodeId.elementId"` → value. This prevents collisions when two different form pages have fields with the same element ID.

3. **advanceToNext() is recursive** — it chains through silent nodes (CONDITION → DATA_ACTION → NOTIFICATION) until it hits one that needs human interaction. Add a safety counter (max 50 hops) to prevent infinite loops from circular edges.

4. **Version immutability** — once a ProcessDefinition is PUBLISHED, it must never be mutated. Publishing creates a snapshot. Editing after publish creates a new DRAFT with version+1. Running instances always reference the version they started with.

5. **DomainModel interaction** — DATA_ACTION nodes interact with existing DomainModel data. Use your existing DomainModel repository/service. The process engine creates/reads documents in the model's collection using the field mappings defined in the node config.

6. **Don't implement NOTIFICATION node internals in Phase 1** — just create the node type and have the engine log "notification would be sent" and skip past it. Wire up actual email/in-app notifications later.

7. **Error handling in advanceToNext()** — if a DATA_ACTION fails (e.g. validation error on the model), the instance should NOT advance. Roll back to the previous node, set instance status to PAUSED with an error message, and return the error to the user.

8. **All history entries are append-only** — never mutate or delete entries from `instance.history`. This is the audit trail.

---

## File Structure

```
com.adaptive.process/
├── model/
│   ├── ProcessDefinition.java
│   ├── ProcessNode.java
│   ├── ProcessEdge.java
│   ├── ProcessSettings.java
│   ├── ProcessInstance.java
│   ├── enums/
│   │   ├── NodeType.java
│   │   ├── ProcessStatus.java      (DRAFT, PUBLISHED, ARCHIVED)
│   │   └── InstanceStatus.java     (ACTIVE, COMPLETED, CANCELLED, PAUSED)
│   └── embedded/
│       ├── CreatedRecord.java
│       ├── Assignment.java
│       ├── HistoryEntry.java
│       └── NodePermissions.java
├── repository/
│   ├── ProcessDefinitionRepository.java
│   └── ProcessInstanceRepository.java
├── service/
│   ├── ProcessDefinitionService.java
│   ├── ProcessEngineService.java
│   ├── ProcessValidationService.java
│   └── FormValidationService.java
├── controller/
│   ├── ProcessDefinitionController.java
│   └── ProcessInstanceController.java
├── dto/
│   ├── CreateProcessRequest.java
│   ├── UpdateProcessRequest.java
│   ├── ProcessDefinitionResponse.java
│   ├── SubmitNodeRequest.java
│   ├── ProcessInstanceResponse.java
│   ├── NodeViewResponse.java
│   └── ValidationResult.java
└── exception/
    ├── ProcessNotFoundException.java
    ├── ProcessValidationException.java
    ├── InvalidNodeSubmissionException.java
    ├── ProcessAlreadyCompletedException.java
    ├── InsufficientProcessPermissionException.java
    └── ProcessExceptionHandler.java
```

---

## What NOT to Build Yet (Phase 2)

- Calculated fields or expression evaluation
- Sub-processes (node that launches another process)
- Scheduled/timer-based triggers
- Parallel branches (AND split / AND join)
- Custom validation functions beyond primitive checks
- File upload handling in form elements
- Process templates or marketplace
- WebSocket-based real-time updates

Focus on getting the core loop working: **define → publish → start → fill form → route by condition → execute data action → complete**. That covers 80% of real use cases.
