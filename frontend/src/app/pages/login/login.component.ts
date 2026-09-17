import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { apiMessage } from '../../shared/api-error';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <section class="card narrow">
      <h1>{{ mode() === 'login' ? 'Welcome back' : 'Create your account' }}</h1>
      <p class="muted">Socratic DSA hints and rubric-graded system design mocks. Free to start.</p>
      <form (ngSubmit)="submit()" novalidate>
        <label>Email
          <input name="email" type="email" [(ngModel)]="email" required autocomplete="email"
                 [class.invalid]="touched() && !emailValid()" (blur)="touched.set(true)" />
          @if (touched() && !emailValid()) { <span class="field-error">Enter a valid email address.</span> }
        </label>
        <label>Password
          <input name="password" type="password" [(ngModel)]="password" required minlength="8"
                 [attr.autocomplete]="mode() === 'login' ? 'current-password' : 'new-password'"
                 [class.invalid]="touched() && !passwordValid()" (blur)="touched.set(true)" />
          @if (touched() && !passwordValid()) { <span class="field-error">At least 8 characters.</span> }
        </label>
        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
        <button type="submit" class="primary wide" [disabled]="busy()">
          @if (busy()) { <span class="spinner"></span> }
          {{ mode() === 'login' ? 'Log in' : 'Create account' }}
        </button>
      </form>
      <p class="muted center">
        @if (mode() === 'login') {
          No account? <button class="link" type="button" (click)="switch('register')">Register</button>
        } @else {
          Already registered? <button class="link" type="button" (click)="switch('login')">Log in</button>
        }
      </p>
    </section>
  `,
})
export class LoginComponent {
  mode = signal<'login' | 'register'>('login');
  email = '';
  password = '';
  error = signal<string | null>(null);
  busy = signal(false);
  touched = signal(false);

  constructor(private auth: AuthService, private router: Router) {}

  emailValid(): boolean { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim()); }
  passwordValid(): boolean { return this.password.length >= 8; }

  switch(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.error.set(null);
    this.touched.set(false);
  }

  submit(): void {
    this.touched.set(true);
    if (!this.emailValid() || !this.passwordValid()) return;
    this.error.set(null);
    this.busy.set(true);
    const call = this.mode() === 'login'
      ? this.auth.login(this.email.trim(), this.password)
      : this.auth.register(this.email.trim(), this.password);
    call.subscribe({
      next: () => this.router.navigate(['/']),
      error: err => {
        this.busy.set(false);
        this.error.set(apiMessage(err, this.mode() === 'login' ? 'Login failed' : 'Registration failed'));
      },
    });
  }
}
