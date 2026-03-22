// ── Enums ────────────────────────────────────────────────────────────────────

export type NodeType =
  | 'START' | 'FORM_PAGE' | 'DATA_VIEW' | 'DATA_ACTION'
  | 'CONDITION' | 'APPROVAL' | 'NOTIFICATION' | 'END';

export type ProcessStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type InstanceStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'PAUSED';

// ── Embedded ─────────────────────────────────────────────────────────────────

export interface NodePermissions {
  allowedRoles: string[];
  allowedUserIds: string[];
}

export interface ProcessNode {
  id: string;
  type: NodeType;
  name: string;
  positionX?: number;
  positionY?: number;
  config: Record<string, any>;
  permissions: NodePermissions;
}

export interface ProcessEdge {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  label?: string;
  conditionRef?: string;
}

export interface ProcessSettings {
  allowSaveDraft: boolean;
  requireAuth: boolean;
}

export interface ProcessDefinition {
  id?: string;
  domainId: string;
  appId: string;
  name: string;
  slug: string;
  description?: string;
  version: number;
  status: ProcessStatus;
  linkedModelIds: string[];
  nodes: ProcessNode[];
  edges: ProcessEdge[];
  settings: ProcessSettings;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

// ── Instance ─────────────────────────────────────────────────────────────────

export interface Assignment {
  userId: string;
  role: string;
  assignedAt: string;
}

export interface HistoryEntry {
  nodeId: string;
  action: string;
  performedBy: string;
  performedAt: string;
  data: Record<string, any>;
  comment?: string;
}

export interface CreatedRecord {
  modelId: string;
  recordId: string;
  createdAt: string;
}

export interface ProcessInstance {
  id?: string;
  processDefinitionId: string;
  processVersion: number;
  domainId: string;
  appId: string;
  status: InstanceStatus;
  currentNodeId: string;
  previousNodeId?: string;
  data: Record<string, any>;
  createdRecordIds: CreatedRecord[];
  assignedTo?: Assignment;
  history: HistoryEntry[];
  startedBy: string;
  startedAt: string;
  completedAt?: string;
  draftData: Record<string, any>;
}

// ── DTOs ─────────────────────────────────────────────────────────────────────

export interface ProcessDefinitionResponse {
  definition: ProcessDefinition;
  nodeCount: number;
  edgeCount: number;
  valid: boolean;
}

export interface ProcessInstanceResponse {
  instance: ProcessInstance;
  currentNode: ProcessNode | null;
}

export interface NodeViewResponse {
  nodeId: string;
  nodeName: string;
  nodeType: NodeType;
  config: Record<string, any>;
  prefilledData: Record<string, any>;
  records?: Record<string, any>[];
  availableActions: string[];
  completionMessage?: string;
}

// ── Form helpers ─────────────────────────────────────────────────────────────

export const NODE_TYPE_OPTIONS: { value: NodeType; label: string; description: string }[] = [
  { value: 'START',        label: 'Start',        description: 'Entry point — auto-proceeds' },
  { value: 'FORM_PAGE',    label: 'Form Page',    description: 'User fills in a form' },
  { value: 'DATA_VIEW',    label: 'Data View',    description: 'User views records from a model' },
  { value: 'DATA_ACTION',  label: 'Data Action',  description: 'Silent CRUD on a model' },
  { value: 'CONDITION',    label: 'Condition',    description: 'Branches based on data' },
  { value: 'APPROVAL',     label: 'Approval',     description: 'Pause for approve/reject' },
  { value: 'NOTIFICATION', label: 'Notification', description: 'Sends a notification (Phase 2)' },
  { value: 'END',          label: 'End',          description: 'Terminal node' },
];

export const STATUS_BADGE_CLASS: Record<ProcessStatus, string> = {
  DRAFT:     'badge-draft',
  PUBLISHED: 'badge-published',
  ARCHIVED:  'badge-archived',
};

export const INSTANCE_STATUS_BADGE_CLASS: Record<InstanceStatus, string> = {
  ACTIVE:    'badge-published',
  COMPLETED: 'badge-completed',
  CANCELLED: 'badge-archived',
  PAUSED:    'badge-draft',
};
