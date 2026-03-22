import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProcessService } from '../../../../core/services/process.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ProcessInstance } from '../../../../core/models/process.model';

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

  instances: ProcessInstance[] = [];
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

    // Only owners/admins may view the instance list
    const ctx = this.auth.getContext();
    if (!ctx || ctx.principalType !== 'OWNER') {
      // Redirect normal users back to the app home
      this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
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
    this.processService.listInstances(this.domainSlug, this.appSlug).subscribe({
      next: (list) => { this.instances = list; this.loading = false; },
      error: (err: any) => { this.error = err?.error?.message || 'Failed to load instances'; this.loading = false; }
    });
  }

  viewInstance(id: string): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'instances', id]);
  }

  goBack(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
  }

  countByStatus(status: string): number {
    return this.instances.filter(i => i.status === status).length;
  }

  formatDate(iso?: string): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
      + ' · ' + d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }
}
