import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { BillingStatus } from '../../models';

@Component({
  selector: 'app-billing',
  template: `
    <h1>Account & Billing</h1>
    @if (notice(); as n) { <p class="card success">{{ n }}</p> }

    @if (status(); as s) {
      <div class="two-col">
        <section class="card">
          <h2>Plan</h2>
          <p><span class="pill">{{ s.tier }}</span> <span class="muted">{{ s.status }}</span></p>
          @if (s.unlimited) {
            <p>Unlimited DSA problems and system design sessions.</p>
            <button (click)="portal()" [disabled]="busy()">Manage billing</button>
          } @else {
            <p>Pro: unlimited sessions, saved history, difficulty-progression tracking.</p>
            @if (s.billingConfigured) {
              <button class="primary" (click)="checkout()" [disabled]="busy()">Upgrade to Pro</button>
              @if (s.hasStripeCustomer) { <button (click)="portal()" [disabled]="busy()">Billing history</button> }
            } @else {
              <p class="muted">Billing is not configured on this server yet.</p>
            }
          }
          @if (error()) { <p class="error">{{ error() }}</p> }
        </section>

        <section class="card">
          <h2>Usage</h2>
          <table>
            <tbody>
              <tr>
                <td>DSA problems today</td>
                <td>{{ s.dsaUsedToday }}@if (s.dsaDailyLimit !== null) { / {{ s.dsaDailyLimit }} }</td>
              </tr>
              <tr>
                <td>Design sessions this week</td>
                <td>{{ s.designUsedThisWeek }}@if (s.designWeeklyLimit !== null) { / {{ s.designWeeklyLimit }} }</td>
              </tr>
            </tbody>
          </table>
          @if (!s.unlimited) { <p class="muted">Counters reset daily (DSA) and on a rolling 7-day window (design).</p> }
        </section>
      </div>
    }
  `,
})
export class BillingComponent implements OnInit {
  status = signal<BillingStatus | null>(null);
  notice = signal<string | null>(null);
  error = signal<string | null>(null);
  busy = signal(false);

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const c = this.route.snapshot.queryParamMap.get('checkout');
    if (c === 'success') this.notice.set('Payment received. Your plan updates as soon as Stripe confirms it.');
    if (c === 'cancel') this.notice.set('Checkout cancelled. You are still on the free tier.');
    this.api.billingStatus().subscribe(s => this.status.set(s));
  }

  checkout(): void { this.redirect(this.api.checkout()); }
  portal(): void { this.redirect(this.api.portal()); }

  private redirect(call: ReturnType<ApiService['checkout']>): void {
    this.busy.set(true);
    this.error.set(null);
    call.subscribe({
      next: r => { window.location.href = r.url; },
      error: err => { this.busy.set(false); this.error.set(err?.error?.error ?? 'Billing request failed'); },
    });
  }
}
