import { Component, OnInit, OnChanges, Input, Output, EventEmitter } from '@angular/core';
import { DomainService } from '../../../../core/services/domain.service';

@Component({
  selector: 'app-model-template-modal',
  templateUrl: './model-template-modal.component.html',
  styleUrls: ['./model-template-modal.component.css']
})
export class ModelTemplateModalComponent implements OnInit, OnChanges {
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

    // Using the model template endpoints we created
    this.domainService.getModelTemplates(this.domainSlug, this.appSlug).subscribe({
      next: (templates: any[]) => {
        this.templates = templates || [];
        this.loading = false;
      },
      error: (err: any) => {
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
      'Sales': '📊',
      'Inventory': '📦',
      'General': '📄'
    };
    return icons[category] || '📄';
  }

  getFieldCount(template: any): number {
    return template.fields?.length || 0;
  }

  getFieldPreview(template: any): string {
    const fields = template.fields || [];
    if (fields.length === 0) return 'No fields';

    const preview = fields.slice(0, 3).map((f: any) => {
      // Try displayName from config first, then key, then fallback to name
      return f.config?.displayName || f.key || f.name || 'Field';
    }).join(', ');
    return fields.length > 3 ? `${preview}, +${fields.length - 3} more` : preview;
  }
}