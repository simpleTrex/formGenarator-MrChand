import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProcessService } from '../../../../core/services/process.service';
import {
  DomainModelField,
  ProcessInstance,
  StepEdgeView,
  StepViewResponse,
} from '../../../../core/models/process.model';

const THEME_MAP: Record<string, string> = {
  midnight: '#1a1a2e', ocean: '#0c4a6e', forest: '#14532d', ember: '#7f1d1d',
  violet: '#3b0764', steel: '#1e293b', rose: '#881337', amber: '#78350f',
  teal: '#134e4a', indigo: '#312e81', slate: '#0f172a', plum: '#4a044e',
  pine: '#052e16', crimson: '#450a0a', navy: '#1e3a5f', graphite: '#374151',
};

@Component({
  selector: 'app-instance-view',
  templateUrl: './instance-view.component.html',
  styleUrls: ['./instance-view.component.css']
})
export class InstanceViewComponent implements OnInit {

  domainSlug = '';
  appSlug = '';
  instanceId = '';

  instance: ProcessInstance | null = null;
  stepView: StepViewResponse | null = null;

  loading = false;
  submitting = false;
  error = '';
  successMsg = '';

  formData: Record<string, any> = {};
  selectedEdgeId = '';
  comment = '';

  objectKeys = Object.keys;
  themeColor = '#1a1a2e';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private processService: ProcessService,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug = this.route.snapshot.params['appSlug'];
    this.instanceId = this.route.snapshot.params['instanceId'];
    this.loadTheme();
    this.load();
  }

  private loadTheme() {
    const appKey = `at-${this.domainSlug}-${this.appSlug}`;
    const domKey = `dt-${this.domainSlug}`;
    const id = localStorage.getItem(appKey) || localStorage.getItem(domKey) || 'midnight';
    this.themeColor = THEME_MAP[id] ?? '#1a1a2e';
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.successMsg = '';

    this.processService.getInstance(this.domainSlug, this.appSlug, this.instanceId).subscribe({
      next: (instance) => {
        this.instance = instance;
        if (instance.status === 'ACTIVE') {
          this.loadStepView();
        } else {
          this.stepView = null;
          this.loading = false;
        }
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load workflow instance';
        this.loading = false;
      }
    });
  }

  loadStepView(): void {
    this.processService.getStepView(this.domainSlug, this.appSlug, this.instanceId).subscribe({
      next: (view) => {
        this.stepView = view;
        this.formData = this.toEditableMap(view.currentData || {});
        this.mergeReferencedData(view);
        this.mergeMappedData(view);

        const firstAvailable = (view.availableEdges || []).find(e => !e.disabled);
        this.selectedEdgeId = firstAvailable?.id || '';

        this.loading = false;
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load current workflow step';
        this.loading = false;
      }
    });
  }

  executeEdge(): void {
    if (!this.stepView || !this.selectedEdgeId) {
      return;
    }

    this.submitting = true;
    this.error = '';
    this.successMsg = '';

    const payload = {
      edgeId: this.selectedEdgeId,
      formData: this.normalizePayloadByFieldType(this.formData),
      comment: this.comment || undefined,
    };

    this.processService.executeEdge(this.domainSlug, this.appSlug, this.instanceId, payload).subscribe({
      next: () => {
        this.submitting = false;
        this.comment = '';
        this.successMsg = 'Workflow action executed successfully.';
        this.load();
      },
      error: (err: any) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Failed to execute workflow edge';
      }
    });
  }

  goBackToApp(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'tasks']);
  }

  get availableEdges(): StepEdgeView[] {
    return this.stepView?.availableEdges || [];
  }

  get modelFields(): DomainModelField[] {
    return this.stepView?.modelFields || [];
  }

  get historyRows() {
    return this.stepView?.history || this.instance?.history || [];
  }

  isReadOnlyField(key: string): boolean {
    return !!this.stepView?.readOnlyFields?.includes(key);
  }

  getEdgeDisabledReason(edgeId: string): string {
    const edge = this.availableEdges.find(e => e.id === edgeId);
    return edge?.disabledReason || '';
  }

  fieldInputType(field: DomainModelField): string {
    if (field.type === 'NUMBER') {
      return 'number';
    }
    if (field.type === 'DATE') {
      return 'date';
    }
    if (field.type === 'DATETIME') {
      return 'datetime-local';
    }
    return 'text';
  }

  fieldIsCheckbox(field: DomainModelField): boolean {
    return field.type === 'BOOLEAN';
  }

  fieldIsJson(field: DomainModelField): boolean {
    return field.type === 'OBJECT' || field.type === 'ARRAY';
  }

  valueAsJsonText(key: string): string {
    const value = this.formData[key];
    if (value == null) {
      return '';
    }
    if (typeof value === 'string') {
      return value;
    }
    try {
      return JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }

  updateJsonField(key: string, value: string): void {
    this.formData[key] = value;
  }

  prettyValue(value: any): string {
    if (value == null || value === '') {
      return '—';
    }
    if (typeof value === 'object') {
      try {
        return JSON.stringify(value);
      } catch {
        return String(value);
      }
    }
    return String(value);
  }

  private toEditableMap(data: Record<string, any>): Record<string, any> {
    const map: Record<string, any> = {};
    for (const field of this.modelFields) {
      const value = data[field.key];
      map[field.key] = value == null ? this.defaultValue(field) : value;
    }
    return map;
  }

  private mergeReferencedData(view: StepViewResponse): void {
    const readOnlyFields = view.readOnlyFields || [];
    const referenced = view.referencedData || {};

    for (const key of readOnlyFields) {
      if (this.formData[key] === '' || this.formData[key] == null) {
        if (referenced[key] !== undefined) {
          this.formData[key] = referenced[key];
        }
      }
    }
  }

  private mergeMappedData(view: StepViewResponse): void {
    const mapped = view.mappedData || {};
    for (const key of Object.keys(mapped)) {
      const existing = this.formData[key];
      if (existing == null || (typeof existing === 'string' && existing.trim() === '')) {
        this.formData[key] = mapped[key];
      }
    }
  }

  private defaultValue(field: DomainModelField): any {
    if (field.type === 'BOOLEAN') {
      return false;
    }
    if (field.type === 'ARRAY') {
      return '[]';
    }
    if (field.type === 'OBJECT') {
      return '{}';
    }
    return '';
  }

  private normalizePayloadByFieldType(source: Record<string, any>): Record<string, any> {
    const payload: Record<string, any> = {};

    for (const field of this.modelFields) {
      const raw = source[field.key];

      if (raw === undefined) {
        continue;
      }
      if (raw === '' && !field.required) {
        payload[field.key] = raw;
        continue;
      }

      switch (field.type) {
        case 'NUMBER': {
          const parsed = Number(raw);
          payload[field.key] = Number.isNaN(parsed) ? raw : parsed;
          break;
        }
        case 'BOOLEAN':
          payload[field.key] = !!raw;
          break;
        case 'ARRAY':
        case 'OBJECT':
          payload[field.key] = this.tryParseJson(raw);
          break;
        default:
          payload[field.key] = raw;
      }
    }

    return payload;
  }

  private tryParseJson(value: any): any {
    if (typeof value !== 'string') {
      return value;
    }
    const trimmed = value.trim();
    if (!trimmed) {
      return value;
    }
    try {
      return JSON.parse(trimmed);
    } catch {
      return value;
    }
  }
}
