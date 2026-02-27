import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ModernInputComponent } from '../../../../shared/components/modern-input/modern-input.component';
import { ModernButtonComponent } from '../../../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../../../shared/components/modern-card/modern-card.component';

@Component({
  selector: 'app-domain-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ModernInputComponent, ModernButtonComponent, ModernCardComponent],
  templateUrl: './domain-login.component.html',
  styleUrls: ['./domain-login.component.css']
})
export class DomainLoginComponent {
  form: FormGroup;
  loading = false;
  submitted = false;
  error = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      domainSlug: ['', Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  get f() {
    return this.form.controls;
  }

  onSubmit() {
    this.submitted = true;
    if (this.form.invalid) {
      return;
    }
    this.loading = true;
    this.authService.loginDomain(
      this.f['domainSlug'].value,
      this.f['username'].value,
      this.f['password'].value
    ).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
        this.router.navigate([returnUrl]);
      },
      error: (err: any) => {
        this.error = err?.error ?? 'Login failed';
        this.loading = false;
      }
    });
  }
}
