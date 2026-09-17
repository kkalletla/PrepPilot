import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <a routerLink="/" class="brand">PrepPilot</a>
      @if (auth.isLoggedIn()) {
        <nav>
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
          <a routerLink="/dsa" routerLinkActive="active">DSA Coach</a>
          <a routerLink="/design" routerLinkActive="active">System Design</a>
        </nav>
        <span class="spacer"></span>
        <span class="muted">{{ auth.email() }}</span>
        <button class="link" (click)="logout()">Log out</button>
      }
    </header>
    <main class="container">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {
  constructor(public auth: AuthService, private router: Router) {}

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
