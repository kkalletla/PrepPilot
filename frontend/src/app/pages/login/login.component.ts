import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <section class="card narrow">
      <h1>{{ mode() === 'login' ? 'Log in' : 'Create account' }}</h1>
      <form (ngSubmit)="submit()">
        <label>Email <input name="email" type="email" [(ngModel)]="email" required autocomplete="email" /></label>
        <label>Password <input name="password" type="password" [(ngModel)]="password" required minlength="8"
                                autocomplete="current-password" /></label>
        @if (error()) { <p class="error">{{ error() }}</p> }
        <button type="submit" [disabled]="busy()">{{ mode() === 'login' ? 'Log in' : 'Register' }}</button>
      </form>
      <p class="muted">
        @if (mode() === 'login') {
          No account? <button class="link" (click)="mode.set('register')">Register</button>
        } @else {
          Already registered? <button class="link" (click)="mode.set('login')">Log in</button>
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

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    this.error.set(null);
    this.busy.set(true);
    const call = this.mode() === 'login'
      ? this.auth.login(this.email, this.password)
      : this.auth.register(this.email, this.password);
    call.subscribe({
      next: () => this.router.navigate(['/']),
      error: err => {
        this.busy.set(false);
        this.error.set(err?.error?.error ?? 'Something went wrong');
      },
    });
  }
}
