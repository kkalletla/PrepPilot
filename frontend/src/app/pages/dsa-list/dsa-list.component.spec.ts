import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { DsaListComponent } from './dsa-list.component';

describe('DsaListComponent', () => {
  it('shows a Pro badge on locked problems', async () => {
    await TestBed.configureTestingModule({
      imports: [DsaListComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const fixture = TestBed.createComponent(DsaListComponent);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);
    http.expectOne(r => r.url === '/api/dsa/problems').flush([
      { id: 1, slug: 'two-sum', title: 'Two Sum', category: 'ARRAYS', difficulty: 'EASY', status: 'SOLVED', hintsUsed: 1, locked: false },
      { id: 2, slug: 'trw', title: 'Trapping Rain Water', category: 'ARRAYS', difficulty: 'HARD', status: null, hintsUsed: 0, locked: true },
    ]);
    fixture.detectChanges();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Solved');
    expect(rows[1].querySelector('.badge.pro')?.textContent).toContain('Pro');
    http.verify();
  });
});
