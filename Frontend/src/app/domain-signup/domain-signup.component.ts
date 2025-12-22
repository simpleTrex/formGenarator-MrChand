import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-domain-signup',
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
      error: err => {
        this.error = err?.error ?? 'Signup failed';
        this.loading = false;
      }
    });
  }
}
