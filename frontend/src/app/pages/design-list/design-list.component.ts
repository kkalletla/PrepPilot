import { Component, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { QuestionSummary, SessionView, STAGE_LABELS } from '../../models';
import { apiMessage, isPaymentRequired } from '../../shared/api-error';
import { LoadingComponent } from '../../shared/loading.component';
import { ToastService } from '../../shared/toast.service';
import { UpgradePromptComponent } from '../../shared/upgrade-prompt.component';

@Component({
  selector: 'app-design-list',
  imports: [RouterLink, UpgradePromptComponent, LoadingComponent],
  template: `
    <h1>System Design Mock Interview</h1>
    @if (limit(); as msg) { <app-upgrade-prompt [message]="msg" /> }

    <section class="card">
      <h2>Question bank</h2>
      @if (loading()) {
        <app-loading />
      } @else {
        <div class="table-wrap">
          <table>
            <thead><tr><th>Question</th><th>Category</th><th>Level</th><th></th></tr></thead>
            <tbody>
              @for (q of questions(); track q.id) {
                <tr [class.locked]="q.locked">
                  <td>{{ q.title }} @if (q.locked) { <span class="badge pro" title="Pro feature">🔒 Pro</span> }</td>
                  <td>{{ q.category.replace('_', ' ') }}</td>
                  <td><span class="pill" [attr.data-level]="q.seniority">{{ q.seniority }}</span></td>
                  <td>
                    @if (q.locked) {
                      <a class="button" routerLink="/billing">Unlock</a>
                    } @else {
                      <button class="primary" (click)="start(q)" [disabled]="starting() === q.id">
                        @if (starting() === q.id) { <span class="spinner"></span> }
                        Start session
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    <section class="card">
      <h2>Your sessions</h2>
      @if (sessions().length === 0) { <p class="muted">No sessions yet — start one above. A session takes about 20 minutes.</p> }
      <ul class="plain">
        @for (s of sessions(); track s.id) {
          <li>
            <a [routerLink]="['/design/sessions', s.id]">{{ s.questionTitle }}</a>
            <span class="muted"> · {{ labels[s.stage] }}</span>
            @if (s.rubric) { <span class="badge ok">{{ s.rubric.overall }}/100</span> }
            @else { <span class="badge">in progress</span> }
          </li>
        }
      </ul>
    </section>
  `,
})
export class DesignListComponent implements OnInit {
  questions = signal<QuestionSummary[]>([]);
  sessions = signal<SessionView[]>([]);
  loading = signal(true);
  starting = signal<number | null>(null);
  limit = signal<string | null>(null);
  labels = STAGE_LABELS;

  constructor(private api: ApiService, private router: Router, private toast: ToastService) {}

  ngOnInit(): void {
    this.api.questions().subscribe({
      next: q => { this.questions.set(q); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
    this.api.sessions().subscribe(s => this.sessions.set(s));
  }

  start(q: QuestionSummary): void {
    this.limit.set(null);
    this.starting.set(q.id);
    this.api.startSession(q.id).subscribe({
      next: s => this.router.navigate(['/design/sessions', s.id]),
      error: err => {
        this.starting.set(null);
        if (isPaymentRequired(err)) this.limit.set(apiMessage(err, 'Free-tier limit reached'));
        else this.toast.error(apiMessage(err, 'Could not start session'));
      },
    });
  }
}
