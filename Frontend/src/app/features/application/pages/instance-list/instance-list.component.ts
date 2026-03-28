import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProcessService } from '../../../../core/services/process.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ProcessInstance, TaskResponse } from '../../../../core/models/process.model';

const THEME_MAP: Record<string, string> = {
  midnight: '#1a1a2e', ocean: '#0c4a6e', forest: '#14532d', ember: '#7f1d1d',
  violet: '#3b0764', steel: '#1e293b', rose: '#881337', amber: '#78350f',
  teal: '#134e4a', indigo: '#312e81', slate: '#0f172a', plum: '#4a044e',
  pine: '#052e16', crimson: '#450a0a', navy: '#1e3a5f', graphite: '#374151',
};

@Component({
  selector: 'app-instance-list',
  templateUrl: './instance-list.component.html',
  styleUrls: ['./instance-list.component.css']
})
export class InstanceListComponent implements OnInit {

  domainSlug = '';
  appSlug = '';
  mode: 'tasks' | 'instances' = 'tasks';
  activeTab: 'tasks' | 'started' = 'tasks';

  instances: ProcessInstance[] = [];
  startedInstances: ProcessInstance[] = [];
  tasks: TaskResponse[] = [];
  loading = false;
  error = '';

  themeColor = '#1a1a2e';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private processService: ProcessService,
    private auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug    = this.route.snapshot.params['appSlug'];
    this.mode = this.route.snapshot.data['mode'] || 'tasks';

    const ctx = this.auth.getContext();
    const isOwner = !!ctx && ctx.principalType === 'OWNER';

    if (this.mode === 'instances' && !isOwner) {
      this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'tasks']);
      return;
    }

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

    if (this.mode === 'tasks') {
      this.loadTasksAndStarted();
      return;
    }

    this.processService.listInstances(this.domainSlug, this.appSlug).subscribe({
      next: (list) => {
        this.instances = list || [];
        this.loading = false;
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load instances';
        this.loading = false;
      }
    });
  }

  private loadTasksAndStarted(): void {
    this.loading = true;
    this.error = '';

    let tasksDone = false;
    let startedDone = false;

    const finalize = () => {
      if (tasksDone && startedDone) {
        this.loading = false;
      }
    };

    this.processService.listMyTasks(this.domainSlug, this.appSlug).subscribe({
      next: (res) => {
        this.tasks = res?.tasks || [];
        tasksDone = true;
        finalize();
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load tasks';
        tasksDone = true;
        finalize();
      }
    });

    this.processService.listMyStartedInstances(this.domainSlug, this.appSlug).subscribe({
      next: (list) => {
        this.startedInstances = list || [];
        startedDone = true;
        finalize();
      },
      error: (err: any) => {
        if (!this.error) {
          this.error = err?.error?.message || 'Failed to load your workflows';
        }
        startedDone = true;
        finalize();
      }
    });
  }

  viewInstance(id: string): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'instances', id]);
  }

  goBack(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
  }

  goToTasks(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'tasks']);
  }

  goToInstances(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'instances']);
  }

  setTab(tab: 'tasks' | 'started'): void {
    this.activeTab = tab;
  }

  countByStatus(status: string): number {
    return this.instances.filter(i => i.status === status).length;
  }

  hasTasks(): boolean {
    return this.tasks.length > 0;
  }

  hasStarted(): boolean {
    return this.startedInstances.length > 0;
  }

  formatDate(iso?: string): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
      + ' · ' + d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }
}
