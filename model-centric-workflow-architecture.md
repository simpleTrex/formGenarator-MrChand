# Model-Centric Workflow Engine — Architecture

## Core Philosophy

**A model IS a node. Edges ARE properties of a model.**

There is no separate concept of "nodes" and "edges" as independent entities. Instead:
- A **WorkflowDefinition** is an ordered chain of **DomainModel references** (each model acts as a step/node)
- Each model-step has **edges** embedded inside it — these are the actions a user can take (submit, approve, reject, cancel)
- Each edge points to another model-step (the next step in the workflow)
- **Permissions live on edges, NOT on models** — this controls who can do what at each step
- A user only sees the edges they have permission for — if they can only "cancel", that's all they see in the dropdown

---

## Why This Design (vs Node-Edge Graph)

| Aspect | Previous design (rejected) | This design |
|---|---|---|
| What is a step? | Abstract "node" with a type | A DomainModel instance — real data |
| Where do edges live? | Separate entity connecting nodes | Inside the model-step, as a property |
| What does the user fill? | Generic form page configured per node | The DomainModel's own fields |
| Who can act? | Permissions on nodes | Permissions on edges |
| What does a user see? | All actions at a node | Only edges they're allowed to execute |
| Data flow | Manual field mapping between nodes | Auto-reference to previous model data |

---

## Data Model (MongoDB Collections)

### 1. WorkflowDefinition

The blueprint — defines which models participate and how they connect.

```
WorkflowDefinition
│
├── _id: ObjectId
├── domainId: ObjectId
├── appId: ObjectId
├── name: String                         — "Leave Request Approval"
├── slug: String                         — "leave-request-approval"  
├── description: String
├── version: Integer                     — starts at 1, increments on publish
├── status: DRAFT | PUBLISHED | ARCHIVED
│
├── steps: [WorkflowStep]               — ★ ordered list of model-steps
│
├── createdBy: ObjectId
├── createdAt: DateTime
└── updatedAt: DateTime
```

### 2. WorkflowStep (embedded in WorkflowDefinition.steps[])

Each step IS a reference to a DomainModel. The step defines what edges (actions) are available at this point.

```
WorkflowStep
│
├── id: String                           — unique within workflow ("step_1", "step_2")
├── modelId: ObjectId                    — ★ reference to a DomainModel
├── name: String                         — display name ("Leave Request", "Manager Review")
├── order: Integer                       — position in the workflow (0, 1, 2...)
├── isStart: Boolean                     — exactly one step must be true
├── isEnd: Boolean                       — can be multiple terminal steps
│
├── edges: [WorkflowEdge]               — ★ actions available at this step
│
├── dataConfig: StepDataConfig           — how this step gets data from previous steps
│
├── positionX: Double                    — for visual designer (drag-and-drop canvas)
└── positionY: Double
```

### 3. WorkflowEdge (embedded in WorkflowStep.edges[])

An edge is an ACTION the user can take at a step. It belongs to the model-step, not to the workflow as a whole.

```
WorkflowEdge
│
├── id: String                           — unique within the step ("edge_submit", "edge_approve")
├── name: String                         — display label ("Submit", "Approve", "Reject", "Cancel")
├── targetStepId: String                 — which step to go to next (null if this leads to completion)
├── isTerminal: Boolean                  — true = this edge ends the workflow (e.g., final approval)
│
├── allowedRoles: [String]               — ★ which roles can execute this edge
├── allowedUserIds: [ObjectId]           — specific users who can execute (optional)
├── onlySubmitter: Boolean               — ★ if true, only the user who created the instance can use this edge
│                                          (for "cancel" scenarios — only the person who submitted can cancel)
│
├── requiredFields: [String]             — fields in the model that must be filled before this edge can execute
│
├── conditions: [EdgeCondition]          — optional: only show this edge if conditions are met
│     ├── field: String                  — field key from current or previous step data
│     ├── operator: EQUALS | NOT_EQUALS | GREATER_THAN | LESS_THAN | IS_EMPTY | IS_NOT_EMPTY
│     └── value: Object                  — comparison value
│
└── autoActions: [AutoAction]            — things that happen automatically when this edge is executed
      ├── type: SEND_NOTIFICATION | UPDATE_FIELD | CREATE_RECORD
      └── config: Map<String, Object>    — action-specific configuration
```

### 4. StepDataConfig (embedded in WorkflowStep)

Defines how a step gets data from previous steps. This is how "manager review" auto-loads the employee's leave data.

```
StepDataConfig
│
├── referencePreviousStep: Boolean       — if true, the step can read data from the previous step's record
├── previousStepFields: [String]         — which fields to pull from the previous step (empty = all)
│
├── autoFetchRules: [AutoFetchRule]      — automatically query data based on previous step values
│     ├── sourceStepId: String           — which step's data to use as lookup key
│     ├── sourceField: String            — field from that step (e.g., "employeeId")
│     ├── targetModelId: ObjectId        — which model to query
│     ├── targetLookupField: String      — field to match against (e.g., "_id" or "employeeId")
│     └── fieldsToFetch: [String]        — which fields to pull into this step's view
│
└── readOnlyFields: [String]             — fields that are displayed but cannot be edited at this step
```

### 5. WorkflowInstance

A running instance of a workflow — one per "leave request submitted", "stock order created", etc.

```
WorkflowInstance
│
├── _id: ObjectId
├── workflowDefinitionId: ObjectId
├── workflowVersion: Integer             — snapshot of which version was used
├── domainId: ObjectId
├── appId: ObjectId
│
├── status: ACTIVE | COMPLETED | CANCELLED
├── currentStepId: String                — ★ which step the instance is currently at
│
├── stepRecords: Map<String, ObjectId>   — ★ stepId → recordId in that step's DomainModel collection
│                                          e.g., { "step_1": "rec_abc", "step_2": "rec_def" }
│                                          Each step creates a real record in its DomainModel
│
├── history: [InstanceHistory]           — ★ append-only audit trail
│     ├── stepId: String
│     ├── edgeId: String                 — which edge was executed
│     ├── edgeName: String               — "Submit", "Approve", etc. (snapshot for readability)
│     ├── performedBy: ObjectId          — who did it
│     ├── performedAt: DateTime
│     ├── comment: String                — optional comment when executing edge
│     └── recordId: ObjectId             — the record created/updated at this step
│
├── startedBy: ObjectId                  — the user who initiated the workflow
├── startedAt: DateTime
└── completedAt: DateTime
```

**Key insight about `stepRecords`:** Each step in the workflow creates a REAL document in its DomainModel's collection. So a "Leave Request" step creates a record in the `leave_requests` model, and the "Manager Review" step creates a record in the `manager_reviews` model. The `stepRecords` map tracks which record belongs to which step. This means the data is real, queryable, and persists independently of the workflow.

---

## How Execution Works

### Starting a Workflow

```
1. User clicks "Start Leave Request" workflow
2. Engine loads the PUBLISHED WorkflowDefinition
3. Creates a new WorkflowInstance (status=ACTIVE, currentStepId=start step)
4. Returns the start step's DomainModel fields → frontend renders the form
5. User fills the form (employee name, leave dates, reason)
6. User sees a dropdown with available edges (filtered by their role)
   → Employee sees: ["Submit"]  (they don't see "Approve" or "Reject")
7. User selects "Submit" and clicks execute
```

### Executing an Edge

```
WorkflowEngine.executeEdge(instanceId, edgeId, formData, userId):

1. Load WorkflowInstance (must be ACTIVE)
2. Load WorkflowDefinition (by definitionId + version)
3. Find currentStep from definition
4. Find the edge by edgeId within currentStep.edges[]

5. PERMISSION CHECK:
   a. Check edge.allowedRoles against user's roles
   b. Check edge.allowedUserIds against userId  
   c. If edge.onlySubmitter == true → check userId == instance.startedBy
   d. If user has NO matching permission → throw FORBIDDEN

6. CONDITION CHECK:
   a. Evaluate edge.conditions against current step data
   b. If any condition fails → throw CONDITION_NOT_MET

7. VALIDATE FORM DATA:
   a. Validate formData against currentStep's DomainModel field rules
   b. Check edge.requiredFields are all filled
   c. If validation fails → return errors

8. SAVE RECORD:
   a. Create/update a document in the step's DomainModel collection
   b. Store: instance.stepRecords[currentStepId] = newRecordId

9. EXECUTE AUTO-ACTIONS:
   a. For each edge.autoActions → fire (notifications, field updates, etc.)

10. ADVANCE:
    a. If edge.isTerminal == true:
       → instance.status = COMPLETED
       → instance.completedAt = now
    b. Else:
       → instance.currentStepId = edge.targetStepId
       → Load next step's dataConfig
       → If referencePreviousStep → prepare data references
       → If autoFetchRules → execute queries to pre-load data

11. RECORD HISTORY:
    a. Append to instance.history: { stepId, edgeId, edgeName, performedBy, performedAt, comment, recordId }

12. SAVE and return updated instance
```

### What Users See — The Task Queue

When a manager logs in, they need to see "which workflow instances are waiting for me to act?"

```
WorkflowEngine.getMyTasks(userId, domainId, appId):

1. Get user's roles (from DomainGroupMember)
2. Find all ACTIVE WorkflowInstances for this domain+app
3. For each instance:
   a. Get currentStep from its definition
   b. Get edges[] from currentStep
   c. Filter edges where:
      - user's roles intersect with edge.allowedRoles
      - OR userId is in edge.allowedUserIds
      - OR (edge.onlySubmitter && userId == instance.startedBy)
   d. If user has at least ONE executable edge → include this instance in results
4. Return list of instances with:
   - instance summary (what workflow, who started it, when)
   - the current step's model data (so they can see what they're acting on)
   - available edges (only the ones this user can execute)
```

**This is critical**: the manager sees a list of pending instances. When they open one, they see the data from the current step (and referenced data from previous steps). Below the data, they see a dropdown with ONLY the edges they can execute (e.g., "Approve" and "Reject"). They fill any required fields, select an edge, and click execute.

### Data Flow Between Steps

When the workflow advances from Step A to Step B:

```
Step B can access Step A's data in three ways:

1. REFERENCE (referencePreviousStep: true)
   → Step B's form shows Step A's record data as read-only fields
   → e.g., Manager sees the leave dates the employee filled in

2. AUTO-FETCH (autoFetchRules)
   → Based on a field from Step A, automatically query another model
   → e.g., Step A has "employeeId" → auto-fetch employee's department, 
     remaining leave balance from the Employee model
   → These fetched fields appear in Step B's view

3. MANUAL (no config)
   → Step B is a completely independent form
   → Previous step's recordId is still stored in instance.stepRecords for reference
```

---

## Complete Leave Request Example

### Workflow Definition

```json
{
  "name": "Leave Request Approval",
  "slug": "leave-request-approval",
  "steps": [
    {
      "id": "step_leave",
      "modelId": "MODEL_ID_LEAVE_REQUEST",
      "name": "Leave Request",
      "order": 0,
      "isStart": true,
      "isEnd": false,
      "edges": [
        {
          "id": "edge_submit",
          "name": "Submit",
          "targetStepId": "step_manager",
          "isTerminal": false,
          "allowedRoles": ["employee"],
          "onlySubmitter": false,
          "requiredFields": ["startDate", "endDate", "reason"]
        },
        {
          "id": "edge_cancel",
          "name": "Cancel",
          "targetStepId": null,
          "isTerminal": true,
          "allowedRoles": [],
          "onlySubmitter": true,
          "requiredFields": []
        }
      ],
      "dataConfig": null
    },
    {
      "id": "step_manager",
      "modelId": "MODEL_ID_MANAGER_REVIEW",
      "name": "Manager Review",
      "order": 1,
      "isStart": false,
      "isEnd": false,
      "edges": [
        {
          "id": "edge_approve",
          "name": "Approve",
          "targetStepId": "step_hr",
          "isTerminal": false,
          "allowedRoles": ["manager"],
          "onlySubmitter": false,
          "requiredFields": []
        },
        {
          "id": "edge_reject",
          "name": "Reject",
          "targetStepId": null,
          "isTerminal": true,
          "allowedRoles": ["manager"],
          "onlySubmitter": false,
          "requiredFields": ["rejectionReason"]
        }
      ],
      "dataConfig": {
        "referencePreviousStep": true,
        "previousStepFields": ["employeeName", "startDate", "endDate", "reason"],
        "autoFetchRules": [
          {
            "sourceStepId": "step_leave",
            "sourceField": "employeeId",
            "targetModelId": "MODEL_ID_EMPLOYEE",
            "targetLookupField": "_id",
            "fieldsToFetch": ["department", "remainingLeaveDays", "managerName"]
          }
        ],
        "readOnlyFields": ["employeeName", "startDate", "endDate", "reason", "department", "remainingLeaveDays"]
      }
    },
    {
      "id": "step_hr",
      "modelId": "MODEL_ID_HR_CONFIRMATION",
      "name": "HR Confirmation",
      "order": 2,
      "isStart": false,
      "isEnd": false,
      "edges": [
        {
          "id": "edge_confirm",
          "name": "Confirm",
          "targetStepId": null,
          "isTerminal": true,
          "allowedRoles": ["hr"],
          "onlySubmitter": false,
          "requiredFields": []
        },
        {
          "id": "edge_sendback",
          "name": "Send Back",
          "targetStepId": "step_manager",
          "isTerminal": false,
          "allowedRoles": ["hr"],
          "onlySubmitter": false,
          "requiredFields": ["sendBackReason"]
        }
      ],
      "dataConfig": {
        "referencePreviousStep": true,
        "previousStepFields": [],
        "autoFetchRules": [],
        "readOnlyFields": ["employeeName", "startDate", "endDate", "reason", "managerDecision"]
      }
    }
  ]
}
```

### Execution Walkthrough

```
1. EMPLOYEE logs in → clicks "New Leave Request"
   → Sees Leave Request form (model fields: employee, dates, reason)
   → Dropdown shows: [Submit]  (cancel is also there since onlySubmitter matches)
   → Fills form, selects "Submit", clicks execute
   → Record created in leave_requests collection
   → Instance advances to step_manager

2. EMPLOYEE changes mind → opens the instance
   → Still sees "Cancel" edge (because onlySubmitter=true and they started it)
   → But the instance is now at step_manager, so cancel is on step_leave...
   → WAIT: we need to handle this — see "Edge on Start Step" note below

3. MANAGER logs in → sees dashboard: "2 leave requests pending"
   → Opens one → sees employee's leave data (read-only) + auto-fetched department info
   → Dropdown shows: [Approve, Reject]
   → Selects "Approve" → Instance advances to step_hr
   → History: { edge: "Approve", performedBy: managerId, ... }

4. HR logs in → sees dashboard: "1 approved leave pending confirmation"  
   → Opens it → sees all previous data (leave + manager decision)
   → Dropdown shows: [Confirm, Send Back]
   → Selects "Confirm" → edge.isTerminal=true → Instance status = COMPLETED
   → History: { edge: "Confirm", performedBy: hrId, ... }
```

---

## Handling Special Cases

### 1. Cancel by Submitter (from any point)

The "cancel" edge lives on the START step, but the instance has moved past it. Two approaches:

**Option A (recommended): Global edges on the instance level**
Add a `globalEdges` field to WorkflowDefinition — edges that can be executed regardless of current step:
```
globalEdges: [
  {
    id: "edge_cancel_global",
    name: "Cancel Request",
    isTerminal: true,
    onlySubmitter: true,
    conditions: [
      { field: "status", operator: "NOT_EQUALS", value: "COMPLETED" }
    ]
  }
]
```

**Option B: Allow "reachback" edges**
Each step can define edges that target a PREVIOUS step (going backwards). The cancel edge on step_leave has `targetStepId: null` and `isTerminal: true`, and is always available to the submitter as long as the instance is ACTIVE.

### 2. Conditional Edges

Sometimes an edge should only appear based on data. Example: "Approve without HR" should only be available if leave is ≤ 2 days:

```json
{
  "id": "edge_approve_direct",
  "name": "Approve (No HR needed)",
  "targetStepId": null,
  "isTerminal": true,
  "allowedRoles": ["manager"],
  "conditions": [
    { "field": "step_leave.leaveDays", "operator": "LESS_THAN", "value": 3 }
  ]
}
```

### 3. Send Back / Loop

When HR selects "Send Back", the instance goes BACK to `step_manager`. The manager sees it again in their queue with the HR's comment. They can approve/reject again. This creates a loop — the history records every pass through.

### 4. Same Model, Different Steps

Two steps CAN reference the SAME DomainModel. Example: an "Edit" step and a "Final Review" step both use the same model but with different edges and different read-only fields. The step creates a NEW record in that model each time it's visited.

---

## Changes to Existing Architecture

### Keep As-Is
- **DomainModel** — this IS the data layer AND the "node". No changes needed.
- **Domain / User / Auth** — no changes
- **Application hierarchy** — workflows live under applications
- **RBAC groups** — roles referenced by edge permissions come from existing domain groups

### Remove / Replace
- **Old WorkflowDefinition** (states[] + transitions[]) → replaced by new WorkflowDefinition with steps[]
- **Old WorkflowInstance** → replaced by new WorkflowInstance with stepRecords and edge-based history
- **CustomForm** (already deprecated) → fully remove

### Add New
- **WorkflowDefinition** (new version with steps + edges)
- **WorkflowInstance** (new version with stepRecords + edge history)
- **WorkflowEngineService** — the execution engine
- **WorkflowValidationService** — validates definition before publishing
- **WorkflowTaskService** — "what's pending for this user?"

### New Permissions

| Permission | Level | Purpose |
|---|---|---|
| `DOMAIN_MANAGE_WORKFLOWS` | Domain | Create/edit/delete/publish workflow definitions |
| `APP_MANAGE_WORKFLOWS` | App | Manage workflows within an app |
| `APP_START_WORKFLOW` | App | Start new workflow instances |
| `APP_VIEW_WORKFLOWS` | App | View running/completed instances |

Note: Edge-level permissions use ROLE NAMES (strings) that map to DomainGroup names. The engine checks if the user belongs to a group whose name matches one of the edge's `allowedRoles`.

---

## API Endpoints

### Workflow Definition (Builder)
```
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows              → create
GET    /adaptive/domains/{slug}/apps/{appSlug}/workflows              → list
GET    /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}     → get
PUT    /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}     → update
DELETE /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}     → delete
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}/publish  → publish
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}/archive  → archive
```

### Workflow Execution (Runtime)
```
POST   /adaptive/domains/{slug}/apps/{appSlug}/workflows/{wfSlug}/start         → start instance
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances                         → list instances
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/my-tasks                → ★ get user's pending tasks
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}            → get instance detail
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/view       → get current step view + available edges
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/execute    → ★ execute an edge
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/history    → get audit trail
```

### Key Request/Response Shapes

**POST .../execute** (execute an edge)
```json
// Request:
{
  "edgeId": "edge_approve",
  "formData": { "approvalNote": "Looks good" },
  "comment": "Approved for 5 days annual leave"
}

// Response:
{
  "instanceId": "...",
  "status": "ACTIVE",                    // or "COMPLETED" if terminal edge
  "currentStepId": "step_hr",            // new current step
  "previousEdge": "Approve",
  "nextStepName": "HR Confirmation"
}
```

**GET .../my-tasks** (user's pending tasks)
```json
// Response:
{
  "tasks": [
    {
      "instanceId": "inst_123",
      "workflowName": "Leave Request Approval",
      "currentStepName": "Manager Review",
      "startedBy": { "id": "...", "name": "John Smith" },
      "startedAt": "2024-01-15T09:00:00Z",
      "waitingSince": "2024-01-15T09:05:00Z",
      "availableEdges": [
        { "id": "edge_approve", "name": "Approve" },
        { "id": "edge_reject", "name": "Reject" }
      ],
      "summary": {                       // key fields from current step's data
        "employeeName": "John Smith",
        "leaveDates": "Jan 20-24",
        "department": "Engineering"
      }
    }
  ]
}
```

---

## Phase 1 Implementation Order

1. **WorkflowDefinition + WorkflowStep + WorkflowEdge** — MongoDB documents + CRUD APIs
2. **WorkflowValidationService** — validate definition structure before publish
3. **WorkflowEngineService** — start, execute edge, advance
4. **WorkflowInstance** — runtime storage with stepRecords + history
5. **WorkflowTaskService** — "my tasks" query (filter by role + edge permissions)
6. **Data referencing** — referencePreviousStep + readOnlyFields
7. **Auto-fetch rules** — query related data based on previous step values
8. **Global edges** — cancel from any point

### Phase 2 (Later)
- Conditional edges (show/hide based on data values)
- AutoActions (notifications, field updates)
- Parallel approval (multiple approvers at same step)
- SLA / timeout tracking
- Dashboard analytics
- Drag-and-drop visual builder frontend
