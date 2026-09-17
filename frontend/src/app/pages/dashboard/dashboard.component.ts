import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DashboardView, ProblemCategory, SessionView } from '../../models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `
    <h1>Dashboard</h1>
    @if (dash(); as d) {
      <div class="stats">
        <div class="stat"><span class="big">{{ d.streakDays }}</span><span>day streak</span></div>
        <div class="stat"><span class="big">{{ d.solvedCount }}</span><span>problems solved</span></div>
        <div class="stat"><span class="big">{{ d.inProgressCount }}</span><span>in progress</span></div>
      </div>

      <section class="card">
        <h2>Tier progress</h2>
        <table>
          <thead><tr><th>Category</th><th>Recommended tier</th><th></th></tr></thead>
          <tbody>
            @for (c of categories; track c) {
              <tr>
                <td>{{ c }}</td>
                <td><span class="pill">{{ d.recommendedTiers[c] }}</span></td>
                <td><a routerLink="/dsa" [queryParams]="{ category: c, difficulty: d.recommendedTiers[c] }">Practice</a></td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }

    <div class="two-col">
      <section class="card">
        <h2>DSA Coach</h2>
        <p>Graduated hints, never full answers.</p>
        @if (dash()?.recent?.length) {
          <a [routerLink]="['/dsa', dash()!.recent[0].problemId]">Resume last problem</a>
        } @else {
          <a routerLink="/dsa">Pick a problem</a>
        }
      </section>
      <section class="card">
        <h2>System Design</h2>
        <p>Staged mock interview, rubric-graded.</p>
        @if (openSession(); as s) {
          <a [routerLink]="['/design/sessions', s.id]">Resume: {{ s.questionTitle }}</a>
        } @else {
          <a routerLink="/design">Start a session</a>
        }
      </section>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  dash = signal<DashboardView | null>(null);
  openSession = signal<SessionView | null>(null);
  categories: ProblemCategory[] = ['ARRAYS', 'LINKED_LISTS'];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.dashboard().subscribe(d => this.dash.set(d));
    this.api.sessions().subscribe(list => this.openSession.set(list.find(s => s.stage !== 'COMPLETE') ?? null));
  }
}
