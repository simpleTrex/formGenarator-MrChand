import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ModernInputComponent } from '../../../../shared/components/modern-input/modern-input.component';
import { ModernButtonComponent } from '../../../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../../../shared/components/modern-card/modern-card.component';

@Component({
  selector: 'app-owner-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ModernInputComponent, ModernButtonComponent, ModernCardComponent],
  templateUrl: './owner-signup.component.html',
  styleUrls: ['./owner-signup.component.css']
})
export class OwnerSignupComponent {
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
      displayName: ['', Validators.required],
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
    this.authService.signupOwner(
      this.f['displayName'].value,
      this.f['email'].value,
      this.f['password'].value
    ).subscribe({
      next: () => {
        this.message = 'Account created. Please log in.';
        this.error = '';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/owner-login']), 1200);
      },
      error: (err: any) => {
        this.error = err?.error ?? 'Signup failed';
        this.loading = false;
      }
    });
  }
}
