import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { HintResponse, ProblemDetail, SolveResult } from '../../models';
import { apiMessage, isPaymentRequired } from '../../shared/api-error';
import { LoadingComponent } from '../../shared/loading.component';
import { ToastService } from '../../shared/toast.service';
import { UpgradePromptComponent } from '../../shared/upgrade-prompt.component';

@Component({
  selector: 'app-dsa-problem',
  imports: [FormsModule, RouterLink, UpgradePromptComponent, LoadingComponent],
  template: `
    @if (problem(); as p) {
      <p><a routerLink="/dsa">← All problems</a></p>
      <h1>{{ p.title }} <span class="pill" [attr.data-tier]="p.difficulty">{{ p.difficulty.replace('_', ' ') }}</span></h1>
      <p class="muted">{{ p.category.replace('_', ' ') }}</p>

      @if (p.locked) {
        <app-upgrade-prompt message="This tier is part of Pro. You can read the problem, but hints and attempts need an upgrade." />
      } @else {
        @if (limit(); as msg) { <app-upgrade-prompt [message]="msg" /> }
      }

      <div class="two-col">
        <section class="card">
          <h2>Problem</h2>
          <p class="statement">{{ p.statement }}</p>

          <h2>Your code / notes</h2>
          <textarea rows="14" class="code" [(ngModel)]="attempt" [disabled]="solved() || p.locked"
                    placeholder="Sketch your approach or paste code here…" spellcheck="false"></textarea>
          <div class="actions">
            <button (click)="saveAttempt()" [disabled]="solved() || p.locked || saving()">Save attempt</button>
            <button class="primary" (click)="markSolved()" [disabled]="solved() || p.locked || saving()">Mark solved</button>
          </div>
          @if (p.progress; as pr) {
            <p class="muted small">Attempts {{ pr.attempts }} · Hints {{ pr.hintsUsed }} ·
              @if (pr.status === 'SOLVED') { <span class="badge ok">Solved</span> } @else { <span class="badge">In progress</span> }
            </p>
          }
        </section>

        <section class="card">
          <h2>Hints</h2>
          <p class="muted small">Ask for a nudge first. Each request goes one level deeper; the last is an approach outline — never code. Fewer hints = stronger signal.</p>
          @for (h of hints(); track h.depth) {
            <div class="hint">
              <strong>Hint {{ h.depth }} / {{ h.maxDepth }}</strong>
              <p>{{ h.hint }}</p>
              @if (h.followUpPrompt) { <p class="muted small"><em>{{ h.followUpPrompt }}</em></p> }
            </div>
          }
          <button (click)="askHint()" [disabled]="solved() || p.locked || exhausted() || hinting()">
            @if (hinting()) { <span class="spinner"></span> }
            {{ hints().length === 0 ? 'I need a hint' : exhausted() ? 'No deeper hints' : 'Go deeper' }}
          </button>
        </section>
      </div>

      @if (result(); as r) {
        <section class="card success">
          <h2>Solved! 🎉</h2>
          <p>Streak: <strong>{{ r.streakDays }}</strong> day(s).
            @if (r.escalated) { You're ready for <strong>{{ r.recommendedTier.replace('_', ' ') }}</strong> problems in this category. }
            @else { Keep going at {{ r.recommendedTier.replace('_', ' ') }} to unlock the next tier. }
          </p>
          <a class="button primary" routerLink="/dsa" [queryParams]="{ category: p.category, difficulty: r.recommendedTier }">Next problem →</a>
        </section>
      }
    } @else {
      <app-loading label="Loading problem…" />
    }
  `,
})
export class DsaProblemComponent implements OnInit {
  problem = signal<ProblemDetail | null>(null);
  hints = signal<HintResponse[]>([]);
  result = signal<SolveResult | null>(null);
  limit = signal<string | null>(null);
  saving = signal(false);
  hinting = signal(false);
  attempt = '';

  constructor(private api: ApiService, private route: ActivatedRoute, private toast: ToastService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.problem(id).subscribe({
      next: p => this.problem.set(p),
      error: err => this.toast.error(apiMessage(err, 'Problem not found')),
    });
  }

  solved(): boolean { return this.problem()?.progress?.status === 'SOLVED'; }
  exhausted(): boolean { return this.hints().at(-1)?.exhausted ?? false; }

  saveAttempt(): void {
    const p = this.problem()!;
    this.saving.set(true);
    this.api.attempt(p.id, this.attempt).subscribe({
      next: progress => { this.problem.set({ ...p, progress }); this.saving.set(false); this.toast.success('Attempt saved'); },
      error: err => { this.saving.set(false); this.handle(err); },
    });
  }

  askHint(): void {
    const p = this.problem()!;
    this.hinting.set(true);
    this.api.hint(p.id, this.attempt).subscribe({
      next: hv => {
        this.hints.update(list => [...list, hv.hint]);
        this.problem.set({ ...p, progress: hv.progress });
        this.hinting.set(false);
      },
      error: err => { this.hinting.set(false); this.handle(err, 'Could not fetch a hint'); },
    });
  }

  markSolved(): void {
    const p = this.problem()!;
    this.saving.set(true);
    this.api.solve(p.id).subscribe({
      next: r => {
        this.result.set(r);
        this.problem.set({ ...p, progress: r.progress });
        this.saving.set(false);
        this.toast.success(r.escalated ? `Tier unlocked: ${r.recommendedTier.replace('_', ' ')}` : 'Marked solved');
      },
      error: err => { this.saving.set(false); this.handle(err); },
    });
  }

  private handle(err: any, fallback = 'Request failed'): void {
    if (isPaymentRequired(err)) this.limit.set(apiMessage(err, 'Free-tier limit reached'));
    else this.toast.error(apiMessage(err, fallback));
  }
}
