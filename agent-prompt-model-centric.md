# Agent Prompt: Implement Model-Centric Workflow Engine

## Context

I'm building a multi-tenant SaaS platform using Spring Boot + MongoDB for small/medium businesses. The platform lets business owners create domains, register users, manage applications, and automate their business processes via workflows.

The attached `model-centric-workflow-architecture.md` contains the full architecture proposal. **Read it completely before writing any code.**

**CRITICAL DESIGN PRINCIPLE:** In this system, a workflow step IS a DomainModel. There are no abstract "nodes" — every step in a workflow corresponds to a real data model, and executing a step creates a real record in that model's collection. Edges (actions like submit, approve, reject) are properties of the step, NOT separate entities. Permissions are assigned to edges, NOT to steps.

## Existing Tech Stack

- **Java 17+** with Spring Boot 3.x
- **MongoDB** as the primary database (Spring Data MongoDB)
- **JWT-based auth** with two principal types: `OWNER` and `DOMAIN_USER`
- **RBAC** with domain-level and app-level permissions
- **Existing entities**: `Domain`, `Owner`, `DomainUser`, `Application`, `DomainModel`, `DomainGroup`, `DomainGroupMember`
- **Existing services**: `PermissionService`, `DomainProvisioningService`, `ApplicationProvisioningService`
- **DomainModel** already has: `id`, `domainId`, `slug`, `name`, `description`, `version`, `sharedWithAllApps`, `allowedAppIds`, `fields[]` (each field has `key`, `type`, `required`, `unique`, `config`)

## What to Implement

Build the **Model-Centric Workflow Engine**. Follow these steps in exact order.

---

### Step 1: Domain Models (MongoDB Documents)

Create the following under `com.adaptive.workflow.model`:

#### 1.1 `WorkflowDefinition.java`
MongoDB document in `"workflow_definitions"` collection.

Fields:
- `id` (ObjectId)
- `domainId` (ObjectId)
- `appId` (ObjectId)
- `name` (String) — "Leave Request Approval"
- `slug` (String) — auto-generated from name
- `description` (String)
- `version` (int, default 1)
- `status` (WorkflowStatus enum: DRAFT, PUBLISHED, ARCHIVED)
- `steps` (List<WorkflowStep>) — the ordered model-steps
- `globalEdges` (List<WorkflowEdge>) — edges executable from any step (like "Cancel")
- `createdBy` (ObjectId)
- `createdAt` (DateTime)
- `updatedAt` (DateTime)

Indexes:
- Compound unique: `{domainId, appId, slug, version}`
- `{domainId, appId, status}`

#### 1.2 `WorkflowStep.java` (embedded, NOT a separate collection)

Fields:
- `id` (String) — unique within workflow, e.g. "step_1"
- `modelId` (ObjectId) — **reference to an existing DomainModel**
- `name` (String) — display name for this step
- `order` (int) — position in workflow (0, 1, 2...)
- `isStart` (boolean) — exactly one step must be true
- `isEnd` (boolean) — can be multiple
- `edges` (List<WorkflowEdge>) — actions available at this step
- `dataConfig` (StepDataConfig) — how to get data from previous steps
- `positionX` (Double) — for visual designer
- `positionY` (Double) — for visual designer

#### 1.3 `WorkflowEdge.java` (embedded in WorkflowStep.edges[])

This is the KEY entity — it represents an action a user can take. Permissions live HERE.

Fields:
- `id` (String) — unique within the step, e.g. "edge_submit"
- `name` (String) — what the user sees: "Submit", "Approve", "Reject", "Cancel"
- `targetStepId` (String) — which step to advance to (null if terminal)
- `isTerminal` (boolean) — true = executing this edge ends the workflow
- `allowedRoles` (List<String>) — **role names** that can execute this edge. These match DomainGroup names. Example: ["manager", "hr"]
- `allowedUserIds` (List<ObjectId>) — specific users who can execute (optional, for overrides)
- `onlySubmitter` (boolean) — if true, only the user who started the workflow instance can execute this edge. Used for "Cancel" actions where only the original submitter should be able to cancel.
- `requiredFields` (List<String>) — field keys from the step's DomainModel that must be filled before this edge can fire
- `conditions` (List<EdgeCondition>) — optional conditions that must be true for this edge to be available
- `autoActions` (List<AutoAction>) — things that happen automatically when edge executes

#### 1.4 `EdgeCondition.java` (embedded in WorkflowEdge.conditions[])

Fields:
- `field` (String) — field key, can reference current step or previous step data using "stepId.fieldKey" format
- `operator` (ConditionOperator enum: EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, IS_EMPTY, IS_NOT_EMPTY, CONTAINS)
- `value` (Object) — the value to compare against

#### 1.5 `AutoAction.java` (embedded in WorkflowEdge.autoActions[])

Fields:
- `type` (AutoActionType enum: SEND_NOTIFICATION, UPDATE_FIELD, CREATE_RECORD)
- `config` (Map<String, Object>) — action-specific settings

For Phase 1, just define the structure. Don't implement the auto-action execution — log and skip.

#### 1.6 `StepDataConfig.java` (embedded in WorkflowStep)

Fields:
- `referencePreviousStep` (boolean) — can this step read the previous step's record?
- `previousStepFields` (List<String>) — which fields to pull (empty = all)
- `autoFetchRules` (List<AutoFetchRule>) — queries to run based on previous step data
- `readOnlyFields` (List<String>) — fields displayed but not editable at this step

#### 1.7 `AutoFetchRule.java` (embedded in StepDataConfig)

Fields:
- `sourceStepId` (String) — which step's data to use as the lookup key
- `sourceField` (String) — field from that step (e.g., "employeeId")
- `targetModelId` (ObjectId) — which DomainModel to query
- `targetLookupField` (String) — field to match against (e.g., "_id")
- `fieldsToFetch` (List<String>) — which fields to return

#### 1.8 `WorkflowInstance.java`
MongoDB document in `"workflow_instances"` collection.

Fields:
- `id` (ObjectId)
- `workflowDefinitionId` (ObjectId)
- `workflowVersion` (int) — snapshot of version used
- `domainId` (ObjectId)
- `appId` (ObjectId)
- `status` (InstanceStatus enum: ACTIVE, COMPLETED, CANCELLED)
- `currentStepId` (String) — which step the instance is at
- `stepRecords` (Map<String, ObjectId>) — stepId → recordId mapping. Each step creates a real record in its DomainModel collection.
- `history` (List<InstanceHistory>) — append-only audit trail
- `startedBy` (ObjectId) — the user who created this instance
- `startedAt` (DateTime)
- `completedAt` (DateTime)

Indexes:
- `{domainId, appId, status}`
- `{workflowDefinitionId}`
- `{startedBy, status}`
- `{currentStepId, status}` — for finding instances at a particular step

#### 1.9 `InstanceHistory.java` (embedded in WorkflowInstance.history[])

Fields:
- `stepId` (String)
- `edgeId` (String) — which edge was executed
- `edgeName` (String) — snapshot: "Submit", "Approve", etc.
- `performedBy` (ObjectId)
- `performedByName` (String) — snapshot of username for easy display
- `performedAt` (DateTime)
- `comment` (String) — optional comment
- `recordId` (ObjectId) — the record created/updated at this step
- `formData` (Map<String, Object>) — snapshot of data submitted (for audit)

#### 1.10 Enums

Create under `com.adaptive.workflow.model.enums`:
- `WorkflowStatus`: DRAFT, PUBLISHED, ARCHIVED
- `InstanceStatus`: ACTIVE, COMPLETED, CANCELLED
- `ConditionOperator`: EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_EQUAL, LESS_EQUAL, IS_EMPTY, IS_NOT_EMPTY, CONTAINS
- `AutoActionType`: SEND_NOTIFICATION, UPDATE_FIELD, CREATE_RECORD

---

### Step 2: Repositories

Create under `com.adaptive.workflow.repository`:

#### 2.1 `WorkflowDefinitionRepository.java`
```java
public interface WorkflowDefinitionRepository extends MongoRepository<WorkflowDefinition, ObjectId> {
    Optional<WorkflowDefinition> findByDomainIdAndAppIdAndSlugAndStatus(
        ObjectId domainId, ObjectId appId, String slug, WorkflowStatus status);
    List<WorkflowDefinition> findByDomainIdAndAppId(ObjectId domainId, ObjectId appId);
    Optional<WorkflowDefinition> findTopByDomainIdAndAppIdAndSlugOrderByVersionDesc(
        ObjectId domainId, ObjectId appId, String slug);
    boolean existsByDomainIdAndAppIdAndSlug(ObjectId domainId, ObjectId appId, String slug);
}
```

#### 2.2 `WorkflowInstanceRepository.java`
```java
public interface WorkflowInstanceRepository extends MongoRepository<WorkflowInstance, ObjectId> {
    List<WorkflowInstance> findByDomainIdAndAppIdAndStatus(
        ObjectId domainId, ObjectId appId, InstanceStatus status);
    List<WorkflowInstance> findByDomainIdAndAppId(ObjectId domainId, ObjectId appId);
    List<WorkflowInstance> findByStartedByAndStatus(ObjectId userId, InstanceStatus status);
    long countByWorkflowDefinitionIdAndStatus(ObjectId definitionId, InstanceStatus status);
}
```

---

### Step 3: DTOs

Create under `com.adaptive.workflow.dto`:

#### 3.1 Request DTOs
- `CreateWorkflowRequest`: name, description, steps (list of step DTOs), globalEdges (list)
- `UpdateWorkflowRequest`: same fields, all optional
- `ExecuteEdgeRequest`: `edgeId` (String), `formData` (Map<String, Object>), `comment` (String optional)
- `StartWorkflowRequest`: `formData` (Map<String, Object>) — initial data for the start step

#### 3.2 Response DTOs
- `WorkflowDefinitionResponse`: full definition with metadata (stepCount, isValid, etc.)
- `WorkflowInstanceResponse`: instance with current step info, available edges for requesting user
- `StepViewResponse`: current step's model fields, pre-filled/referenced data, read-only fields, available edges (filtered by user's roles)
- `TaskResponse`: for "my tasks" — instance summary + available edges + key data fields
- `TaskListResponse`: list of TaskResponse with count
- `ExecuteEdgeResponse`: updated instance status, new current step, what happened
- `HistoryResponse`: list of history entries for an instance

---

### Step 4: Workflow Validation Service

Create `WorkflowValidationService.java` under `com.adaptive.workflow.service`.

Validates a WorkflowDefinition before it can be published:

1. **Exactly one step with isStart=true**
2. **At least one step with isEnd=true OR at least one edge with isTerminal=true**
3. **All step IDs are unique** within the workflow
4. **All edge IDs are unique** within their parent step
5. **Every edge's targetStepId** (when not null) references an existing step ID
6. **Every step's modelId** references a valid DomainModel that exists and is accessible to this app
7. **Every edge has at least one permission**: allowedRoles is not empty, OR allowedUserIds is not empty, OR onlySubmitter is true
8. **No orphan steps**: every non-start step must be reachable (some edge in some step points to it)
9. **Edge requiredFields** must reference valid field keys in the step's DomainModel
10. **globalEdges** must all have isTerminal=true or a valid targetStepId

Return `ValidationResult` with `isValid` (boolean) and `errors` (List<String> with specific messages).

---

### Step 5: Workflow Engine Service (CORE)

Create `WorkflowEngineService.java` — this is the heart of the system.

#### 5.1 `startWorkflow(ObjectId definitionId, Map<String, Object> formData, ObjectId userId)`

```
1. Load PUBLISHED WorkflowDefinition (throw if not found/not published)
2. Find the start step (isStart=true)
3. Validate formData against start step's DomainModel field rules
4. Create a real record in the start step's DomainModel collection
   → Use MongoTemplate to insert into the model's collection
   → The collection name comes from the DomainModel's slug or a configured collection name
5. Create WorkflowInstance:
   - status = ACTIVE
   - currentStepId = start step's id
   - stepRecords = { startStepId: newRecordId }
   - history = [{ stepId, edgeId: null, edgeName: "Started", performedBy, ... }]
   - startedBy = userId
6. Save and return instance
```

**IMPORTANT:** The record created in step 4 is a REAL document in the DomainModel's data collection. This is not workflow-specific storage — it's the actual business data. If the DomainModel is "leave_requests" with fields [employeeName, startDate, endDate, reason], then a real leave request document gets created.

Use `MongoTemplate` to dynamically insert into the correct collection based on the DomainModel. You'll need a helper that resolves which MongoDB collection a DomainModel stores its records in.

#### 5.2 `executeEdge(ObjectId instanceId, String edgeId, Map<String, Object> formData, String comment, ObjectId userId)`

This is the most critical method. Follow this exact sequence:

```
1. Load WorkflowInstance (throw if not ACTIVE)
2. Load WorkflowDefinition by (workflowDefinitionId + workflowVersion)
3. Find currentStep from the definition's steps
4. Find the edge:
   a. First check currentStep.edges for matching edgeId
   b. If not found, check definition.globalEdges for matching edgeId
   c. If not found anywhere → throw EdgeNotFoundException

5. PERMISSION CHECK (this is critical — permissions are on EDGES):
   a. Get user's groups/roles from DomainGroupMember
   b. Get user's role names (group names they belong to)
   c. Check: does ANY of user's role names appear in edge.allowedRoles?
   d. Check: is userId in edge.allowedUserIds?
   e. If edge.onlySubmitter == true → check userId == instance.startedBy
   f. If NONE of the above match → throw InsufficientPermissionException
      Message: "You do not have permission to execute '{edgeName}'"

6. CONDITION CHECK (if edge has conditions):
   a. For each condition in edge.conditions:
      - Resolve the field value from instance data
        (if field contains "." like "step_leave.employeeId", look up stepRecords 
         for that step, then query the record for that field)
      - Apply the operator
      - If any condition fails → throw ConditionNotMetException
   b. All conditions must pass (AND logic)

7. VALIDATE FORM DATA:
   a. Load the DomainModel for the current step (by step.modelId)
   b. For each field in the model that appears in formData:
      - Validate type (String, Number, Boolean, Date, etc.)
      - Check required fields from edge.requiredFields
   c. If validation fails → return field-level errors

8. SAVE RECORD in the step's DomainModel collection:
   a. If this step already has a record (instance.stepRecords[currentStepId] exists):
      → UPDATE the existing record with formData
   b. If no record yet:
      → CREATE a new record in the DomainModel's collection
      → Store: instance.stepRecords[currentStepId] = newRecordId

9. ADVANCE THE INSTANCE:
   a. If edge.isTerminal == true:
      → instance.status = COMPLETED (or CANCELLED if edge name is "Cancel")
      → instance.completedAt = now()
   b. Else:
      → instance.currentStepId = edge.targetStepId

10. APPEND HISTORY (always, this is the audit trail):
    → { stepId, edgeId, edgeName: edge.name, performedBy: userId,
        performedByName: user's display name, performedAt: now(),
        comment, recordId, formData: snapshot of submitted data }

11. AUTO-ACTIONS (Phase 1: just log them, don't execute):
    → For each edge.autoActions: log("Auto-action: " + type + " - skipped in Phase 1")

12. SAVE instance and return ExecuteEdgeResponse
```

#### 5.3 `getStepView(ObjectId instanceId, ObjectId userId)`

Returns what the user should see at the current step — the form fields, any referenced/fetched data, and the edges they can execute.

```
1. Load instance + definition
2. Find currentStep
3. Load the step's DomainModel → get field definitions

4. BUILD DATA VIEW:
   a. If step has a record (stepRecords[currentStepId]):
      → Load it and include as "currentData"
   b. If step.dataConfig.referencePreviousStep == true:
      → Find the previous step (from history, the last step visited)
      → Load its record from stepRecords[previousStepId]
      → Filter by dataConfig.previousStepFields (or include all)
      → Include as "referencedData"
   c. If step.dataConfig.autoFetchRules is not empty:
      → For each rule:
        - Get the source value from the source step's record
        - Query the target model's collection where targetLookupField = sourceValue
        - Pull only fieldsToFetch
      → Include as "fetchedData"
   d. Mark dataConfig.readOnlyFields as read-only in the response

5. FILTER EDGES BY USER PERMISSION:
   a. Get user's roles
   b. From currentStep.edges, include only edges where:
      - User's roles intersect with edge.allowedRoles, OR
      - userId is in edge.allowedUserIds, OR
      - (edge.onlySubmitter && userId == instance.startedBy)
   c. Also check definition.globalEdges the same way
   d. For each included edge, evaluate conditions — if conditions fail, 
      still include the edge but mark it as "disabled" with a reason

6. Return StepViewResponse:
   {
     stepName, modelFields, currentData, referencedData, fetchedData,
     readOnlyFields, availableEdges (filtered), history
   }
```

#### 5.4 `getMyTasks(ObjectId userId, ObjectId domainId, ObjectId appId)`

This powers the dashboard — "what instances are waiting for me?"

```
1. Get user's roles (all DomainGroup names the user belongs to)
2. Query all ACTIVE instances for this domain+app
3. For each instance:
   a. Load its workflow definition (can cache — definitions don't change once published)
   b. Find the currentStep
   c. Check currentStep.edges + definition.globalEdges:
      - Does ANY edge match the user's roles/userId/submitter-check?
   d. If YES → include this instance in results
      Include: instanceId, workflowName, currentStepName, startedBy info,
               startedAt, the matching available edges, and a summary of 
               key data fields from the current step's record

4. Sort by startedAt (oldest first — FIFO)
5. Return TaskListResponse
```

**Performance note:** This method could be slow if there are many active instances. For Phase 1, this is acceptable. For Phase 2, consider:
- Adding a `pendingForRoles` field on WorkflowInstance that gets updated on each edge execution
- Creating a separate `workflow_tasks` collection that's denormalized for fast queries
- Indexing on `currentStepId + status` and joining with edge permissions

---

### Step 6: Workflow Definition Service

Create `WorkflowDefinitionService.java` for CRUD on definitions:

- `createWorkflow(...)`: Create DRAFT definition, auto-generate slug from name. Validate that all referenced modelIds exist and are accessible to the app.
- `updateWorkflow(...)`: Only allowed if status=DRAFT. Replace steps/edges.
- `publishWorkflow(...)`: Run WorkflowValidationService. If valid, set status=PUBLISHED. If a previous PUBLISHED version exists, archive it. Increment version.
- `archiveWorkflow(...)`: Set status=ARCHIVED. Warn if active instances exist.
- `getWorkflow(...)`: Return by slug (latest version or specific version).
- `listWorkflows(...)`: All definitions for domain+app, with pagination.
- `deleteWorkflow(...)`: Only DRAFT can be deleted. Hard delete.

---

### Step 7: REST Controllers

Create under `com.adaptive.workflow.controller`:

#### 7.1 `WorkflowDefinitionController.java`

```
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows              → createWorkflow
GET    /adaptive/domains/{slug}/apps/{appSlug}/workflows              → listWorkflows
GET    /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}     → getWorkflow
PUT    /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}     → updateWorkflow
DELETE /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}     → deleteWorkflow
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}/publish  → publishWorkflow
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}/archive  → archiveWorkflow
```

#### 7.2 `WorkflowInstanceController.java`

```
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}/start         → startWorkflow
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances                         → listInstances
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/my-tasks                → getMyTasks ★
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}            → getInstance
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/view       → getStepView ★
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/execute    → executeEdge ★
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/history    → getHistory
```

**Security:**
- Definition CRUD → `@PreAuthorize` with `DOMAIN_MANAGE_WORKFLOWS` or `APP_MANAGE_WORKFLOWS`
- Start workflow → `APP_START_WORKFLOW`
- Execute edge → **checked inside the engine** (edge-level permissions, not annotation-level)
- View/list → `APP_VIEW_WORKFLOWS`
- my-tasks → any authenticated domain user (the edge permissions filter what they see)

**Path resolution:** Each method must:
1. Resolve domain by `{slug}` → get domainId
2. Resolve app by `{appSlug}` within domain → get appId
3. Use domainId + appId for all queries

---

### Step 8: DomainModel Record Helper

Create `ModelRecordService.java` — handles dynamic CRUD against DomainModel collections.

```java
@Service
public class ModelRecordService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private DomainModelRepository domainModelRepository;
    
    /**
     * Get the MongoDB collection name for a DomainModel.
     * Convention: "domain_{domainId}_model_{modelSlug}_records"
     * This keeps each model's data isolated.
     */
    public String getCollectionName(DomainModel model) {
        return "domain_" + model.getDomainId().toHexString() 
             + "_model_" + model.getSlug() + "_records";
    }
    
    /**
     * Create a record in the model's collection.
     * Validates field types against the model's field definitions.
     */
    public ObjectId createRecord(ObjectId modelId, Map<String, Object> data, ObjectId createdBy) {
        DomainModel model = domainModelRepository.findById(modelId)
            .orElseThrow(() -> new ModelNotFoundException(modelId));
        
        // Validate data against model fields
        validateDataAgainstModel(model, data);
        
        // Add metadata
        data.put("_createdBy", createdBy);
        data.put("_createdAt", Instant.now());
        data.put("_updatedAt", Instant.now());
        
        Document doc = new Document(data);
        mongoTemplate.insert(doc, getCollectionName(model));
        return doc.getObjectId("_id");
    }
    
    /**
     * Update an existing record.
     */
    public void updateRecord(ObjectId modelId, ObjectId recordId, Map<String, Object> data) {
        DomainModel model = domainModelRepository.findById(modelId)
            .orElseThrow();
        
        validateDataAgainstModel(model, data);
        data.put("_updatedAt", Instant.now());
        
        Query query = new Query(Criteria.where("_id").is(recordId));
        Update update = new Update();
        data.forEach(update::set);
        
        mongoTemplate.updateFirst(query, update, getCollectionName(model));
    }
    
    /**
     * Read a record by ID.
     */
    public Map<String, Object> getRecord(ObjectId modelId, ObjectId recordId) {
        DomainModel model = domainModelRepository.findById(modelId)
            .orElseThrow();
        
        Query query = new Query(Criteria.where("_id").is(recordId));
        Document doc = mongoTemplate.findOne(query, Document.class, getCollectionName(model));
        return doc != null ? new HashMap<>(doc) : null;
    }
    
    /**
     * Query records by a field value (for auto-fetch rules).
     */
    public List<Map<String, Object>> queryByField(
            ObjectId modelId, String fieldKey, Object value, List<String> fieldsToFetch) {
        DomainModel model = domainModelRepository.findById(modelId)
            .orElseThrow();
        
        Query query = new Query(Criteria.where(fieldKey).is(value));
        if (fieldsToFetch != null && !fieldsToFetch.isEmpty()) {
            fieldsToFetch.forEach(f -> query.fields().include(f));
        }
        
        List<Document> docs = mongoTemplate.find(query, Document.class, getCollectionName(model));
        return docs.stream().map(d -> new HashMap<String, Object>(d)).toList();
    }
    
    private void validateDataAgainstModel(DomainModel model, Map<String, Object> data) {
        // Validate each field in data against model.fields definitions
        // Check types: STRING, NUMBER, BOOLEAN, DATE, DATETIME, REFERENCE, OBJECT, ARRAY
        // Check required/unique constraints
        // Throw ValidationException with field-level errors
    }
}
```

---

### Step 9: Permissions

Add to existing permission enums:

**Domain level:**
- `DOMAIN_MANAGE_WORKFLOWS`

**App level:**
- `APP_MANAGE_WORKFLOWS`
- `APP_START_WORKFLOW`
- `APP_VIEW_WORKFLOWS`

Update provisioning services:
- **Domain Admin group** → add `DOMAIN_MANAGE_WORKFLOWS`
- **App Admin group** → add all 3 new APP permissions
- **App Editor group** → add `APP_MANAGE_WORKFLOWS` + `APP_START_WORKFLOW` + `APP_VIEW_WORKFLOWS`
- **App Viewer group** → add `APP_VIEW_WORKFLOWS` only

**Edge permissions use role NAMES (strings) that match DomainGroup names.** The engine resolves this by:
1. Getting user's DomainGroupMember entries for the domain
2. Getting the DomainGroup names from those entries
3. Checking if any group name appears in `edge.allowedRoles`

This means if a domain has groups named "manager", "hr", "employee", the workflow edges reference those exact strings.

---

### Step 10: Exception Handling

Create under `com.adaptive.workflow.exception`:

- `WorkflowNotFoundException` — definition or instance not found
- `WorkflowValidationException` — carries List<String> of errors
- `EdgeNotFoundException` — the requested edge doesn't exist on current step
- `InsufficientEdgePermissionException` — user can't execute this edge (include which edge and why)
- `ConditionNotMetException` — edge conditions not satisfied
- `WorkflowAlreadyCompletedException` — instance is COMPLETED or CANCELLED
- `InvalidFormDataException` — form data doesn't match model field requirements
- `ModelNotFoundException` — referenced DomainModel doesn't exist

Create `WorkflowExceptionHandler.java` with `@RestControllerAdvice`:
- Map each exception to appropriate HTTP status (404, 403, 400, 409)
- Return consistent error response: `{ error, message, details, timestamp }`

---

## File Structure

```
com.adaptive.workflow/
├── model/
│   ├── WorkflowDefinition.java
│   ├── WorkflowStep.java
│   ├── WorkflowEdge.java
│   ├── EdgeCondition.java
│   ├── AutoAction.java
│   ├── StepDataConfig.java
│   ├── AutoFetchRule.java
│   ├── WorkflowInstance.java
│   ├── InstanceHistory.java
│   └── enums/
│       ├── WorkflowStatus.java
│       ├── InstanceStatus.java
│       ├── ConditionOperator.java
│       └── AutoActionType.java
├── repository/
│   ├── WorkflowDefinitionRepository.java
│   └── WorkflowInstanceRepository.java
├── service/
│   ├── WorkflowDefinitionService.java
│   ├── WorkflowEngineService.java
│   ├── WorkflowValidationService.java
│   ├── WorkflowTaskService.java          (getMyTasks logic, can be in engine too)
│   └── ModelRecordService.java           (dynamic CRUD on DomainModel collections)
├── controller/
│   ├── WorkflowDefinitionController.java
│   └── WorkflowInstanceController.java
├── dto/
│   ├── request/
│   │   ├── CreateWorkflowRequest.java
│   │   ├── UpdateWorkflowRequest.java
│   │   ├── StartWorkflowRequest.java
│   │   └── ExecuteEdgeRequest.java
│   └── response/
│       ├── WorkflowDefinitionResponse.java
│       ├── WorkflowInstanceResponse.java
│       ├── StepViewResponse.java
│       ├── TaskResponse.java
│       ├── TaskListResponse.java
│       ├── ExecuteEdgeResponse.java
│       ├── HistoryResponse.java
│       └── ValidationResult.java
└── exception/
    ├── WorkflowNotFoundException.java
    ├── WorkflowValidationException.java
    ├── EdgeNotFoundException.java
    ├── InsufficientEdgePermissionException.java
    ├── ConditionNotMetException.java
    ├── WorkflowAlreadyCompletedException.java
    ├── InvalidFormDataException.java
    ├── ModelNotFoundException.java
    └── WorkflowExceptionHandler.java
```

---

## Critical Implementation Notes

1. **A step IS a DomainModel reference.** Every time a step is visited, a real record gets created in that model's MongoDB collection. The record persists even if the workflow is cancelled. This is business data, not workflow metadata.

2. **Permissions are on EDGES, not steps.** A user might be at a step where they can see the data but have NO edges available to them — that means it's not their turn to act. The `getStepView()` method should return the data (for transparency) but show zero available edges.

3. **The dropdown is the edge selector.** The frontend shows a dropdown of available edge names (filtered by the user's permissions). The user selects one and clicks "Execute". This is not a button-per-action UI — it's a single dropdown + execute button.

4. **`onlySubmitter` is special.** It doesn't check role — it checks identity. The person who STARTED the workflow instance is the only one who can execute edges marked `onlySubmitter=true`. This handles the "cancel" case: anyone can start a leave request, but only the person who started it can cancel it.

5. **`stepRecords` is the data backbone.** It maps each step to the record created in its DomainModel. When `referencePreviousStep=true`, the engine uses this map to look up the previous step's record and pull its data. This is how the manager sees the employee's leave details.

6. **History is append-only and immutable.** Never update or delete history entries. Each edge execution creates a new entry. Include `formData` snapshot in history so you can see exactly what was submitted at each step.

7. **Version immutability.** Once a WorkflowDefinition is PUBLISHED, it must never be modified. Publishing creates a snapshot. Editing creates a new DRAFT version. Running instances always use the version they started with.

8. **Global edges** are checked in addition to current step edges. They allow "Cancel from any step" functionality. Global edges should typically be terminal (isTerminal=true).

9. **Collection naming for DomainModel records:** Use a deterministic convention like `domain_{domainId}_model_{modelSlug}_records`. This keeps data isolated per domain and per model.

10. **Role name matching is case-insensitive.** When checking `edge.allowedRoles` against user's group names, use case-insensitive comparison to avoid "Manager" vs "manager" mismatches.

---

## What NOT to Build Yet (Phase 2)

- AutoAction execution (notifications, field updates) — just log and skip
- Conditional edge visibility based on data values — include the structure but skip evaluation
- Parallel approval (multiple approvers must all approve)
- SLA / timeout tracking
- Drag-and-drop visual builder frontend
- Dashboard analytics / reporting
- Bulk operations on instances
- Webhooks / external integrations
- File attachments on steps

**Phase 1 goal:** A working engine where you can define a workflow with model-steps and edges, start instances, execute edges with role-based permissions, see your pending tasks, and have a full audit trail. That covers leave requests, stock approvals, purchase orders, and most linear business processes.
