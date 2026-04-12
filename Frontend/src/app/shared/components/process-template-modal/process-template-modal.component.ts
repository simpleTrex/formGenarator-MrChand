import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { DomainService } from '../../../../core/services/domain.service';

@Component({
  selector: 'app-process-template-modal',
  templateUrl: './process-template-modal.component.html',
  styleUrls: ['./process-template-modal.component.css']
})
export class ProcessTemplateModalComponent implements OnInit {
  @Input() isVisible = false;
  @Input() domainSlug = '';
  @Input() appSlug = '';

  @Output() templateSelected = new EventEmitter<any>();
  @Output() startFromScratch = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  templates: any[] = [];
  loading = false;
  error = '';
  selectedTemplate: any | null = null;

  constructor(private domainService: DomainService) {}

  ngOnInit(): void {
    if (this.isVisible) {
      this.loadTemplates();
    }
  }

  ngOnChanges(): void {
    if (this.isVisible && this.templates.length === 0) {
      this.loadTemplates();
    }
  }

  private loadTemplates(): void {
    this.loading = true;
    this.error = '';

    // Using the process template endpoints we created
    this.domainService.getProcessTemplates(this.domainSlug, this.appSlug).subscribe({
      next: (templates) => {
        this.templates = templates || [];
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load templates';
        this.loading = false;
      }
    });
  }

  selectTemplate(template: any): void {
    this.selectedTemplate = template;
  }

  createFromTemplate(): void {
    if (this.selectedTemplate) {
      this.templateSelected.emit(this.selectedTemplate);
    }
  }

  createBlank(): void {
    this.startFromScratch.emit();
  }

  closeModal(): void {
    this.selectedTemplate = null;
    this.close.emit();
  }

  getTemplateDescription(template: any): string {
    return template.description || 'No description available';
  }

  getCategoryIcon(category: string): string {
    const icons: Record<string, string> = {
      'HR': '👥',
      'Finance': '💰',
      'Operations': '⚙️',
      'IT': '💻',
      'Legal': '📋',
      'General': '📄'
    };
    return icons[category] || '📄';
  }
}