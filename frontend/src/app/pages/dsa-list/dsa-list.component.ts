import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CATEGORIES, DifficultyTier, ProblemCategory, ProblemSummary, TIERS } from '../../models';
import { LoadingComponent } from '../../shared/loading.component';

@Component({
  selector: 'app-dsa-list',
  imports: [FormsModule, RouterLink, LoadingComponent],
  template: `
    <h1>DSA Coach</h1>
    <div class="filters">
      <label>Category
        <select [(ngModel)]="category" (ngModelChange)="load()">
          <option value="">All</option>
          @for (c of categories; track c) { <option [value]="c">{{ c.replace('_', ' ') }}</option> }
        </select>
      </label>
      <label>Difficulty
        <select [(ngModel)]="difficulty" (ngModelChange)="load()">
          <option value="">All</option>
          @for (t of tiers; track t) { <option [value]="t">{{ t.replace('_', ' ') }}</option> }
        </select>
      </label>
    </div>
    @if (loading()) {
      <app-loading label="Loading problems…" />
    } @else {
      <div class="card table-wrap">
        <table>
          <thead><tr><th>Problem</th><th>Category</th><th>Tier</th><th>Status</th><th>Hints</th></tr></thead>
          <tbody>
            @for (p of problems(); track p.id) {
              <tr [class.locked]="p.locked">
                <td>
                  <a [routerLink]="['/dsa', p.id]">{{ p.title }}</a>
                  @if (p.locked) { <span class="badge pro" title="Pro feature">🔒 Pro</span> }
                </td>
                <td>{{ p.category.replace('_', ' ') }}</td>
                <td><span class="pill" [attr.data-tier]="p.difficulty">{{ p.difficulty.replace('_', ' ') }}</span></td>
                <td>
                  @if (p.status === 'SOLVED') { <span class="badge ok">Solved</span> }
                  @else if (p.status === 'IN_PROGRESS') { <span class="badge">In progress</span> }
                  @else { <span class="muted">—</span> }
                </td>
                <td>{{ p.status ? p.hintsUsed : '' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="muted">No problems match these filters.</td></tr>
            }
          </tbody>
        </table>
      </div>
      @if (anyLocked()) {
        <p class="muted small">🔒 Hard and FAANG-bar problems are part of <a routerLink="/billing">Pro</a>.</p>
      }
    }
  `,
})
export class DsaListComponent implements OnInit {
  problems = signal<ProblemSummary[]>([]);
  loading = signal(true);
  category: ProblemCategory | '' = '';
  difficulty: DifficultyTier | '' = '';
  categories = CATEGORIES;
  tiers = TIERS;

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.category = (q.get('category') as ProblemCategory) ?? '';
    this.difficulty = (q.get('difficulty') as DifficultyTier) ?? '';
    this.load();
  }

  anyLocked(): boolean { return this.problems().some(p => p.locked); }

  load(): void {
    this.loading.set(true);
    this.router.navigate([], { queryParams: { category: this.category || null, difficulty: this.difficulty || null }, replaceUrl: true });
    this.api.problems(this.category, this.difficulty).subscribe({
      next: list => { this.problems.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
