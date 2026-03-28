import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../../../../core/services/domain.service';
import { ProcessService } from '../../../../core/services/process.service';
import {
  AutoFetchRule,
  DomainModelField,
  DomainFieldType,
  ProcessDefinitionResponse,
  StepDataConfig,
  WorkflowEdge,
  WorkflowStatus,
  WorkflowStep,
} from '../../../../core/models/process.model';

@Component({
  selector: 'app-process-builder',
  templateUrl: './process-builder.component.html',
  styleUrls: ['./process-builder.component.css']
})
export class ProcessBuilderComponent implements OnInit {

  domainSlug = '';
  appSlug = '';
  workflowSlug = '';

  workflowStatus: WorkflowStatus | null = null;
  name = '';
  description = '';
  steps: WorkflowStep[] = [];
  globalEdges: WorkflowEdge[] = [];
  selectedStepId: string | null = null;
  editorOpen = false;

  availableRoles: string[] = [];
  fieldTypes: DomainFieldType[] = [
    'STRING',
    'NUMBER',
    'BOOLEAN',
    'DATE',
    'DATETIME',
    'REFERENCE',
    'EMPLOYEE_REFERENCE',
    'OBJECT',
    'ARRAY',
  ];

  loading = false;
  saving = false;
  error = '';
  successMsg = '';
  publishErrors: string[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private processService: ProcessService,
    private domainService: DomainService,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug = this.route.snapshot.params['appSlug'];
    this.workflowSlug = this.route.snapshot.queryParamMap.get('wf') || '';

    this.loadRoles();

    if (this.workflowSlug) {
      this.loadWorkflow(this.workflowSlug);
      return;
    }

    this.resolveWorkflowFromApp();
  }

  private resolveWorkflowFromApp(): void {
    this.loading = true;
    this.processService.listWorkflows(this.domainSlug, this.appSlug).subscribe({
      next: (workflows) => {
        const existing = Array.isArray(workflows) && workflows.length > 0 ? workflows[0] : null;
        if (existing?.slug) {
          this.workflowSlug = existing.slug;
          this.loadWorkflow(existing.slug);
          return;
        }
        this.loading = false;
        this.initDraft();
      },
      error: () => {
        this.loading = false;
        this.initDraft();
      }
    });
  }

  get isDraft(): boolean {
    return this.workflowStatus !== 'PUBLISHED';
  }

  goBack(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
  }

  openExplainer(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'workflows', 'explainer']);
  }

  loadRoles(): void {
    this.domainService.getAppGroups(this.domainSlug, this.appSlug).subscribe({
      next: (groups) => {
        const names = (groups || [])
          .map((g: any) => g?.name)
          .filter((name: string) => !!name);
        this.availableRoles = Array.from(new Set(names));
      },
      error: () => {
        this.availableRoles = [];
      }
    });
  }

  loadWorkflow(slug: string): void {
    this.loading = true;
    this.error = '';
    this.publishErrors = [];
    this.selectedStepId = null;
    this.editorOpen = false;

    this.processService.getWorkflow(this.domainSlug, this.appSlug, slug).subscribe({
      next: (res: ProcessDefinitionResponse) => {
        const wf = res.workflow;

        this.workflowSlug = wf.slug;
        this.workflowStatus = wf.status;
        this.name = wf.name || '';
        this.description = wf.description || '';
        this.steps = (wf.steps || []).map(s => this.withStepDefaults(s));
        this.globalEdges = (wf.globalEdges || []).map(e => this.withEdgeDefaults(e));

        this.normalizeStepOrder();
        this.loading = false;
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load workflow';
        this.loading = false;
      }
    });
  }

  initDraft(): void {
    this.workflowStatus = null;
    this.name = '';
    this.description = '';
    this.steps = [];
    this.globalEdges = [];
    this.selectedStepId = null;
    this.editorOpen = false;
    this.addStepAndEdit();
  }

  addStep(): WorkflowStep {
    const index = this.steps.length;

    const step: WorkflowStep = {
      id: `step_${index + 1}`,
      name: `Step ${index + 1}`,
      order: index,
      start: this.steps.length === 0,
      end: false,
      fields: [],
      edges: [],
      dataConfig: this.defaultDataConfig(),
      positionX: 140 + index * 120,
      positionY: 180,
    };

    this.steps = [...this.steps, step];
    this.normalizeStepOrder();
    return step;
  }

  addStepAndEdit(): void {
    const step = this.addStep();
    this.openStepEditor(step);
  }

  removeStep(index: number): void {
    const removed = this.steps[index];
    this.steps.splice(index, 1);

    if (removed && removed.id === this.selectedStepId) {
      this.closeEditor();
    }

    if (!this.steps.some(s => s.start) && this.steps.length > 0) {
      this.steps[0].start = true;
    }

    this.normalizeStepOrder();
  }

  setStartStep(index: number): void {
    this.steps.forEach((step, i) => {
      step.start = i === index;
    });
  }

  setStartStepById(stepId: string): void {
    const index = this.steps.findIndex(step => step.id === stepId);
    if (index >= 0) {
      this.setStartStep(index);
    }
  }

  openStepEditor(step: WorkflowStep): void {
    if (!step) {
      return;
    }
    this.ensureDataConfig(step);
    if (!step.fields) {
      step.fields = [];
    }
    this.selectedStepId = step.id;
    this.editorOpen = true;
  }

  closeEditor(): void {
    this.editorOpen = false;
  }

  get selectedStep(): WorkflowStep | null {
    return this.steps.find(s => s.id === this.selectedStepId) || null;
  }

  orderedSteps(): WorkflowStep[] {
    return [...this.steps].sort((a, b) => a.order - b.order);
  }

  graphLevels(): WorkflowStep[][] {
    if (!this.steps.length) {
      return [];
    }

    const byId = new Map<string, WorkflowStep>();
    this.steps.forEach(step => byId.set(step.id, step));

    const start = this.steps.find(s => s.start) || this.steps[0];
    const levelById = new Map<string, number>();
    const queue: Array<{ id: string; level: number }> = [];

    if (start?.id) {
      levelById.set(start.id, 0);
      queue.push({ id: start.id, level: 0 });
    }

    while (queue.length) {
      const current = queue.shift();
      if (!current) {
        continue;
      }
      const step = byId.get(current.id);
      if (!step || !step.edges) {
        continue;
      }
      for (const edge of step.edges) {
        if (!edge.targetStepId || edge.terminal) {
          continue;
        }
        if (!levelById.has(edge.targetStepId)) {
          levelById.set(edge.targetStepId, current.level + 1);
          queue.push({ id: edge.targetStepId, level: current.level + 1 });
        }
      }
    }

    const maxLevel = Math.max(0, ...Array.from(levelById.values()));
    const levels: WorkflowStep[][] = Array.from({ length: maxLevel + 1 }, () => []);

    for (const step of this.steps) {
      const level = levelById.get(step.id);
      if (level !== undefined) {
        levels[level].push(step);
      }
    }

    return levels.map(row => row.sort((a, b) => a.order - b.order));
  }

  unlinkedSteps(): WorkflowStep[] {
    const inbound = new Set<string>();
    this.steps.forEach(step => {
      (step.edges || []).forEach(edge => {
        if (edge.targetStepId) {
          inbound.add(edge.targetStepId);
        }
      });
    });

    return this.steps.filter(step => {
      const hasOutbound = !!step.edges && step.edges.length > 0;
      const hasInbound = inbound.has(step.id);
      return !hasOutbound && !hasInbound;
    });
  }

  edgeTargetLabel(edge: WorkflowEdge): string {
    if (edge.terminal) {
      return 'Terminal';
    }
    if (!edge.targetStepId) {
      return 'Unset';
    }
    const target = this.steps.find(s => s.id === edge.targetStepId);
    if (target?.name) {
      return `${target.name} (${target.id})`;
    }
    return edge.targetStepId;
  }

  edgeRoleLabel(edge: WorkflowEdge): string {
    const roles = (edge.allowedRoles || []).filter(Boolean);
    const roleText = roles.length ? `Roles: ${roles.join(', ')}` : '';
    const submitterText = edge.onlySubmitter ? 'Submitter only' : '';
    if (roleText && submitterText) {
      return `${roleText} · ${submitterText}`;
    }
    if (roleText) {
      return roleText;
    }
    if (submitterText) {
      return submitterText;
    }
    return 'No role set';
  }

  removeSelectedStep(): void {
    const step = this.selectedStep;
    if (!step) {
      return;
    }
    const index = this.steps.findIndex(s => s.id === step.id);
    if (index >= 0) {
      this.removeStep(index);
    }
  }

  addEdgeToStep(step: WorkflowStep): void {
    step.edges = [...step.edges, this.newEdge(step.id)];
  }

  removeEdgeFromStep(step: WorkflowStep, edgeIndex: number): void {
    step.edges.splice(edgeIndex, 1);
  }

  addGlobalEdge(): void {
    this.globalEdges = [...this.globalEdges, this.newEdge()];
  }

  removeGlobalEdge(index: number): void {
    this.globalEdges.splice(index, 1);
  }

  addAutoFetchRule(step: WorkflowStep): void {
    const dataConfig = this.ensureDataConfig(step);
    dataConfig.autoFetchRules = [...dataConfig.autoFetchRules, this.newFetchRule()];
  }

  removeAutoFetchRule(step: WorkflowStep, index: number): void {
    const dataConfig = this.ensureDataConfig(step);
    dataConfig.autoFetchRules.splice(index, 1);
  }

  saveDraft(): void {
    if (this.workflowStatus === 'PUBLISHED') {
      this.error = 'Archive the workflow before editing.';
      return;
    }

    if (!this.name.trim()) {
      this.error = 'Workflow name is required.';
      return;
    }

    if (this.steps.length === 0) {
      this.error = 'At least one step is required.';
      return;
    }

    this.error = '';
    this.successMsg = '';
    this.publishErrors = [];
    this.saving = true;

    const payload = {
      name: this.name.trim(),
      description: this.description,
      steps: this.sanitizeSteps(this.steps),
      globalEdges: this.sanitizeEdges(this.globalEdges),
    };

    const request$ = this.workflowSlug
      ? this.processService.updateWorkflow(this.domainSlug, this.appSlug, this.workflowSlug, payload)
      : this.processService.createWorkflow(this.domainSlug, this.appSlug, payload);

    request$.subscribe({
      next: (res: ProcessDefinitionResponse) => {
        this.saving = false;
        this.successMsg = 'Draft saved successfully.';

        this.workflowSlug = res.workflow.slug;
        this.workflowStatus = res.workflow.status;
        this.steps = (res.workflow.steps || []).map(s => this.withStepDefaults(s));
        this.globalEdges = (res.workflow.globalEdges || []).map(e => this.withEdgeDefaults(e));

        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { wf: this.workflowSlug },
          queryParamsHandling: 'merge',
        });
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save draft';
      }
    });
  }

  publish(): void {
    if (!this.workflowSlug) {
      this.error = 'Save the draft first, then publish.';
      return;
    }

    this.saving = true;
    this.error = '';
    this.successMsg = '';
    this.publishErrors = [];

    this.processService.publishWorkflow(this.domainSlug, this.appSlug, this.workflowSlug).subscribe({
      next: (res: ProcessDefinitionResponse) => {
        this.saving = false;
        this.workflowStatus = res.workflow.status;
        this.successMsg = 'Workflow published successfully.';
        this.publishErrors = [];
      },
      error: (err: any) => {
        this.saving = false;
        this.publishErrors = this.extractValidationErrors(err);
        if (this.publishErrors.length > 0) {
          this.error = 'Publish failed. Hover the warning icon for details.';
        } else {
          this.error = err?.error?.message || 'Failed to publish workflow';
        }
      }
    });
  }

  archive(): void {
    if (!this.workflowSlug) {
      return;
    }

    if (!confirm('Archive this workflow?')) {
      return;
    }

    this.saving = true;
    this.error = '';
    this.successMsg = '';

    this.processService.archiveWorkflow(this.domainSlug, this.appSlug, this.workflowSlug).subscribe({
      next: () => {
        this.saving = false;
        this.workflowStatus = 'ARCHIVED';
        this.successMsg = 'Workflow archived.';
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to archive workflow';
      }
    });
  }

  deleteDraft(): void {
    if (!this.workflowSlug) {
      return;
    }

    if (!confirm('Delete this DRAFT workflow? This cannot be undone.')) {
      return;
    }

    this.saving = true;
    this.error = '';

    this.processService.deleteWorkflow(this.domainSlug, this.appSlug, this.workflowSlug).subscribe({
      next: () => {
        this.saving = false;
        this.successMsg = 'Draft workflow deleted.';
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
      },
      error: (err: any) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to delete workflow';
      }
    });
  }

  private extractValidationErrors(err: any): string[] {
    const details = err?.error?.details;
    if (Array.isArray(details)) {
      return details.map((item: any) => String(item)).filter(Boolean);
    }
    if (details && typeof details === 'object') {
      return Object.values(details)
        .map((item: any) => String(item))
        .filter(Boolean);
    }
    const message = err?.error?.message;
    return message ? [String(message)] : [];
  }

  csv(values: string[] | undefined): string {
    return (values || []).join(', ');
  }

  parseCsv(input: string): string[] {
    return (input || '')
      .split(',')
      .map(v => v.trim())
      .filter(v => !!v);
  }

  stepTargetOptions(currentStepId: string): WorkflowStep[] {
    return this.steps.filter(s => s.id !== currentStepId);
  }

  getStepModelFields(step: WorkflowStep): DomainModelField[] {
    return step.fields || [];
  }

  getPreviousStep(step: WorkflowStep): WorkflowStep | null {
    const ordered = [...this.steps].sort((a, b) => a.order - b.order);
    const idx = ordered.findIndex(s => s.id === step.id);
    if (idx > 0) {
      return ordered[idx - 1];
    }
    return null;
  }

  getPreviousStepFields(step: WorkflowStep): DomainModelField[] {
    const previous = this.getPreviousStep(step);
    if (previous) {
      return previous.fields || [];
    }
    return step.fields || [];
  }

  getAutoFetchTargetFields(rule: AutoFetchRule): DomainModelField[] {
    return [];
  }

  getSourceStepOptionsForStep(step: WorkflowStep): WorkflowStep[] {
    return this.steps.filter(s => s.order < step.order);
  }

  getStepById(stepId: string): WorkflowStep | null {
    return this.steps.find(s => s.id === stepId) || null;
  }

  getAutoFetchSourceFields(rule: AutoFetchRule): DomainModelField[] {
    if (!rule.sourceStepId) {
      return [];
    }
    const sourceStep = this.getStepById(rule.sourceStepId);
    if (!sourceStep) {
      return [];
    }
    return sourceStep.fields || [];
  }

  getGlobalEdgeFields(): DomainModelField[] {
    const start = this.steps.find(s => s.start) || this.steps[0];
    if (!start) {
      return [];
    }
    return start.fields || [];
  }

  addField(step: WorkflowStep): void {
    if (!step.fields) {
      step.fields = [];
    }
    step.fields.push({
      key: '',
      type: 'STRING',
      required: false,
      unique: false,
      config: {},
    });
  }

  removeField(step: WorkflowStep, index: number): void {
    if (!step.fields) {
      return;
    }
    step.fields.splice(index, 1);
  }

  addListItem(list: string[]): void {
    list.push('');
  }

  removeListItem(list: string[], index: number): void {
    list.splice(index, 1);
  }

  setListItem(list: string[], index: number, value: string): void {
    list[index] = value;
  }

  getFieldOptionLabel(field: DomainModelField): string {
    return `${field.key} (${field.type})`;
  }

  private withStepDefaults(step: WorkflowStep): WorkflowStep {
    return {
      ...step,
      fields: step.fields || [],
      edges: (step.edges || []).map(edge => this.withEdgeDefaults(edge)),
      dataConfig: step.dataConfig ? this.withDataConfigDefaults(step.dataConfig) : this.defaultDataConfig(),
      start: !!step.start,
      end: !!step.end,
    };
  }

  private withEdgeDefaults(edge: WorkflowEdge): WorkflowEdge {
    return {
      id: edge.id || '',
      name: edge.name || '',
      targetStepId: edge.targetStepId || null,
      terminal: !!edge.terminal,
      allowedRoles: edge.allowedRoles || [],
      allowedUserIds: [],
      onlySubmitter: !!edge.onlySubmitter,
      requiredFields: edge.requiredFields || [],
      conditions: edge.conditions || [],
      autoActions: edge.autoActions || [],
    };
  }

  private withDataConfigDefaults(config: StepDataConfig): StepDataConfig {
    return {
      referencePreviousStep: !!config.referencePreviousStep,
      reuseFromStepId: config.reuseFromStepId || '',
      previousStepFields: config.previousStepFields || [],
      autoFetchRules: config.autoFetchRules || [],
      readOnlyFields: config.readOnlyFields || [],
    };
  }

  private defaultDataConfig(): StepDataConfig {
    return {
      referencePreviousStep: false,
      reuseFromStepId: '',
      previousStepFields: [],
      autoFetchRules: [],
      readOnlyFields: [],
    };
  }

  private ensureDataConfig(step: WorkflowStep): StepDataConfig {
    if (!step.dataConfig) {
      step.dataConfig = this.defaultDataConfig();
    }
    return step.dataConfig;
  }

  private newEdge(currentStepId?: string): WorkflowEdge {
    const prefix = currentStepId ? `${currentStepId}_` : 'global_';
    return {
      id: `${prefix}edge_${Date.now()}`,
      name: 'Action',
      targetStepId: null,
      terminal: false,
      allowedRoles: [],
      allowedUserIds: [],
      onlySubmitter: false,
      requiredFields: [],
      conditions: [],
      autoActions: [],
    };
  }

  private newFetchRule(): AutoFetchRule {
    return {
      sourceStepId: '',
      sourceField: '',
      targetField: '',
    };
  }

  private normalizeStepOrder(): void {
    this.steps.forEach((step, index) => {
      step.order = index;
    });
  }

  private sanitizeSteps(steps: WorkflowStep[]): WorkflowStep[] {
    return steps.map((step, index) => ({
      ...step,
      id: step.id.trim(),
      name: step.name.trim(),
      order: index,
      fields: step.fields || [],
      edges: this.sanitizeEdges(step.edges),
      dataConfig: this.withDataConfigDefaults(step.dataConfig || this.defaultDataConfig()),
    }));
  }

  private sanitizeEdges(edges: WorkflowEdge[]): WorkflowEdge[] {
    return edges.map(edge => ({
      ...edge,
      id: edge.id.trim(),
      name: edge.name.trim(),
      targetStepId: edge.terminal ? null : (edge.targetStepId || null),
      allowedRoles: (edge.allowedRoles || []).map(v => v.trim()).filter(Boolean),
      allowedUserIds: [],
      requiredFields: (edge.requiredFields || []).map(v => v.trim()).filter(Boolean),
      conditions: edge.conditions || [],
      autoActions: edge.autoActions || [],
    }));
  }
}
