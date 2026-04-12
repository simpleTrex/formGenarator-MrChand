import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-architecture',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './architecture.component.html',
  styleUrls: ['./architecture.component.css'],
})
export class ArchitectureComponent {
  activeSection = 'overview';

  sections = [
    { id: 'overview',      label: 'System Overview' },
    { id: 'workflow-core', label: 'Workflow Core' },
    { id: 'runtime',       label: 'Runtime Lifecycle' },
    { id: 'access',        label: 'Access And Tasks' },
    { id: 'storage',       label: 'Storage Model' },
    { id: 'api',           label: 'API Surface' },
    { id: 'stack',         label: 'Tech Stack' },
  ];

  scrollTo(id: string) {
    this.activeSection = id;
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
