import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ModernInputComponent } from '../../../../shared/components/modern-input/modern-input.component';
import { ModernButtonComponent } from '../../../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../../../shared/components/modern-card/modern-card.component';

@Component({
  selector: 'app-domain-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ModernInputComponent, ModernButtonComponent, ModernCardComponent],
  templateUrl: './domain-signup.component.html',
  styleUrls: ['./domain-signup.component.css']
})
export class DomainSignupComponent {
  form: FormGroup;
  submitted = false;
  loading = false;
  message = '';
  error = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      domainSlug: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
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
    this.authService.signupDomain(
      this.f['domainSlug'].value,
      this.f['username'].value,
      this.f['email'].value,
      this.f['password'].value
    ).subscribe({
      next: () => {
        this.message = 'User created. Please login.';
        this.error = '';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/domain-login']), 1200);
      },
      error: (err: any) => {
        this.error = err?.error ?? 'Signup failed';
        this.loading = false;
      }
    });
  }
}
