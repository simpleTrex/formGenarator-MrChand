export type WorkflowStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type InstanceStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type ConditionOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'LESS_THAN'
  | 'GREATER_EQUAL'
  | 'LESS_EQUAL'
  | 'IS_EMPTY'
  | 'IS_NOT_EMPTY'
  | 'CONTAINS';
export type AutoActionType = 'SEND_NOTIFICATION' | 'UPDATE_FIELD' | 'CREATE_RECORD';

export type DomainFieldType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATETIME'
  | 'REFERENCE'
  | 'EMPLOYEE_REFERENCE'
  | 'OBJECT'
  | 'ARRAY';

export interface DomainModelField {
  key: string;
  type: DomainFieldType;
  required?: boolean;
  unique?: boolean;
  config?: Record<string, any>;
}

export interface EdgeCondition {
  field: string;
  operator: ConditionOperator;
  value: any;
}

export interface AutoAction {
  type: AutoActionType;
  config: Record<string, any>;
}

export interface WorkflowEdge {
  id: string;
  name: string;
  targetStepId?: string | null;
  terminal: boolean;
  allowedRoles: string[];
  allowedUserIds: string[];
  onlySubmitter: boolean;
  requiredFields: string[];
  conditions: EdgeCondition[];
  autoActions: AutoAction[];
}

export interface AutoFetchRule {
  sourceStepId: string;
  sourceField: string;
  targetField: string;
}

export interface StepDataConfig {
  referencePreviousStep: boolean;
  reuseFromStepId?: string;
  previousStepFields: string[];
  autoFetchRules: AutoFetchRule[];
  readOnlyFields: string[];
}

export interface WorkflowStep {
  id: string;
  modelId?: string;
  name: string;
  order: number;
  start: boolean;
  end: boolean;
  fields: DomainModelField[];
  edges: WorkflowEdge[];
  dataConfig?: StepDataConfig;
  positionX?: number;
  positionY?: number;
}

export interface WorkflowDefinition {
  id?: string;
  domainId: string;
  appId: string;
  name: string;
  slug: string;
  description?: string;
  version: number;
  status: WorkflowStatus;
  steps: WorkflowStep[];
  globalEdges: WorkflowEdge[];
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface InstanceHistory {
  stepId: string;
  edgeId?: string | null;
  edgeName: string;
  performedBy: string;
  performedByName?: string;
  performedAt: string;
  comment?: string;
  recordId?: string;
  formData?: Record<string, any>;
}

export interface WorkflowInstance {
  id?: string;
  workflowDefinitionId: string;
  workflowVersion: number;
  domainId: string;
  appId: string;
  status: InstanceStatus;
  currentStepId: string;
  stepRecords: Record<string, any>;
  history: InstanceHistory[];
  startedBy: string;
  startedAt: string;
  completedAt?: string;
}

export interface WorkflowDefinitionResponse {
  workflow: WorkflowDefinition;
  stepCount: number;
  valid: boolean;
  validationErrors: string[];
}

export interface WorkflowInstanceResponse {
  instanceId: string;
  workflowDefinitionId: string;
  workflowVersion: number;
  status: string;
  currentStepId: string;
  currentStepName?: string;
  startedBy: string;
  startedAt: string;
  completedAt?: string;
  availableEdges?: WorkflowEdge[];
  summary?: Record<string, any>;
}

export interface StepEdgeView {
  id: string;
  name: string;
  disabled: boolean;
  disabledReason?: string;
}

export interface StepViewResponse {
  instanceId: string;
  stepId: string;
  stepName: string;
  modelFields: DomainModelField[];
  currentData: Record<string, any>;
  referencedData: Record<string, any>;
  mappedData: Record<string, any>;
  readOnlyFields: string[];
  availableEdges: StepEdgeView[];
  history: InstanceHistory[];
}

export interface ExecuteEdgeResponse {
  instanceId: string;
  status: string;
  currentStepId?: string;
  previousEdge?: string;
  nextStepName?: string;
}

export interface HistoryResponse {
  history: InstanceHistory[];
}

export interface TaskResponse {
  instanceId: string;
  workflowName: string;
  currentStepName: string;
  startedBy: Record<string, any>;
  startedAt: string;
  waitingSince: string;
  availableEdges: Array<Record<string, any>>;
  summary: Record<string, any>;
}

export interface TaskListResponse {
  count: number;
  tasks: TaskResponse[];
}

// Backward-compatible aliases to minimize cross-file churn.
export type ProcessDefinition = WorkflowDefinition;
export type ProcessInstance = WorkflowInstance;
export type ProcessDefinitionResponse = WorkflowDefinitionResponse;
export type ProcessInstanceResponse = WorkflowInstanceResponse;
export type NodeViewResponse = StepViewResponse;

export const STATUS_BADGE_CLASS: Record<WorkflowStatus, string> = {
  DRAFT: 'badge-draft',
  PUBLISHED: 'badge-published',
  ARCHIVED: 'badge-archived',
};

export const INSTANCE_STATUS_BADGE_CLASS: Record<InstanceStatus, string> = {
  ACTIVE: 'badge-published',
  COMPLETED: 'badge-completed',
  CANCELLED: 'badge-archived',
};
