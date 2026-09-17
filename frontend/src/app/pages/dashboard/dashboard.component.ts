import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DashboardView, ProblemCategory, SessionView } from '../../models';
import { LoadingComponent } from '../../shared/loading.component';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, LoadingComponent],
  template: `
    <h1>Dashboard</h1>
    @if (dash(); as d) {
      <div class="stats">
        <div class="stat"><span class="big">{{ d.streakDays }}</span><span>day streak {{ d.streakDays > 0 ? '🔥' : '' }}</span></div>
        <div class="stat"><span class="big">{{ d.solvedCount }}</span><span>problems solved</span></div>
        <div class="stat"><span class="big">{{ d.inProgressCount }}</span><span>in progress</span></div>
      </div>

      <div class="two-col">
        <section class="card">
          <h2>DSA Coach</h2>
          <p class="muted">Graduated hints, never full answers.</p>
          @if (d.recent.length) {
            <a class="button primary" [routerLink]="['/dsa', d.recent[0].problemId]">Resume last problem</a>
          } @else {
            <a class="button primary" routerLink="/dsa" [queryParams]="{ category: 'ARRAYS', difficulty: 'EASY' }">Start with Arrays · Easy</a>
          }
        </section>
        <section class="card">
          <h2>System Design</h2>
          <p class="muted">Staged mock interview, rubric-graded.</p>
          @if (openSession(); as s) {
            <a class="button primary" [routerLink]="['/design/sessions', s.id]">Resume: {{ s.questionTitle }}</a>
          } @else {
            <a class="button primary" routerLink="/design">Start a session</a>
          }
        </section>
      </div>

      <section class="card">
        <h2>Tier progress</h2>
        <p class="muted">Three solves at a tier with at most one hint each unlock the next tier.</p>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Category</th><th>Recommended tier</th><th></th></tr></thead>
            <tbody>
              @for (c of categories; track c) {
                <tr>
                  <td>{{ c.replace('_', ' ') }}</td>
                  <td><span class="pill" [attr.data-tier]="d.recommendedTiers[c]">{{ d.recommendedTiers[c].replace('_', ' ') }}</span></td>
                  <td><a routerLink="/dsa" [queryParams]="{ category: c, difficulty: d.recommendedTiers[c] }">Practice →</a></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      @if (d.historyDays !== null) {
        <p class="muted small">Showing the last {{ d.historyDays }} days of activity. <a routerLink="/billing">Pro</a> keeps your full history.</p>
      }
    } @else {
      <app-loading />
    }
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
