# Workflow Engine Redesign — Architecture Proposal

## The Problem With Your Current Model

Your existing `WorkflowDefinition` uses a **state-machine** pattern (states + transitions). This is good for approval flows (Draft → Pending → Approved), but it cannot express what you actually need:

| What you need | Current model supports? |
|---|---|
| Multi-page forms with fields | ❌ No — states have no UI definition |
| Conditional page routing (like Google Forms) | ❌ No — transitions are role-based, not data-based |
| CRUD operations on data models (add stock, view list) | ❌ No — transitions only move state |
| Drag-and-drop visual builder | ⚠️ Partial — has x/y positions but nothing else |
| Reusable across any business process | ⚠️ Partial — too tied to approval patterns |

**Recommendation:** Replace `WorkflowDefinition` + `WorkflowInstance` with the **Node-Edge Process Model** below. Keep your `DomainModel` as-is — it becomes the data backbone.

---

## Proposed Architecture: Node-Edge Process Model

### Core Idea

A process is a **directed graph** of **nodes** (what the user sees/does) connected by **edges** (how they navigate between nodes). This maps directly to a drag-and-drop canvas.

```
┌─────────┐     ┌──────────────┐     ┌──────────────┐     ┌─────────┐
│  START   │────▶│  Stock Form  │────▶│  Review Page │────▶│   END   │
│  (auto)  │     │  (form page) │     │  (data view) │     │ (done)  │
└─────────┘     └──────────────┘     └──────────────┘     └─────────┘
                       │
                       │ condition: quantity > 100
                       ▼
                ┌──────────────┐
                │  Approval    │
                │  (approval)  │
                └──────────────┘
```

---

## Data Model (MongoDB Collections)

### 1. ProcessDefinition (replaces WorkflowDefinition)

```
ProcessDefinition
│
├── _id: ObjectId
├── domainId: ObjectId
├── appId: ObjectId
├── name: String                    — "Stock Intake Process"
├── slug: String                    — "stock-intake-process"
├── description: String
├── version: Integer                — starts at 1, increments on publish
├── status: DRAFT | PUBLISHED | ARCHIVED
├── linkedModelIds: ObjectId[]      — which DomainModels this process reads/writes
│
├── nodes: [ProcessNode]            — ★ the visual blocks on the canvas
│
├── edges: [ProcessEdge]            — ★ the connections (arrows) between nodes
│
├── settings: {
│     allowSaveDraft: Boolean       — can users save mid-process?
│     requireAuth: Boolean          — must be logged in?
│     createdBy: ObjectId
│   }
│
├── createdAt: DateTime
└── updatedAt: DateTime
```

### 2. ProcessNode (embedded in ProcessDefinition.nodes[])

Every block on the drag-and-drop canvas is a node. The `type` determines what it does.

```
ProcessNode
│
├── id: String                      — unique within this process (e.g., "node_1")
├── type: NodeType                  — see table below
├── name: String                    — display label ("Enter Stock Details")
├── positionX: Double               — canvas X coordinate
├── positionY: Double               — canvas Y coordinate
│
├── config: NodeConfig              — ★ varies by type (see below)
│
└── permissions: {                  — who can interact with this node
      allowedRoles: String[]
      allowedUserIds: ObjectId[]
    }
```

**Node Types:**

| NodeType | Purpose | What user sees |
|---|---|---|
| `START` | Entry point (exactly 1 per process) | Nothing — auto-proceeds |
| `FORM_PAGE` | A page with input fields | Form with text inputs, dropdowns, date pickers, etc. |
| `DATA_VIEW` | Shows data from a DomainModel | Table/list view with optional search/filter |
| `DATA_ACTION` | Performs CRUD silently (no UI) | Nothing — runs in background, then proceeds |
| `CONDITION` | Branches based on data | Nothing visible — routes to different nodes |
| `APPROVAL` | Pause for someone to approve/reject | Approval form with approve/reject buttons |
| `NOTIFICATION` | Sends a notification | Nothing visible — fires and proceeds |
| `END` | Terminal node (can be multiple) | Success/completion message |

### 3. NodeConfig (varies by node type)

#### FORM_PAGE config
```
{
  elements: [PageElement]           — the form fields on this page
  layout: "SINGLE_COLUMN" | "TWO_COLUMN"
  submitLabel: String               — button text ("Next", "Submit", "Save")
}
```

#### PageElement (embedded in FORM_PAGE config)
```
PageElement
│
├── id: String                      — unique within the page ("el_1")
├── type: ElementType               — see below
├── label: String                   — "Product Name"
├── order: Integer                  — display order (0, 1, 2...)
│
├── binding: {                      — ★ connects this field to a DomainModel field
│     modelId: ObjectId             — which DomainModel
│     fieldKey: String              — which field in that model (e.g., "productName")
│   }
│
├── validation: {
│     required: Boolean
│     min: Number                   — for NUMBER type
│     max: Number
│     minLength: Integer            — for STRING type
│     maxLength: Integer
│     pattern: String               — regex pattern
│   }
│
├── config: {                       — type-specific settings
│     placeholder: String
│     defaultValue: any
│     options: [                    — for SELECT / RADIO / CHECKBOX
│       { label: String, value: String }
│     ]
│     multiple: Boolean             — for SELECT (multi-select)
│     accept: String                — for FILE_UPLOAD ("image/*", ".pdf")
│   }
│
└── visibilityRule: {               — show/hide this field based on another field
      dependsOn: String             — another element's id
      operator: "EQUALS" | "NOT_EQUALS" | "GREATER_THAN" | "LESS_THAN" | "IS_EMPTY" | "IS_NOT_EMPTY"
      value: any
    }
```

**Element Types (keep it primitive as you said):**

| ElementType | Maps to | Notes |
|---|---|---|
| `TEXT_INPUT` | `<input type="text">` | Single line text |
| `TEXT_AREA` | `<textarea>` | Multi-line text |
| `NUMBER_INPUT` | `<input type="number">` | Numeric value |
| `DATE_PICKER` | `<input type="date">` | Date selection |
| `DATETIME_PICKER` | `<input type="datetime-local">` | Date + time |
| `SELECT` | `<select>` | Dropdown (single or multi) |
| `RADIO` | Radio buttons | Single selection from options |
| `CHECKBOX` | Checkboxes | Multiple selection or boolean |
| `FILE_UPLOAD` | File input | Stores reference to uploaded file |
| `LABEL` | Static text | Display only — instructions or headings |
| `HIDDEN` | Hidden field | Carries data without showing it |

#### DATA_VIEW config
```
{
  modelId: ObjectId                 — which DomainModel to show
  displayFields: String[]           — which fields to show as columns ["name", "quantity", "price"]
  allowSearch: Boolean
  allowSort: Boolean
  pageSize: Integer                 — rows per page (default 10)
  filters: [                        — pre-applied filters
    { fieldKey: String, operator: String, value: any }
  ]
  actions: ["VIEW_DETAIL" | "EDIT" | "DELETE"]   — row-level actions allowed
}
```

#### DATA_ACTION config (silent CRUD — no UI)
```
{
  operation: "CREATE" | "UPDATE" | "DELETE"
  modelId: ObjectId                 — target DomainModel
  fieldMappings: [                  — where each field gets its value
    {
      targetField: String           — field in the DomainModel
      source: "FORM_FIELD" | "STATIC" | "CONTEXT"
      value: String                 — element id, static value, or context key
    }
  ]
}
```

#### CONDITION config
```
{
  rules: [                          — evaluated top to bottom, first match wins
    {
      id: String
      conditions: [                 — ALL must be true (AND logic)
        {
          field: String             — element id or "context.someKey"
          operator: "EQUALS" | "NOT_EQUALS" | "GREATER_THAN" | "LESS_THAN" |
                    "CONTAINS" | "IS_EMPTY" | "IS_NOT_EMPTY"
          value: any
        }
      ]
      targetEdgeId: String          — which edge to follow if conditions match
    }
  ]
  defaultEdgeId: String             — fallback if no rules match
}
```

#### APPROVAL config
```
{
  approverRoles: String[]           — who can approve
  approverUserIds: ObjectId[]       — specific users
  formElements: [PageElement]       — optional fields (comment, reason)
  actions: [                        — buttons shown to the approver
    { id: "approve", label: "Approve", style: "PRIMARY" },
    { id: "reject", label: "Reject", style: "DANGER" }
  ]
  timeoutHours: Integer             — auto-escalate after N hours (optional)
}
```

#### NOTIFICATION config
```
{
  channel: "EMAIL" | "IN_APP"
  recipientType: "STATIC_USER" | "ROLE" | "DYNAMIC_FIELD"
  recipientValue: String            — userId, role name, or element id
  subject: String                   — supports {{fieldKey}} placeholders
  body: String                      — supports {{fieldKey}} placeholders
}
```

### 4. ProcessEdge (embedded in ProcessDefinition.edges[])

```
ProcessEdge
│
├── id: String                      — unique within process ("edge_1")
├── fromNodeId: String              — source node id
├── toNodeId: String                — target node id
├── label: String                   — display label on the arrow ("If approved", "Next")
│
└── conditionRef: String            — if fromNode is CONDITION type,
                                      this matches a rule's targetEdgeId
```

**Validation rules for edges:**
- START node: exactly 1 outgoing edge, 0 incoming
- END node: 0 outgoing edges, 1+ incoming
- FORM_PAGE: exactly 1 outgoing edge (unless connected to CONDITION)
- CONDITION: N outgoing edges (one per rule + default)
- APPROVAL: 2+ outgoing edges (one per action: approve, reject, etc.)
- Every non-START node must be reachable from START
- Every non-END path must eventually reach an END

### 5. ProcessInstance (replaces WorkflowInstance)

```
ProcessInstance
│
├── _id: ObjectId
├── processDefinitionId: ObjectId
├── processVersion: Integer         — snapshot which version was used
├── domainId: ObjectId
├── appId: ObjectId
│
├── status: "ACTIVE" | "COMPLETED" | "CANCELLED" | "PAUSED"
├── currentNodeId: String           — where the user currently is
├── previousNodeId: String
│
├── data: Map<String, Object>       — ★ all collected form data lives here
│                                     key = "nodeId.elementId", value = user input
│
├── createdRecordIds: [             — tracks records created by DATA_ACTION nodes
│     { modelId: ObjectId, recordId: ObjectId, createdAt: DateTime }
│   ]
│
├── assignedTo: {                   — for APPROVAL nodes
│     userId: ObjectId
│     role: String
│     assignedAt: DateTime
│   }
│
├── history: [                      — ★ append-only audit trail
│     {
│       nodeId: String
│       action: String              — "ENTERED" | "SUBMITTED" | "APPROVED" | "REJECTED" | "AUTO_ROUTED"
│       performedBy: ObjectId
│       performedAt: DateTime
│       data: Map<String, Object>   — snapshot of data at this point
│       comment: String
│     }
│   ]
│
├── startedBy: ObjectId
├── startedAt: DateTime
├── completedAt: DateTime
│
└── draftData: Map<String, Object>  — unsaved form data (if allowSaveDraft=true)
```

---

## How Execution Works (the Engine)

### ProcessEngine Service

```
ProcessEngine
│
├── startProcess(processDefinitionId, userId)
│     1. Load published ProcessDefinition
│     2. Create ProcessInstance (status=ACTIVE, currentNodeId=START node)
│     3. Auto-advance from START → first real node
│     4. Record history entry
│     5. Return instance with first node's config
│
├── submitNode(instanceId, nodeId, formData, userId)
│     1. Load instance + definition
│     2. Verify: instance.currentNodeId == nodeId
│     3. Verify: user has permission for this node
│     4. Based on node type:
│        ├── FORM_PAGE  → validate formData against elements' validation rules
│        │                 merge formData into instance.data
│        ├── APPROVAL   → record approve/reject action
│        └── DATA_VIEW  → no data to collect (user just viewed)
│     5. Find outgoing edge(s)
│     6. Advance to next node (may chain through silent nodes)
│     7. Save + return updated instance
│
├── advanceToNext(instance, fromNodeId)     [PRIVATE — internal routing]
│     1. Find outgoing edges from current node
│     2. If next node is CONDITION:
│        → evaluate rules against instance.data
│        → pick matching edge
│        → recurse: advanceToNext(instance, conditionNodeId)
│     3. If next node is DATA_ACTION:
│        → execute CRUD operation
│        → record created/updated recordId
│        → recurse: advanceToNext(instance, dataActionNodeId)
│     4. If next node is NOTIFICATION:
│        → fire notification async
│        → recurse: advanceToNext(instance, notificationNodeId)
│     5. If next node is FORM_PAGE | APPROVAL | DATA_VIEW | END:
│        → STOP here (requires user interaction or is terminal)
│        → set instance.currentNodeId = this node
│     6. If END:
│        → set instance.status = COMPLETED
│
├── getNodeView(instanceId, userId)
│     → Returns the current node's config + any data needed to render it
│     → For DATA_VIEW: queries the DomainModel and returns records
│     → For FORM_PAGE: returns element definitions + any pre-filled values
│     → For APPROVAL: returns the approval form + business context
│
└── saveDraft(instanceId, nodeId, partialData, userId)
      → Saves to instance.draftData without advancing
```

### Execution Example: Stock Intake Process

```
User starts "Stock Intake Process"

1. ENGINE: Creates instance, lands on START
2. ENGINE: Auto-advances to "Enter Stock Details" (FORM_PAGE)
   → Returns form config to frontend

3. USER: Fills in { productName: "Widget A", quantity: 150, category: "Electronics" }
4. USER: Clicks "Next"

5. ENGINE: submitNode() validates form, merges into instance.data
6. ENGINE: advanceToNext() → hits CONDITION node
   → Evaluates: quantity > 100? YES
   → Follows "high_quantity" edge → "Manager Approval" (APPROVAL node)
   → STOPS (needs human interaction)

7. MANAGER: Sees approval task, clicks "Approve"

8. ENGINE: submitNode() records approval
9. ENGINE: advanceToNext() → hits DATA_ACTION node
   → Executes CREATE on "Stock" DomainModel
   → Maps: productName → name, quantity → quantity, category → category
   → Records created recordId
   → advanceToNext() → hits NOTIFICATION node
   → Sends email: "Stock Widget A added (150 units)"
   → advanceToNext() → hits END
   → Sets status = COMPLETED
```

---

## Changes to Existing Architecture

### Keep As-Is
- **DomainModel** — perfect as the data layer. Process nodes bind to these.
- **Domain / User / Auth** — no changes needed
- **RBAC groups + permissions** — extend with new process permissions
- **Application hierarchy** — processes live under applications

### Remove / Replace
- **WorkflowDefinition** → replaced by `ProcessDefinition`
- **WorkflowInstance** → replaced by `ProcessInstance`
- **states[]** → replaced by `nodes[]` (much more capable)
- **transitions[]** → replaced by `edges[]` + node configs
- **CustomForm** (already deprecated) → fully remove, use DomainModel

### Add New
- **ProcessDefinition** collection in MongoDB
- **ProcessInstance** collection in MongoDB
- **ProcessEngine** service (the execution engine)
- **ProcessValidationService** (validates definition before publishing)

### New Permissions to Add

| Permission | Level | Purpose |
|---|---|---|
| `DOMAIN_MANAGE_PROCESSES` | Domain | Create/edit/delete process definitions |
| `APP_MANAGE_PROCESSES` | App | Manage processes within an app |
| `APP_START_PROCESS` | App | Start new process instances |
| `APP_VIEW_PROCESSES` | App | View running/completed instances |

---

## API Endpoints

### Process Definition (Builder)

```
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes
GET    /adaptive/domains/{slug}/apps/{appSlug}/processes
GET    /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}
PUT    /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}
DELETE /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}/publish
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}/archive
```

### Process Execution (Runtime)

```
POST   /adaptive/domains/{slug}/apps/{appSlug}/processes/{processSlug}/start
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}
GET    /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/current-node
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/submit
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/save-draft
POST   /adaptive/domains/{slug}/apps/{appSlug}/instances/{instanceId}/cancel
```

---

## Frontend Visual Builder — What It Maps To

```
┌─────────────────────────────────────────────────────┐
│  CANVAS (drag-and-drop area)                        │
│                                                     │
│   [START] ──→ [Form Page] ──→ [Condition] ──→ ...  │
│                                    │                │
│                                    └──→ [Approval]  │
│                                                     │
│  Each block = a ProcessNode                         │
│  Each arrow = a ProcessEdge                         │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  RIGHT PANEL (when a node is selected)              │
│                                                     │
│  For FORM_PAGE:                                     │
│    → Drag-and-drop form elements                    │
│    → Each element binds to a DomainModel field      │
│    → Set validation rules                           │
│                                                     │
│  For CONDITION:                                     │
│    → Define rules (if field X = Y, go to node Z)    │
│                                                     │
│  For DATA_ACTION:                                   │
│    → Pick operation (Create/Update/Delete)           │
│    → Map fields from form data to model fields      │
│                                                     │
│  For DATA_VIEW:                                     │
│    → Pick model, choose columns, set filters        │
└─────────────────────────────────────────────────────┘
```

---

## Phase 1 Implementation Order

1. **ProcessDefinition + ProcessNode + ProcessEdge** — MongoDB schema + CRUD APIs
2. **ProcessValidationService** — validate graph structure before publish
3. **ProcessEngine** — startProcess, submitNode, advanceToNext
4. **ProcessInstance** — runtime storage + history
5. **FORM_PAGE + DATA_VIEW nodes** — the two most important node types
6. **CONDITION node** — conditional routing
7. **DATA_ACTION node** — CRUD on DomainModels
8. **APPROVAL node** — human-in-the-loop
9. **NOTIFICATION node** — can be deferred to Phase 2

### Phase 2 (Later)
- Calculated fields / expressions
- Sub-processes (a node that launches another process)
- Scheduled / timer-based triggers
- Parallel branches (AND split / AND join)
- Custom validation functions
- File attachment handling
- Process templates / marketplace
