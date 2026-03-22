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
    { id: 'hierarchy',     label: 'Entity Hierarchy' },
    { id: 'access',        label: 'Access Control' },
    { id: 'process',       label: 'Process Engine' },
    { id: 'data',          label: 'Data Storage' },
    { id: 'api',           label: 'API Structure' },
    { id: 'stack',         label: 'Tech Stack' },
  ];

  scrollTo(id: string) {
    this.activeSection = id;
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
