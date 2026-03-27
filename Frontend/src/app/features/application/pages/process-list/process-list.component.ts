import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProcessService } from '../../../../core/services/process.service';
import { DomainService } from '../../../../core/services/domain.service';
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

  statusBadge = STATUS_BADGE_CLASS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private processService: ProcessService,
    private domainService: DomainService,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug = this.route.snapshot.params['appSlug'];
    this.loadProcesses();
  }

  loadProcesses(): void {
    this.loading = true;
    this.error = '';
    this.processService.listProcesses(this.domainSlug, this.appSlug).subscribe({
      next: (res: any) => {
        // API returns array of ProcessDefinition (not wrapped in response)
        this.processes = Array.isArray(res)
          ? res.map((d: ProcessDefinition) => ({ definition: d, nodeCount: d.nodes?.length ?? 0, edgeCount: d.edges?.length ?? 0, valid: false }))
          : [];
        this.loading = false;
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to load processes';
        this.loading = false;
      }
    });
  }

  createNew(): void {
    // Navigate to process builder where users can choose templates or start from scratch
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'process']);
  }

  editProcess(slug: string): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'processes', slug, 'edit']);
  }

  startProcess(slug: string): void {
    this.setActionLoading(slug, true);
    this.processService.startProcess(this.domainSlug, this.appSlug).subscribe({
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
    this.processService.publishProcess(this.domainSlug, this.appSlug).subscribe({
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
    this.processService.archiveProcess(this.domainSlug, this.appSlug).subscribe({
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
    this.processService.deleteProcess(this.domainSlug, this.appSlug).subscribe({
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
