import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DesignFeedback, DesignStage, RubricDimension, SessionView, STAGE_LABELS } from '../../models';
import { apiMessage } from '../../shared/api-error';
import { LoadingComponent } from '../../shared/loading.component';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-design-session',
  imports: [FormsModule, RouterLink, LoadingComponent],
  template: `
    @if (session(); as s) {
      <p><a routerLink="/design">← All sessions</a></p>
      <h1>{{ s.questionTitle }}</h1>

      <ol class="stages" aria-label="Interview stages">
        @for (st of stages; track st) {
          <li [class.done]="isDone(st)" [class.current]="s.stage === st">
            {{ labels[st] }}
            @if (scoreFor(st) !== null) { <span class="stage-score">{{ scoreFor(st) }}/10</span> }
          </li>
        }
      </ol>

      <section class="card chat" #chat>
        @for (t of s.transcript; track $index) {
          <div class="turn" [class.coach]="t.role === 'COACH'" [class.candidate]="t.role === 'CANDIDATE'">
            <span class="who">{{ t.role === 'COACH' ? 'Interviewer' : 'You' }} · {{ labels[t.stage] }}</span>
            <p>{{ t.content }}</p>
          </div>
        }
      </section>

      @if (s.stage !== 'COMPLETE') {
        <section class="card">
          <h2>{{ labels[s.stage] }}</h2>
          <p class="muted small">{{ guidance(s.stage) }}</p>
          <textarea rows="8" [(ngModel)]="answer" [placeholder]="placeholder(s.stage)" [disabled]="busy()"></textarea>
          <div class="actions">
            <button class="primary" (click)="submit()" [disabled]="busy() || !answer.trim()">
              @if (busy()) { <span class="spinner"></span> }
              Submit {{ labels[s.stage].toLowerCase() }}
            </button>
            <span class="muted small">{{ wordCount() }} words</span>
          </div>
        </section>
      }

      @if (lastFeedback(); as f) {
        <section class="card feedback">
          <h2>Feedback on {{ labels[f.stage] }} — {{ f.stageScore }}/10</h2>
          @if (f.strengths.length) { <p><span class="badge ok">Covered</span> {{ f.strengths.join(', ') }}</p> }
          @if (f.gaps.length) { <p><span class="badge warn">Missed</span> {{ f.gaps.join(', ') }}</p> }
        </section>
      }

      @if (s.rubric; as r) {
        <section class="card success">
          <h2>Scorecard — {{ r.overall }}/100</h2>
          <div class="table-wrap">
            <table>
              <tbody>
                @for (d of dimensions; track d) {
                  <tr>
                    <td>{{ d.replaceAll('_', ' ') }}</td>
                    <td class="meter-cell"><meter min="0" max="10" low="4" high="7" optimum="10" [value]="r.dimensions[d]"></meter> {{ r.dimensions[d] }}/10</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <p>{{ r.narrative }}</p>
          <a class="button primary" routerLink="/design">Try another question →</a>
        </section>
      }
    } @else {
      <app-loading label="Loading session…" />
    }
  `,
})
export class DesignSessionComponent implements OnInit {
  session = signal<SessionView | null>(null);
  lastFeedback = signal<DesignFeedback | null>(null);
  stageScores = signal<Partial<Record<DesignStage, number>>>({});
  busy = signal(false);
  answer = '';
  labels = STAGE_LABELS;
  stages: DesignStage[] = ['REQUIREMENTS', 'COMPONENTS', 'DATA_MODEL', 'SCALING'];
  dimensions: RubricDimension[] = ['SCALABILITY', 'DATA_MODELING', 'TRADE_OFF_REASONING', 'COMMUNICATION_CLARITY'];

  constructor(private api: ApiService, private route: ActivatedRoute, private toast: ToastService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.session(id).subscribe({
      next: s => this.session.set(s),
      error: err => this.toast.error(apiMessage(err, 'Session not found')),
    });
  }

  isDone(stage: DesignStage): boolean {
    const s = this.session();
    return !!s && this.stages.indexOf(stage) < (s.stage === 'COMPLETE' ? 4 : this.stages.indexOf(s.stage));
  }

  scoreFor(stage: DesignStage): number | null { return this.stageScores()[stage] ?? null; }
  wordCount(): number { return this.answer.trim() ? this.answer.trim().split(/\s+/).length : 0; }

  guidance(stage: DesignStage): string {
    switch (stage) {
      case 'REQUIREMENTS': return 'Clarify scope, estimate scale, and state the read/write ratio before drawing anything.';
      case 'COMPONENTS': return 'Name the services and how a request flows through them. Justify each choice.';
      case 'DATA_MODEL': return 'Entities, keys, and the store you would pick — and what the alternative would cost.';
      case 'SCALING': return 'Where it breaks at 10x, and what you change. Mention the hot spots explicitly.';
      default: return '';
    }
  }

  placeholder(stage: DesignStage): string {
    switch (stage) {
      case 'REQUIREMENTS': return 'Functional and non-functional requirements, scale estimates, read/write ratio…';
      case 'COMPONENTS': return 'Major services, API surface, caches, queues, and how a request flows…';
      case 'DATA_MODEL': return 'Entities, keys, storage choice and why, access patterns…';
      case 'SCALING': return 'Sharding, replication, caching, hot spots, failure modes at 10x…';
      default: return '';
    }
  }

  submit(): void {
    const s = this.session()!;
    this.busy.set(true);
    this.api.answer(s.id, this.answer).subscribe({
      next: r => {
        this.session.set(r.session);
        this.lastFeedback.set(r.feedback);
        this.stageScores.update(m => ({ ...m, [r.feedback.stage]: r.feedback.stageScore }));
        this.answer = '';
        this.busy.set(false);
        if (r.session.stage === 'COMPLETE') this.toast.success(`Session complete: ${r.session.rubric?.overall}/100`);
      },
      error: err => { this.busy.set(false); this.toast.error(apiMessage(err, 'Could not submit')); },
    });
  }
}
