import { Component, OnInit, AfterViewInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../core/services/auth.service';
import { DomainService } from '../../../../core/services/domain.service';
import { HeroSectionComponent } from '../../../../shared/components/hero-section/hero-section.component';
import { ModernButtonComponent } from '../../../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../../../shared/components/modern-card/modern-card.component';

declare var lucide: any;

@Component({
  selector: 'home-page',
  standalone: true,
  imports: [CommonModule, RouterModule, HeroSectionComponent, ModernButtonComponent, ModernCardComponent],
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.css']
})

export class HomePageComponent implements OnInit, AfterViewInit {
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

  ngAfterViewInit(): void {
    if (typeof lucide !== 'undefined') {
      lucide.createIcons();
    }
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
