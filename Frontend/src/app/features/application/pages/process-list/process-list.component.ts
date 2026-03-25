import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProcessService } from '../../../../core/services/process.service';
import { ProcessDefinitionResponse, ProcessDefinition, STATUS_BADGE_CLASS } from '../../../../core/models/process.model';

@Component({
  selector: 'app-process-list',
  templateUrl: './process-list.component.html',
  styleUrls: ['./process-list.component.css']
})
export class ProcessListComponent implements OnInit {

  domainSlug = '';
  appSlug = '';

  processes: ProcessDefinitionResponse[] = [];
  loading = false;
  error = '';
  actionLoading: Record<string, boolean> = {};
  actionError: Record<string, string> = {};

  themeColor = '#1e293b';

  statusBadge = STATUS_BADGE_CLASS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private processService: ProcessService,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug = this.route.snapshot.params['appSlug'];
    this.loadTheme();
    this.loadProcesses();
  }

  private loadTheme() {
    const THEME_MAP: Record<string, string> = {
      midnight: '#1a1a2e', ocean: '#0c4a6e', forest: '#14532d', ember: '#7f1d1d',
      violet: '#3b0764', steel: '#1e293b', rose: '#881337', amber: '#78350f',
      teal: '#134e4a', indigo: '#312e81', slate: '#0f172a', plum: '#4a044e',
      pine: '#052e16', crimson: '#450a0a', navy: '#1e3a5f', graphite: '#374151',
    };
    const appKey = `at-${this.domainSlug}-${this.appSlug}`;
    const domKey = `dt-${this.domainSlug}`;
    const id = localStorage.getItem(appKey) || localStorage.getItem(domKey) || 'midnight';
    this.themeColor = THEME_MAP[id] ?? '#1a1a2e';
  }

  loadProcesses(): void {
    this.loading = true;
    this.error = '';
    this.processService.listProcesses(this.domainSlug, this.appSlug).subscribe({
      next: (res: any) => {
        // API returns array of ProcessDefinitionResponse
        this.processes = Array.isArray(res) ? res : [];
        this.loading = false;
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load processes';
        this.loading = false;
      }
    });
  }

  createNew(): void {
    const defaultSlug = 'process-' + Math.random().toString(36).substring(2, 7);
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'process', defaultSlug]);
  }

  editProcess(slug: string): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'process', slug]);
  }

  startProcess(slug: string): void {
    this.setActionLoading(slug, true);
    this.processService.startProcess(this.domainSlug, this.appSlug, slug).subscribe({
      next: (res) => {
        this.setActionLoading(slug, false);
        const instanceId = res.instance?.id;
        if (instanceId) {
          this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'instances', instanceId]);
        }
      },
      error: (err: any) => {
        this.setActionLoading(slug, false);
        this.actionError[slug] = err?.error?.message || 'Failed to start process';
      }
    });
  }

  publishProcess(slug: string): void {
    this.setActionLoading(slug, true);
    this.processService.publishProcess(this.domainSlug, this.appSlug, slug).subscribe({
      next: () => { this.setActionLoading(slug, false); this.loadProcesses(); },
      error: (err: any) => {
        this.setActionLoading(slug, false);
        this.actionError[slug] = err?.error?.message || 'Failed to publish';
      }
    });
  }

  archiveProcess(slug: string): void {
    if (!confirm('Archive this process? Running instances will continue but no new ones can start.')) return;
    this.setActionLoading(slug, true);
    this.processService.archiveProcess(this.domainSlug, this.appSlug, slug).subscribe({
      next: () => { this.setActionLoading(slug, false); this.loadProcesses(); },
      error: (err: any) => {
        this.setActionLoading(slug, false);
        this.actionError[slug] = err?.error?.message || 'Failed to archive';
      }
    });
  }

  deleteProcess(slug: string): void {
    if (!confirm('Delete this draft process? This cannot be undone.')) return;
    this.setActionLoading(slug, true);
    this.processService.deleteProcess(this.domainSlug, this.appSlug, slug).subscribe({
      next: () => { this.setActionLoading(slug, false); this.loadProcesses(); },
      error: (err: any) => {
        this.setActionLoading(slug, false);
        this.actionError[slug] = err?.error?.message || 'Failed to delete';
      }
    });
  }

  viewInstances(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'instances']);
  }

  goBack(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
  }

  private setActionLoading(slug: string, val: boolean): void {
    this.actionLoading = { ...this.actionLoading, [slug]: val };
  }
}
