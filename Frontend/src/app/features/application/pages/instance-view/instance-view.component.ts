import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProcessService } from '../../../../core/services/process.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  ProcessInstance, NodeViewResponse, ProcessInstanceResponse,
} from '../../../../core/models/process.model';

interface FormField {
  id: string;
  type: string;
  label: string;
  required?: boolean;
  config?: Record<string, any>;
  validation?: Record<string, any>;
}

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
  nodeView: NodeViewResponse | null = null;

  loading = false;
  submitting = false;
  error = '';
  successMsg = '';

  formData: Record<string, any> = {};
  selectedAction = '';
  comment = '';

  objectKeys = Object.keys;
  themeColor = '#1a1a2e';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private processService: ProcessService,
    public auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.domainSlug  = this.route.snapshot.params['slug'];
    this.appSlug     = this.route.snapshot.params['appSlug'];
    this.instanceId  = this.route.snapshot.params['instanceId'];
    this.loadTheme();
    this.load();
  }

  private loadTheme() {
    const appKey = `at-${this.domainSlug}-${this.appSlug}`;
    const domKey = `dt-${this.domainSlug}`;
    const id = localStorage.getItem(appKey) || localStorage.getItem(domKey) || 'midnight';
    this.themeColor = THEME_MAP[id] ?? '#1a1a2e';
  }

  /** True if user is owner or has APP_WRITE — used to show admin-only UI bits */
  get isAdmin(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && ctx.principalType === 'OWNER');
  }

  load(): void {
    this.loading = true;
    this.error = '';

    this.processService.getInstance(this.domainSlug, this.appSlug, this.instanceId).subscribe({
      next: (inst) => {
        this.instance = inst;
        if (inst.status === 'ACTIVE') {
          this.loadNodeView();
        } else {
          this.loading = false;
        }
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load instance';
        this.loading = false;
      }
    });
  }

  loadNodeView(): void {
    this.processService.getNodeView(this.domainSlug, this.appSlug, this.instanceId).subscribe({
      next: (view) => {
        this.nodeView = view;
        this.formData = { ...(view.prefilledData || {}) };
        this.selectedAction = view.availableActions?.[0] || '';
        this.loading = false;
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load current step';
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (!this.nodeView) return;
    this.submitting = true;
    this.error = '';
    this.successMsg = '';

    const payload = {
      nodeId: this.nodeView.nodeId,
      formData: this.formData,
      action: this.nodeView.nodeType === 'APPROVAL' ? this.selectedAction : undefined,
      comment: this.comment || undefined,
    };

    this.processService.submitNode(this.domainSlug, this.appSlug, this.instanceId, payload).subscribe({
      next: (res: ProcessInstanceResponse) => {
        this.submitting = false;
        this.successMsg = '';
        this.instance = res.instance;
        this.comment = '';
        this.formData = {};
        if (res.instance.status === 'ACTIVE') {
          this.loadNodeView();
        } else {
          this.nodeView = null;
        }
      },
      error: (err: any) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Submission failed';
      }
    });
  }

  cancel(): void {
    if (!confirm('Cancel this process instance? This cannot be undone.')) return;
    this.processService.cancelInstance(this.domainSlug, this.appSlug, this.instanceId).subscribe({
      next: () => this.load(),
      error: (err: any) => this.error = err?.error?.message || 'Failed to cancel',
    });
  }

  goBackToApp(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
  }

  get formElements(): FormField[] {
    if (!this.nodeView?.config?.['elements']) return [];
    return this.nodeView.config['elements'] as FormField[];
  }

  get approvalActions(): string[] {
    return this.nodeView?.availableActions || ['approve', 'reject'];
  }

  isVisible(el: FormField): boolean {
    const rule = el.config?.['visibilityRule'] || (el as any).visibilityRule;
    if (!rule) return true;
    const depVal = this.formData[rule.dependsOn];
    const actual = depVal == null ? '' : String(depVal);
    const expected = rule.value == null ? '' : String(rule.value);
    switch (rule.operator) {
      case 'EQUALS': return actual === expected;
      case 'NOT_EQUALS': return actual !== expected;
      case 'IS_EMPTY': return actual === '';
      case 'IS_NOT_EMPTY': return actual !== '';
      default: return true;
    }
  }

  getOptions(el: FormField): { label: string; value: string }[] {
    return el.config?.['options'] || [];
  }

  getRecordColumns(records: Record<string, any>[]): string[] {
    if (!records || records.length === 0) return [];
    return Object.keys(records[0]).filter(k => k !== '_id');
  }

  formatColumnName(key: string): string {
    return key
      .replace(/_/g, ' ')
      .replace(/([A-Z])/g, ' $1')
      .replace(/\b\w/g, c => c.toUpperCase())
      .trim();
  }
}
