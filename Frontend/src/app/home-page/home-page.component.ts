import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { DomainService } from '../services/domain.service';

@Component({
  selector: 'home-page',
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.css']
})

export class HomePageComponent implements OnInit {
  _authService: AuthService;
  domains: any[] = [];
  loadingDomains = false;
  ownerContext = false;
  notice = '';

  constructor(
    private authService: AuthService,
    private domainService: DomainService,
    private router: Router
  ) {
    this._authService = authService;
  }

  ngOnInit(): void {
    if (!this._authService.isLoggedIn()) {
      return;
    }
    const ctx = this._authService.getContext();
    if (ctx?.principalType === 'OWNER') {
      this.ownerContext = true;
      this.loadUserDomains();
      return;
    }
    this._authService.logout();
    this.ownerContext = false;
    this.notice = 'AdaptiveBP owner portal is restricted to owners. Please login as an owner.';
  }

  loadUserDomains(): void {
    this.loadingDomains = true;
    this.domainService.getMyDomains().subscribe({
      next: (domains) => {
        this.domains = domains;
        this.loadingDomains = false;
      },
      error: (error) => {
        console.error('Error loading domains:', error);
        this.loadingDomains = false;
      }
    });
  }

  navigateToDomain(slug: string): void {
    this.router.navigate(['/domain', slug]);
  }

  get displayName(): string | undefined {
    return this._authService.getContext()?.username || this._authService.getContext()?.email;
  }
}
