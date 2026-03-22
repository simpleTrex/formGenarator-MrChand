import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProcessService } from '../../../../core/services/process.service';
import { DomainService } from '../../../../core/services/domain.service';
import {
  ProcessNode, ProcessEdge, ProcessSettings,
  NODE_TYPE_OPTIONS, NodeType
} from '../../../../core/models/process.model';

// ── Config sub-types ─────────────────────────────────────────────────────────

export interface FormElement {
  id: string;
  type: string;
  label: string;
  validation: { required?: boolean; min?: number; max?: number };
  config: { placeholder?: string; options?: { value: string; label: string }[] };
}

export interface FieldMapping {
  targetField: string;
  source: 'FORM_FIELD' | 'STATIC_VALUE';
  value: string;
}

export interface ConditionRow {
  field: string;
  operator: string;
  value: string;
}

export interface ConditionRule {
  conditions: ConditionRow[];
  targetEdgeId: string;
}

export interface ApprovalAction {
  id: string;
  label: string;
}

export interface FlowLayoutNode {
  id: string;
  type: string;
  name: string;
  x: number;
  y: number;
  width: number;
  height: number;
  isDisconnected: boolean;
}

export interface FlowLayoutEdge {
  id: string;
  label?: string;
  path: string;
  midX: number;
  midY: number;
}

// ── Constants ────────────────────────────────────────────────────────────────

export const FORM_ELEMENT_TYPES = [
  { value: 'TEXT_INPUT',   label: 'Text Input' },
  { value: 'NUMBER_INPUT', label: 'Number Input' },
  { value: 'TEXT_AREA',    label: 'Text Area' },
  { value: 'SELECT',       label: 'Dropdown (Select)' },
  { value: 'DATE_PICKER',  label: 'Date Picker' },
  { value: 'CHECKBOX',     label: 'Checkbox' },
  { value: 'RADIO',        label: 'Radio Group' },
];

export const CONDITION_OPERATORS = [
  'EQUALS', 'NOT_EQUALS', 'GREATER_THAN', 'LESS_THAN',
  'CONTAINS', 'IS_EMPTY', 'IS_NOT_EMPTY',
];

@Component({
  selector: 'app-process-builder',
  templateUrl: './process-builder.component.html',
  styleUrls: ['./process-builder.component.css']
})
export class ProcessBuilderComponent implements OnInit {

  domainSlug = '';
  appSlug = '';

  hasExisting = false;
  processStatus: string | null = null;

  // Definition fields
  name = '';
  description = '';
  linkedModelIds: string[] = [];
  nodes: ProcessNode[] = [];
  edges: ProcessEdge[] = [];
  settings: ProcessSettings = { allowSaveDraft: false, requireAuth: true };

  // Available models (loaded from API)
  availableModels: any[] = [];
  modelsLoading = false;

  // UI state
  loading = false;
  saving = false;
  error = '';
  successMsg = '';
  validationErrors: string[] = [];
  activeTab: 'nodes' | 'edges' | 'settings' | 'flow' = 'nodes';
  showGuide = false;
  guideSection: 'overview' | 'nodes' | 'edges' | 'conditions' | 'tips' = 'overview';

  // Node form
  showNodeForm = false;
  editingNodeIndex = -1;
  nodeForm: ProcessNode = this.emptyNode();

  // ── FORM_PAGE config state ─────────────────────────────────────────────────
  formElements: FormElement[] = [];
  formSubmitLabel = 'Submit';
  showElementForm = false;
  editingElementIndex = -1;
  elementForm: FormElement = this.emptyElement();
  newSelectOption = { value: '', label: '' };
  importModelId = '';

  // ── DATA_ACTION config state ───────────────────────────────────────────────
  daModelId = '';
  daOperation: 'CREATE' | 'UPDATE' | 'DELETE' = 'CREATE';
  daMappings: FieldMapping[] = [];

  // ── DATA_VIEW config state ─────────────────────────────────────────────────
  dvModelId = '';
  dvDisplayFields: string[] = [];

  // ── CONDITION config state ─────────────────────────────────────────────────
  conditionRules: ConditionRule[] = [];
  conditionDefaultEdge = '';

  // ── APPROVAL config state ──────────────────────────────────────────────────
  approvalActions: ApprovalAction[] = [];

  // ── Ancestor field refs (for DATA_ACTION / CONDITION dropdowns) ────────────
  ancestorFieldRefs: { ref: string; label: string }[] = [];

  // ── Flow preview state ────────────────────────────────────────────────────
  flowNodes: FlowLayoutNode[] = [];
  flowEdges: FlowLayoutEdge[] = [];
  disconnectedNodeIds: string[] = [];

  // Edge form
  showEdgeForm = false;
  editingEdgeIndex = -1;
  edgeForm: ProcessEdge = this.emptyEdge();

  nodeTypeOptions = NODE_TYPE_OPTIONS;
  formElementTypes = FORM_ELEMENT_TYPES;
  conditionOperators = CONDITION_OPERATORS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private processService: ProcessService,
    private domainService: DomainService,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug    = this.route.snapshot.params['appSlug'];
    this.loadModels();
    this.load();
  }

  // ── Load process ───────────────────────────────────────────────────────────

  load(): void {
    this.loading = true;
    this.processService.getProcess(this.domainSlug, this.appSlug).subscribe({
      next: (res) => {
        const d = res.definition;
        this.name           = d.name;
        this.description    = d.description || '';
        this.linkedModelIds = d.linkedModelIds || [];
        this.nodes          = d.nodes || [];
        this.edges          = d.edges || [];
        this.settings       = d.settings || { allowSaveDraft: false, requireAuth: true };
        this.processStatus  = d.status;
        this.hasExisting    = true;
        this.loading        = false;
      },
      error: (err: any) => {
        if (err?.status === 404) {
          this.hasExisting   = false;
          this.processStatus = null;
          this.seedDefaults();
        } else {
          this.error = err?.error?.message || 'Failed to load process';
        }
        this.loading = false;
      }
    });
  }

  private seedDefaults(): void {
    this.nodes = [
      { id: 'node_start', type: 'START', name: 'Start', positionX: 0,   positionY: 0, config: {}, permissions: { allowedRoles: [], allowedUserIds: [] } },
      { id: 'node_end',   type: 'END',   name: 'End',   positionX: 600, positionY: 0, config: {}, permissions: { allowedRoles: [], allowedUserIds: [] } },
    ];
  }

  // ── Models ─────────────────────────────────────────────────────────────────

  loadModels(): void {
    this.modelsLoading = true;
    this.domainService.getDomainModels(this.domainSlug, this.appSlug).subscribe({
      next: (models: any[]) => { this.availableModels = models || []; this.modelsLoading = false; },
      error: () => { this.modelsLoading = false; }
    });
  }

  toggleModel(modelId: string): void {
    this.linkedModelIds = this.isModelLinked(modelId)
      ? this.linkedModelIds.filter(id => id !== modelId)
      : [...this.linkedModelIds, modelId];
  }

  isModelLinked(modelId: string): boolean {
    return this.linkedModelIds.includes(modelId);
  }

  getLinkedModels(): any[] {
    return this.availableModels.filter(m => this.linkedModelIds.includes(m.id));
  }

  getModelById(id: string): any {
    return this.availableModels.find(m => m.id === id);
  }

  getModelFields(modelId: string): any[] {
    return this.getModelById(modelId)?.fields || [];
  }

  // ── Nodes ─────────────────────────────────────────────────────────────────

  openAddNode(): void {
    this.editingNodeIndex = -1;
    this.nodeForm = this.emptyNode();
    this.clearNodeConfigState();
    this.ancestorFieldRefs = [];
    this.showNodeForm = true;
  }

  openEditNode(index: number): void {
    this.editingNodeIndex = index;
    const n = this.nodes[index];
    this.nodeForm = { ...n, config: { ...n.config }, permissions: { ...n.permissions } };
    this.deserializeNodeConfig(n);
    this.ancestorFieldRefs = this.getAncestorFieldRefs(n.id);
    this.showNodeForm = true;
  }

  onNodeTypeChange(): void {
    this.clearNodeConfigState();
  }

  private deserializeNodeConfig(node: ProcessNode): void {
    this.clearNodeConfigState();
    const c = node.config || {};
    switch (node.type) {
      case 'FORM_PAGE':
        this.formElements = (c['elements'] || []).map((el: any) => ({
          id: el.id || '',
          type: el.type || 'TEXT_INPUT',
          label: el.label || '',
          validation: { ...(el.validation || {}) },
          config: { ...(el.config || {}), options: [...(el.config?.options || [])] },
        }));
        this.formSubmitLabel = c['submitLabel'] || 'Submit';
        break;

      case 'DATA_ACTION':
        this.daModelId    = c['modelId'] || '';
        this.daOperation  = c['operation'] || 'CREATE';
        this.daMappings   = (c['fieldMappings'] || []).map((m: any) => ({ ...m }));
        break;

      case 'DATA_VIEW':
        this.dvModelId       = c['modelId'] || '';
        this.dvDisplayFields = [...(c['displayFields'] || [])];
        break;

      case 'CONDITION':
        this.conditionRules = (c['rules'] || []).map((r: any) => ({
          conditions: (r.conditions || []).map((co: any) => ({ ...co })),
          targetEdgeId: r.targetEdgeId || '',
        }));
        this.conditionDefaultEdge = c['defaultEdgeId'] || '';
        break;

      case 'APPROVAL':
        this.approvalActions = (c['actions'] || []).map((a: any) => ({ ...a }));
        break;
    }
  }

  private serializeNodeConfig(): Record<string, any> {
    switch (this.nodeForm.type) {
      case 'FORM_PAGE':
        return { elements: this.formElements, submitLabel: this.formSubmitLabel };
      case 'DATA_ACTION':
        return { operation: this.daOperation, modelId: this.daModelId, fieldMappings: this.daMappings };
      case 'DATA_VIEW':
        return { modelId: this.dvModelId, displayFields: this.dvDisplayFields };
      case 'CONDITION':
        return { rules: this.conditionRules, defaultEdgeId: this.conditionDefaultEdge };
      case 'APPROVAL':
        return { actions: this.approvalActions };
      default:
        return {};
    }
  }

  private clearNodeConfigState(): void {
    this.formElements       = [];
    this.formSubmitLabel    = 'Submit';
    this.showElementForm    = false;
    this.editingElementIndex = -1;
    this.elementForm        = this.emptyElement();
    this.importModelId      = '';
    this.daModelId          = '';
    this.daOperation        = 'CREATE';
    this.daMappings         = [];
    this.dvModelId          = '';
    this.dvDisplayFields    = [];
    this.conditionRules     = [];
    this.conditionDefaultEdge = '';
    this.approvalActions    = [];
    this.ancestorFieldRefs  = [];
  }

  saveNode(): void {
    if (!this.nodeForm.id.trim() || !this.nodeForm.type || !this.nodeForm.name.trim()) {
      this.error = 'Node id, type, and name are required';
      return;
    }
    this.nodeForm.config = this.serializeNodeConfig();
    if (this.editingNodeIndex >= 0) {
      this.nodes = this.nodes.map((n, i) => i === this.editingNodeIndex ? { ...this.nodeForm } : n);
    } else {
      if (this.nodes.find(n => n.id === this.nodeForm.id)) {
        this.error = 'Node id already exists';
        return;
      }
      this.nodes = [...this.nodes, { ...this.nodeForm }];
    }
    this.showNodeForm = false;
    this.error = '';
  }

  removeNode(index: number): void {
    const nodeId = this.nodes[index].id;
    this.nodes = this.nodes.filter((_, i) => i !== index);
    this.edges = this.edges.filter(e => e.fromNodeId !== nodeId && e.toNodeId !== nodeId);
  }

  cancelNodeForm(): void { this.showNodeForm = false; this.error = ''; }

  // ── FORM_PAGE: elements ───────────────────────────────────────────────────

  openAddElement(): void {
    this.editingElementIndex = -1;
    this.elementForm = this.emptyElement();
    this.showElementForm = true;
  }

  openEditElement(index: number): void {
    this.editingElementIndex = index;
    const el = this.formElements[index];
    this.elementForm = {
      id: el.id,
      type: el.type,
      label: el.label,
      validation: { ...el.validation },
      config: { placeholder: el.config?.placeholder, options: [...(el.config?.options || [])] },
    };
    this.showElementForm = true;
  }

  saveElement(): void {
    if (!this.elementForm.id.trim() || !this.elementForm.label.trim()) {
      return;
    }
    const clean: FormElement = {
      id: this.elementForm.id.trim(),
      type: this.elementForm.type,
      label: this.elementForm.label.trim(),
      validation: {},
      config: {},
    };
    if (this.elementForm.validation?.required) clean.validation['required'] = true;
    if (this.elementForm.validation?.min != null) clean.validation['min'] = this.elementForm.validation.min;
    if (this.elementForm.validation?.max != null) clean.validation['max'] = this.elementForm.validation.max;
    if (this.elementForm.config?.placeholder) clean.config['placeholder'] = this.elementForm.config.placeholder;
    if ((this.elementForm.config?.options || []).length) clean.config['options'] = this.elementForm.config.options;

    if (this.editingElementIndex >= 0) {
      this.formElements = this.formElements.map((el, i) =>
        i === this.editingElementIndex ? clean : el);
    } else {
      this.formElements = [...this.formElements, clean];
    }
    this.showElementForm = false;
  }

  cancelElementForm(): void { this.showElementForm = false; }

  removeElement(index: number): void {
    this.formElements = this.formElements.filter((_, i) => i !== index);
  }

  moveElement(index: number, dir: -1 | 1): void {
    const newIdx = index + dir;
    if (newIdx < 0 || newIdx >= this.formElements.length) return;
    const arr = [...this.formElements];
    [arr[index], arr[newIdx]] = [arr[newIdx], arr[index]];
    this.formElements = arr;
  }

  addSelectOption(): void {
    if (!this.newSelectOption.value.trim()) return;
    this.elementForm.config.options = [
      ...(this.elementForm.config.options || []),
      { value: this.newSelectOption.value.trim(), label: this.newSelectOption.label.trim() || this.newSelectOption.value.trim() },
    ];
    this.newSelectOption = { value: '', label: '' };
  }

  removeSelectOption(index: number): void {
    this.elementForm.config.options = (this.elementForm.config.options || []).filter((_, i) => i !== index);
  }

  importFromModel(modelId: string): void {
    const model = this.getModelById(modelId);
    if (!model) return;
    let added = 0;
    for (const field of model.fields) {
      if (!this.formElements.find(el => el.id === field.key)) {
        this.formElements.push({
          id: field.key,
          type: this.fieldTypeToElementType(field.type),
          label: field.key.replace(/_/g, ' ').replace(/\b\w/g, (c: string) => c.toUpperCase()),
          validation: { required: !!field.required },
          config: {},
        });
        added++;
      }
    }
    this.importModelId = '';
  }

  private fieldTypeToElementType(type: string): string {
    const map: Record<string, string> = {
      STRING: 'TEXT_INPUT', NUMBER: 'NUMBER_INPUT',
      BOOLEAN: 'CHECKBOX',  DATE: 'DATE_PICKER', DATETIME: 'DATE_PICKER',
    };
    return map[type] || 'TEXT_INPUT';
  }

  hasOptions(type: string): boolean    { return type === 'SELECT' || type === 'RADIO'; }
  hasPlaceholder(type: string): boolean { return type === 'TEXT_INPUT' || type === 'TEXT_AREA'; }
  hasMinMax(type: string): boolean      { return type === 'NUMBER_INPUT'; }

  // ── DATA_ACTION ────────────────────────────────────────────────────────────

  onDaModelChange(): void {
    const fields = this.getModelFields(this.daModelId);
    this.daMappings = fields.map((f: any) => ({
      targetField: f.key,
      source: 'FORM_FIELD' as const,
      value: '',
    }));
  }

  // ── DATA_VIEW ─────────────────────────────────────────────────────────────

  isDvFieldSelected(key: string): boolean { return this.dvDisplayFields.includes(key); }

  toggleDvField(key: string): void {
    this.dvDisplayFields = this.isDvFieldSelected(key)
      ? this.dvDisplayFields.filter(f => f !== key)
      : [...this.dvDisplayFields, key];
  }

  // ── CONDITION ─────────────────────────────────────────────────────────────

  addConditionRule(): void {
    this.conditionRules = [...this.conditionRules, {
      conditions: [{ field: '', operator: 'EQUALS', value: '' }],
      targetEdgeId: '',
    }];
  }

  removeConditionRule(index: number): void {
    this.conditionRules = this.conditionRules.filter((_, i) => i !== index);
  }

  addConditionRow(ruleIndex: number): void {
    this.conditionRules[ruleIndex].conditions.push({ field: '', operator: 'EQUALS', value: '' });
  }

  removeConditionRow(ruleIndex: number, condIdx: number): void {
    this.conditionRules[ruleIndex].conditions =
      this.conditionRules[ruleIndex].conditions.filter((_, i) => i !== condIdx);
  }

  // ── APPROVAL ──────────────────────────────────────────────────────────────

  addApprovalAction(): void {
    this.approvalActions = [...this.approvalActions, { id: '', label: '' }];
  }

  removeApprovalAction(index: number): void {
    this.approvalActions = this.approvalActions.filter((_, i) => i !== index);
  }

  // ── Edges ─────────────────────────────────────────────────────────────────

  openAddEdge(): void {
    this.editingEdgeIndex = -1;
    this.edgeForm = this.emptyEdge();
    this.showEdgeForm = true;
  }

  openEditEdge(index: number): void {
    this.editingEdgeIndex = index;
    this.edgeForm = { ...this.edges[index] };
    this.showEdgeForm = true;
  }

  saveEdge(): void {
    if (!this.edgeForm.id || !this.edgeForm.fromNodeId || !this.edgeForm.toNodeId) {
      this.error = 'Edge id, from node, and to node are required';
      return;
    }
    if (this.editingEdgeIndex >= 0) {
      this.edges = this.edges.map((e, i) => i === this.editingEdgeIndex ? { ...this.edgeForm } : e);
    } else {
      if (this.edges.find(e => e.id === this.edgeForm.id)) {
        this.error = 'Edge id already exists';
        return;
      }
      this.edges = [...this.edges, { ...this.edgeForm }];
    }
    this.showEdgeForm = false;
    this.error = '';
  }

  removeEdge(index: number): void {
    this.edges = this.edges.filter((_, i) => i !== index);
  }

  cancelEdgeForm(): void { this.showEdgeForm = false; this.error = ''; }

  // ── Save / Publish ─────────────────────────────────────────────────────────

  get isPublished(): boolean { return this.processStatus === 'PUBLISHED'; }
  get isArchived(): boolean  { return this.processStatus === 'ARCHIVED'; }
  get isDraft(): boolean     { return this.processStatus === 'DRAFT' || this.processStatus === null; }

  save(): void {
    if (this.isPublished) return;
    if (!this.name.trim()) { this.error = 'Process name is required'; return; }
    this.saving = true;
    this.error = '';
    this.successMsg = '';
    this.validationErrors = [];

    const payload = {
      name: this.name.trim(),
      description: this.description,
      linkedModelIds: this.linkedModelIds,
      nodes: this.nodes,
      edges: this.edges,
      settings: this.settings,
    };

    const req$ = this.hasExisting
      ? this.processService.updateProcess(this.domainSlug, this.appSlug, payload)
      : this.processService.createProcess(this.domainSlug, this.appSlug, payload);

    req$.subscribe({
      next: (res) => {
        this.saving = false;
        this.hasExisting   = true;
        this.processStatus = res.definition.status;
        this.successMsg    = 'Saved.';
        if (!res.valid) this.successMsg += ' (Fix validation issues before publishing.)';
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save';
        if (err?.error?.errors) this.validationErrors = err.error.errors;
      }
    });
  }

  publish(): void {
    if (this.isPublished) return;
    this.saving = true;
    this.error = '';
    this.successMsg = '';
    this.processService.publishProcess(this.domainSlug, this.appSlug).subscribe({
      next: (res) => {
        this.saving = false;
        this.processStatus = res.definition.status;
        this.successMsg = 'Process published. Users can now start it.';
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.error?.message || 'Publish failed';
        if (err?.error?.errors) this.validationErrors = err.error.errors;
      }
    });
  }

  archive(): void {
    this.saving = true;
    this.error = '';
    this.successMsg = '';
    this.processService.archiveProcess(this.domainSlug, this.appSlug).subscribe({
      next: () => {
        // Immediately create a new DRAFT with current content so editing continues seamlessly
        const payload = {
          name: this.name,
          description: this.description,
          linkedModelIds: this.linkedModelIds,
          nodes: this.nodes,
          edges: this.edges,
          settings: this.settings,
        };
        this.processService.createProcess(this.domainSlug, this.appSlug, payload).subscribe({
          next: (res) => {
            this.saving = false;
            this.hasExisting   = true;
            this.processStatus = res.definition.status; // DRAFT
            this.successMsg    = 'Archived. A new draft has been created — edit and publish when ready.';
          },
          error: (err: any) => {
            this.saving = false;
            this.processStatus = 'ARCHIVED';
            this.error = 'Archived but could not create new draft: ' + (err?.error?.message || err?.message || 'unknown error');
          }
        });
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.error?.message || 'Archive failed';
      }
    });
  }

  createNewDraft(): void {
    this.saving = true;
    this.error = '';
    this.successMsg = '';
    const payload = {
      name: this.name,
      description: this.description,
      linkedModelIds: this.linkedModelIds,
      nodes: this.nodes,
      edges: this.edges,
      settings: this.settings,
    };
    this.processService.createProcess(this.domainSlug, this.appSlug, payload).subscribe({
      next: (res) => {
        this.saving = false;
        this.hasExisting   = true;
        this.processStatus = res.definition.status;
        this.successMsg    = 'New draft created. You can now edit and publish.';
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to create new draft';
      }
    });
  }

  setTab(tab: 'nodes' | 'edges' | 'settings' | 'flow'): void {
    this.activeTab = tab;
    if (tab === 'flow') this.computeFlowLayout();
  }

  goBack(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
  }

  // ── Ancestor field refs ───────────────────────────────────────────────────

  getAncestorFieldRefs(nodeId: string): { ref: string; label: string }[] {
    const visited = new Set<string>();
    const queue: string[] = [nodeId];
    const refs: { ref: string; label: string }[] = [];

    while (queue.length > 0) {
      const current = queue.shift()!;
      if (visited.has(current)) continue;
      visited.add(current);

      for (const edge of this.edges.filter(e => e.toNodeId === current)) {
        const parent = this.nodes.find(n => n.id === edge.fromNodeId);
        if (!parent) continue;
        if (parent.type === 'FORM_PAGE') {
          const elements: any[] = parent.config?.['elements'] || [];
          for (const el of elements) {
            if (el.id) {
              refs.push({
                ref: `${parent.id}__${el.id}`,
                label: `${parent.name || parent.id} → ${el.label || el.id}`,
              });
            }
          }
        }
        if (!visited.has(edge.fromNodeId)) queue.push(edge.fromNodeId);
      }
    }
    return refs;
  }

  // ── Flow preview ──────────────────────────────────────────────────────────

  readonly FLOW_NODE_W = 160;
  readonly FLOW_NODE_H = 50;

  computeFlowLayout(): void {
    const W = this.FLOW_NODE_W, H = this.FLOW_NODE_H;
    const H_GAP = 240, V_GAP = 80;

    // BFS reachability from START (for disconnected detection)
    const startNode = this.nodes.find(n => n.type === 'START');
    const reachable = new Set<string>();
    if (startNode) {
      const q = [startNode.id];
      while (q.length > 0) {
        const id = q.shift()!;
        if (reachable.has(id)) continue;
        reachable.add(id);
        this.edges.filter(e => e.fromNodeId === id).forEach(e => q.push(e.toNodeId));
      }
    }

    // Kahn's topological sort to compute longest-path level per node
    const inDeg = new Map<string, number>();
    for (const n of this.nodes) inDeg.set(n.id, 0);
    for (const e of this.edges) inDeg.set(e.toNodeId, (inDeg.get(e.toNodeId) ?? 0) + 1);

    const levelMap = new Map<string, number>();
    const topo: string[] = [];
    const zeroQ = this.nodes.filter(n => (inDeg.get(n.id) ?? 0) === 0).map(n => n.id);
    const topoQueue = [...zeroQ];
    for (const id of topoQueue) levelMap.set(id, levelMap.get(id) ?? 0);

    while (topoQueue.length > 0) {
      const id = topoQueue.shift()!;
      topo.push(id);
      for (const e of this.edges.filter(e => e.fromNodeId === id)) {
        const childLevel = (levelMap.get(id) ?? 0) + 1;
        if (childLevel > (levelMap.get(e.toNodeId) ?? 0)) levelMap.set(e.toNodeId, childLevel);
        const newDeg = (inDeg.get(e.toNodeId) ?? 1) - 1;
        inDeg.set(e.toNodeId, newDeg);
        if (newDeg === 0) topoQueue.push(e.toNodeId);
      }
    }

    // Disconnected nodes get a separate column on the right
    const maxLevel = Math.max(0, ...Array.from(levelMap.values()));
    const byLevel = new Map<number, string[]>();
    for (const n of this.nodes) {
      const level = levelMap.has(n.id) ? levelMap.get(n.id)! : maxLevel + 1;
      if (!byLevel.has(level)) byLevel.set(level, []);
      byLevel.get(level)!.push(n.id);
    }

    const posMap = new Map<string, { x: number; y: number }>();
    for (const [level, ids] of byLevel.entries()) {
      ids.forEach((id, idx) => posMap.set(id, { x: level * H_GAP + 20, y: idx * V_GAP + 20 }));
    }

    this.flowNodes = this.nodes.map(n => {
      const pos = posMap.get(n.id) ?? { x: 20, y: 20 };
      return { id: n.id, type: n.type, name: n.name, x: pos.x, y: pos.y, width: W, height: H, isDisconnected: !reachable.has(n.id) };
    });

    this.flowEdges = this.edges.flatMap(e => {
      const from = this.flowNodes.find(n => n.id === e.fromNodeId);
      const to   = this.flowNodes.find(n => n.id === e.toNodeId);
      if (!from || !to || from === to) return [];
      return [{ id: e.id, label: e.label, path: this.edgePath(from, to), midX: (from.x + from.width + to.x) / 2, midY: (from.y + from.height / 2 + to.y + to.height / 2) / 2 }];
    });

    this.disconnectedNodeIds = this.nodes.filter(n => !reachable.has(n.id)).map(n => n.id);
  }

  private edgePath(from: FlowLayoutNode, to: FlowLayoutNode): string {
    const W = this.FLOW_NODE_W, H = this.FLOW_NODE_H;
    if (to.x >= from.x + W) {
      // Forward edge: exit right → enter left
      const x1 = from.x + W, y1 = from.y + H / 2;
      const x2 = to.x, y2 = to.y + H / 2;
      const cx = (x1 + x2) / 2;
      return `M ${x1} ${y1} C ${cx} ${y1} ${cx} ${y2} ${x2} ${y2}`;
    } else {
      // Backward or same-column: route below
      const x1 = from.x + W / 2, y1 = from.y + H;
      const x2 = to.x + W / 2, y2 = to.y;
      const cy = Math.max(y1, y2) + 50;
      return `M ${x1} ${y1} C ${x1} ${cy} ${x2} ${cy} ${x2} ${y2}`;
    }
  }

  nodeTypeFill(type: string): string {
    const colors: Record<string, string> = {
      START: '#c6f6d5', END: '#fed7d7',
      FORM_PAGE: '#bee3f8', DATA_ACTION: '#feebc8',
      DATA_VIEW: '#e9d8fd', CONDITION: '#fefcbf',
      APPROVAL: '#fbd38d',
    };
    return colors[type] ?? '#e2e8f0';
  }

  get svgViewBox(): string {
    if (this.flowNodes.length === 0) return '0 0 800 300';
    const maxX = Math.max(...this.flowNodes.map(n => n.x + n.width)) + 40;
    const maxY = Math.max(...this.flowNodes.map(n => n.y + n.height)) + 40;
    return `0 0 ${maxX} ${Math.max(maxY, 200)}`;
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private emptyNode(): ProcessNode {
    return { id: '', type: 'FORM_PAGE', name: '', positionX: 0, positionY: 0, config: {}, permissions: { allowedRoles: [], allowedUserIds: [] } };
  }

  private emptyEdge(): ProcessEdge {
    return { id: '', fromNodeId: '', toNodeId: '', label: '', conditionRef: '' };
  }

  private emptyElement(): FormElement {
    return { id: '', type: 'TEXT_INPUT', label: '', validation: {}, config: {} };
  }
}
