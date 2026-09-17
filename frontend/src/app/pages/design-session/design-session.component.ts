import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DesignFeedback, DesignStage, RubricDimension, SessionView, STAGE_LABELS } from '../../models';

@Component({
  selector: 'app-design-session',
  imports: [FormsModule, RouterLink],
  template: `
    @if (session(); as s) {
      <p><a routerLink="/design">← All sessions</a></p>
      <h1>{{ s.questionTitle }}</h1>

      <ol class="stages">
        @for (st of stages; track st) {
          <li [class.done]="isDone(st)" [class.current]="s.stage === st">{{ labels[st] }}</li>
        }
      </ol>

      <section class="card chat">
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
          <textarea rows="8" [(ngModel)]="answer" [placeholder]="placeholder(s.stage)"></textarea>
          @if (error()) { <p class="error">{{ error() }}</p> }
          <div class="actions">
            <button class="primary" (click)="submit()" [disabled]="busy() || !answer.trim()">Submit stage</button>
          </div>
        </section>
      }

      @if (lastFeedback(); as f) {
        <section class="card">
          <h2>Feedback on {{ labels[f.stage] }} — {{ f.stageScore }}/10</h2>
          @if (f.strengths.length) { <p><strong>Covered:</strong> {{ f.strengths.join(', ') }}</p> }
          @if (f.gaps.length) { <p><strong>Missed:</strong> {{ f.gaps.join(', ') }}</p> }
        </section>
      }

      @if (s.rubric; as r) {
        <section class="card success">
          <h2>Scorecard — {{ r.overall }}/100</h2>
          <table>
            <tbody>
              @for (d of dimensions; track d) {
                <tr>
                  <td>{{ d.replace('_', ' ') }}</td>
                  <td><meter min="0" max="10" [value]="r.dimensions[d]"></meter> {{ r.dimensions[d] }}/10</td>
                </tr>
              }
            </tbody>
          </table>
          <p>{{ r.narrative }}</p>
        </section>
      }
    }
  `,
})
export class DesignSessionComponent implements OnInit {
  session = signal<SessionView | null>(null);
  lastFeedback = signal<DesignFeedback | null>(null);
  error = signal<string | null>(null);
  busy = signal(false);
  answer = '';
  labels = STAGE_LABELS;
  stages: DesignStage[] = ['REQUIREMENTS', 'COMPONENTS', 'DATA_MODEL', 'SCALING'];
  dimensions: RubricDimension[] = ['SCALABILITY', 'DATA_MODELING', 'TRADE_OFF_REASONING', 'COMMUNICATION_CLARITY'];

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.session(id).subscribe(s => this.session.set(s));
  }

  isDone(stage: DesignStage): boolean {
    const s = this.session();
    return !!s && this.stages.indexOf(stage) < (s.stage === 'COMPLETE' ? 4 : this.stages.indexOf(s.stage));
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
    this.error.set(null);
    this.api.answer(s.id, this.answer).subscribe({
      next: r => {
        this.session.set(r.session);
        this.lastFeedback.set(r.feedback);
        this.answer = '';
        this.busy.set(false);
      },
      error: err => {
        this.busy.set(false);
        this.error.set(err?.error?.error ?? 'Could not submit');
      },
    });
  }
}
