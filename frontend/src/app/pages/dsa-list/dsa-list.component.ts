import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CATEGORIES, DifficultyTier, ProblemCategory, ProblemSummary, TIERS } from '../../models';

@Component({
  selector: 'app-dsa-list',
  imports: [FormsModule, RouterLink],
  template: `
    <h1>DSA Coach</h1>
    <div class="filters">
      <label>Category
        <select [(ngModel)]="category" (ngModelChange)="load()">
          <option value="">All</option>
          @for (c of categories; track c) { <option [value]="c">{{ c }}</option> }
        </select>
      </label>
      <label>Difficulty
        <select [(ngModel)]="difficulty" (ngModelChange)="load()">
          <option value="">All</option>
          @for (t of tiers; track t) { <option [value]="t">{{ t }}</option> }
        </select>
      </label>
    </div>
    <table class="card">
      <thead><tr><th>Problem</th><th>Category</th><th>Tier</th><th>Status</th><th>Hints</th></tr></thead>
      <tbody>
        @for (p of problems(); track p.id) {
          <tr>
            <td><a [routerLink]="['/dsa', p.id]">{{ p.title }}</a></td>
            <td>{{ p.category }}</td>
            <td><span class="pill">{{ p.difficulty }}</span></td>
            <td>{{ p.status ?? '—' }}</td>
            <td>{{ p.status ? p.hintsUsed : '' }}</td>
          </tr>
        } @empty {
          <tr><td colspan="5" class="muted">No problems match these filters.</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class DsaListComponent implements OnInit {
  problems = signal<ProblemSummary[]>([]);
  category: ProblemCategory | '' = '';
  difficulty: DifficultyTier | '' = '';
  categories = CATEGORIES;
  tiers = TIERS;

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.category = (q.get('category') as ProblemCategory) ?? '';
    this.difficulty = (q.get('difficulty') as DifficultyTier) ?? '';
    this.load();
  }

  load(): void {
    this.api.problems(this.category, this.difficulty).subscribe(list => this.problems.set(list));
  }
}
