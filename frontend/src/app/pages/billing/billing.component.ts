import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { BillingStatus } from '../../models';
import { apiMessage } from '../../shared/api-error';
import { LoadingComponent } from '../../shared/loading.component';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-billing',
  imports: [LoadingComponent],
  template: `
    <h1>Account & Billing</h1>
    @if (notice(); as n) { <p class="card success">{{ n }}</p> }

    @if (status(); as s) {
      <div class="two-col">
        <section class="card">
          <h2>Plan</h2>
          <p><span class="pill" [attr.data-tier]="s.tier">{{ s.tier }}</span> <span class="muted">{{ s.status.replace('_', ' ') }}</span></p>
          @if (s.unlimited) {
            <p>You're on Pro: unlimited sessions, every tier and question, full history.</p>
            <button (click)="portal()" [disabled]="busy()">Manage billing</button>
          } @else {
            <ul class="perks">
              <li>Unlimited DSA problems and design sessions</li>
              <li>Hard and FAANG-bar tiers unlocked</li>
              <li>Staff-level design questions</li>
              <li>Full history and progression tracking</li>
            </ul>
            @if (s.billingConfigured) {
              <div class="actions">
                <button class="primary" (click)="checkout()" [disabled]="busy()">
                  @if (busy()) { <span class="spinner"></span> } Upgrade to Pro
                </button>
                @if (s.hasStripeCustomer) { <button (click)="portal()" [disabled]="busy()">Billing history</button> }
              </div>
            } @else {
              <p class="muted small">Billing is not configured on this server yet.</p>
            }
          }
        </section>

        <section class="card">
          <h2>Usage</h2>
          <div class="usage-row">
            <span>DSA problems today</span>
            <span><strong>{{ s.dsaUsedToday }}</strong>@if (s.dsaDailyLimit !== null) { / {{ s.dsaDailyLimit }} } @else { <span class="muted">unlimited</span> }</span>
          </div>
          @if (s.dsaDailyLimit !== null) { <meter min="0" [max]="s.dsaDailyLimit" [value]="s.dsaUsedToday"></meter> }
          <div class="usage-row">
            <span>Design sessions this week</span>
            <span><strong>{{ s.designUsedThisWeek }}</strong>@if (s.designWeeklyLimit !== null) { / {{ s.designWeeklyLimit }} } @else { <span class="muted">unlimited</span> }</span>
          </div>
          @if (s.designWeeklyLimit !== null) { <meter min="0" [max]="s.designWeeklyLimit" [value]="s.designUsedThisWeek"></meter> }
          @if (!s.unlimited) { <p class="muted small">Counters reset daily (DSA) and on a rolling 7-day window (design).</p> }
        </section>
      </div>
    } @else {
      <app-loading />
    }
  `,
})
export class BillingComponent implements OnInit {
  status = signal<BillingStatus | null>(null);
  notice = signal<string | null>(null);
  busy = signal(false);

  constructor(private api: ApiService, private route: ActivatedRoute, private toast: ToastService) {}

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
    call.subscribe({
      next: r => { window.location.href = r.url; },
      error: err => { this.busy.set(false); this.toast.error(apiMessage(err, 'Billing request failed')); },
    });
  }
}
