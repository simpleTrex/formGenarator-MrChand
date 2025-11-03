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

  constructor(
    private authService: AuthService,
    private domainService: DomainService,
    private router: Router
  ) {
    this._authService = authService;
  }

  ngOnInit(): void {
    if (this._authService.isLoggedIn()) {
      this.loadUserDomains();
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
}
