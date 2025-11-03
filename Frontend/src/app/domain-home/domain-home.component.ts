import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomainService } from '../services/domain.service';

@Component({
  selector: 'app-domain-home',
  templateUrl: './domain-home.component.html',
  styleUrls: ['./domain-home.component.css']
})
export class DomainHomeComponent implements OnInit {
  slug = '';
  domain: any = null;
  error = '';

  constructor(private route: ActivatedRoute, private domainService: DomainService) {}

  ngOnInit(): void {
    this.slug = this.route.snapshot.params['slug'];
    if (this.slug) {
      this.domainService.getBySlug(this.slug).subscribe({
        next: (res) => this.domain = res,
        error: (err) => this.error = err?.error?.message || 'Domain not found or access denied'
      });
    }
  }
}
