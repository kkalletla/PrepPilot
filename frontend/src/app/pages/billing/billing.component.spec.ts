import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { BillingComponent } from './billing.component';

describe('BillingComponent', () => {
  it('shows free-tier usage against limits and an upgrade button', async () => {
    await TestBed.configureTestingModule({
      imports: [BillingComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const fixture = TestBed.createComponent(BillingComponent);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/billing/status').flush({
      tier: 'FREE', status: 'NONE', unlimited: false, dsaUsedToday: 2, dsaDailyLimit: 3,
      designUsedThisWeek: 0, designWeeklyLimit: 1, hasStripeCustomer: false, billingConfigured: true,
    });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('2 / 3');
    expect(el.querySelector('button.primary')?.textContent).toContain('Upgrade to Pro');
    http.verify();
  });
});
