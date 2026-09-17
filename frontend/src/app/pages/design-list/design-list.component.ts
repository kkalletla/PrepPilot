import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { QuestionSummary, SessionView, STAGE_LABELS } from '../../models';
import { UpgradePromptComponent } from '../../shared/upgrade-prompt.component';

@Component({
  selector: 'app-design-list',
  imports: [RouterLink, UpgradePromptComponent],
  template: `
    <h1>System Design Mock Interview</h1>
    @if (limit(); as msg) { <app-upgrade-prompt [message]="msg" /> }
    @if (error()) { <p class="error">{{ error() }}</p> }
    <section class="card">
      <h2>Question bank</h2>
      <table>
        <thead><tr><th>Question</th><th>Category</th><th>Level</th><th></th></tr></thead>
        <tbody>
          @for (q of questions(); track q.id) {
            <tr>
              <td>{{ q.title }}</td>
              <td>{{ q.category }}</td>
              <td><span class="pill">{{ q.seniority }}</span></td>
              <td><button (click)="start(q)">Start session</button></td>
            </tr>
          }
        </tbody>
      </table>
    </section>

    <section class="card">
      <h2>Your sessions</h2>
      @if (sessions().length === 0) { <p class="muted">No sessions yet.</p> }
      <ul class="plain">
        @for (s of sessions(); track s.id) {
          <li>
            <a [routerLink]="['/design/sessions', s.id]">{{ s.questionTitle }}</a>
            — {{ labels[s.stage] }}
            @if (s.rubric) { · score {{ s.rubric.overall }}/100 }
          </li>
        }
      </ul>
    </section>
  `,
})
export class DesignListComponent implements OnInit {
  questions = signal<QuestionSummary[]>([]);
  sessions = signal<SessionView[]>([]);
  labels = STAGE_LABELS;
  limit = signal<string | null>(null);
  error = signal<string | null>(null);

  constructor(private api: ApiService, private router: Router) {}

  ngOnInit(): void {
    this.api.questions().subscribe(q => this.questions.set(q));
    this.api.sessions().subscribe(s => this.sessions.set(s));
  }

  start(q: QuestionSummary): void {
    this.limit.set(null);
    this.error.set(null);
    this.api.startSession(q.id).subscribe({
      next: s => this.router.navigate(['/design/sessions', s.id]),
      error: err => {
        if (err?.status === 402) this.limit.set(err.error?.error ?? 'Free-tier limit reached');
        else this.error.set(err?.error?.error ?? 'Could not start session');
      },
    });
  }
}
