import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { DomainService } from '../services/domain.service';

@Component({
  selector: 'app-domain-create',
  templateUrl: './domain-create.component.html',
  styleUrls: ['./domain-create.component.css']
})
export class DomainCreateComponent {
  name = '';
  slug = '';
  description = '';
  industry = '';
  error = '';
  submitting = false;

  constructor(private domainService: DomainService, private router: Router) {}

  onNameInput() {
    if (!this.slug) {
      this.slug = this.slugify(this.name);
    }
  }

  slugify(input: string): string {
    return (input || '')
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+/, '')
      .replace(/-+$/, '');
  }

  create() {
    this.error = '';
    if (!this.name || !this.slug) return;
    this.submitting = true;
    const payload = {
      name: this.name,
      slug: this.slugify(this.slug || this.name),
      description: this.description || undefined,
      industry: this.industry || undefined,
    };
    this.domainService.createDomain(payload).subscribe({
      next: (res) => {
        const s = res?.slug || payload.slug;
        this.router.navigate(['/domain', s]);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create domain';
        this.submitting = false;
      }
    });
  }
}
