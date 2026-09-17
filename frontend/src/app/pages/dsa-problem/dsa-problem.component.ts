import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { HintResponse, ProblemDetail, SolveResult } from '../../models';

@Component({
  selector: 'app-dsa-problem',
  imports: [FormsModule, RouterLink],
  template: `
    @if (problem(); as p) {
      <p><a routerLink="/dsa">← All problems</a></p>
      <h1>{{ p.title }} <span class="pill">{{ p.difficulty }}</span></h1>
      <p class="muted">{{ p.category }}</p>

      <div class="two-col">
        <section class="card">
          <h2>Problem</h2>
          <p>{{ p.statement }}</p>

          <h2>Your code / notes</h2>
          <textarea rows="14" [(ngModel)]="attempt" [disabled]="solved()"
                    placeholder="Sketch your approach or paste code here…"></textarea>
          <div class="actions">
            <button (click)="saveAttempt()" [disabled]="solved()">Save attempt</button>
            <button class="primary" (click)="markSolved()" [disabled]="solved()">Mark solved</button>
          </div>
          @if (p.progress; as pr) {
            <p class="muted">Attempts: {{ pr.attempts }} · Hints used: {{ pr.hintsUsed }} · {{ pr.status }}</p>
          }
        </section>

        <section class="card">
          <h2>Hints</h2>
          <p class="muted">Ask for a nudge first. Each request goes one level deeper; the last is an approach outline — never code.</p>
          @for (h of hints(); track h.depth) {
            <div class="hint">
              <strong>Hint {{ h.depth }} / {{ h.maxDepth }}</strong>
              <p>{{ h.hint }}</p>
              @if (h.followUpPrompt) { <p class="muted"><em>{{ h.followUpPrompt }}</em></p> }
            </div>
          }
          @if (error()) { <p class="error">{{ error() }}</p> }
          <button (click)="askHint()" [disabled]="solved() || exhausted()">
            {{ hints().length === 0 ? 'I need a hint' : exhausted() ? 'No deeper hints' : 'Go deeper' }}
          </button>
        </section>
      </div>

      @if (result(); as r) {
        <section class="card success">
          <h2>Solved!</h2>
          <p>Streak: {{ r.streakDays }} day(s).
            @if (r.escalated) { You're ready for <strong>{{ r.recommendedTier }}</strong> problems in this category. }
            @else { Keep going at {{ r.recommendedTier }} to unlock the next tier. }
          </p>
          <a routerLink="/dsa" [queryParams]="{ category: p.category, difficulty: r.recommendedTier }">Next problem</a>
        </section>
      }
    }
  `,
})
export class DsaProblemComponent implements OnInit {
  problem = signal<ProblemDetail | null>(null);
  hints = signal<HintResponse[]>([]);
  result = signal<SolveResult | null>(null);
  error = signal<string | null>(null);
  attempt = '';

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.problem(id).subscribe(p => this.problem.set(p));
  }

  solved(): boolean { return this.problem()?.progress?.status === 'SOLVED'; }
  exhausted(): boolean { return this.hints().at(-1)?.exhausted ?? false; }

  saveAttempt(): void {
    const p = this.problem()!;
    this.api.attempt(p.id, this.attempt).subscribe(progress => this.problem.set({ ...p, progress }));
  }

  askHint(): void {
    const p = this.problem()!;
    this.error.set(null);
    this.api.hint(p.id, this.attempt).subscribe({
      next: hv => {
        this.hints.update(list => [...list, hv.hint]);
        this.problem.set({ ...p, progress: hv.progress });
      },
      error: err => this.error.set(err?.error?.error ?? 'Could not fetch a hint'),
    });
  }

  markSolved(): void {
    const p = this.problem()!;
    this.api.solve(p.id).subscribe(r => {
      this.result.set(r);
      this.problem.set({ ...p, progress: r.progress });
    });
  }
}
