# AI Interview Coach — Build Spec

2026-09-17 · @Someone

## Overview & Goals

**What it is:** An AI-powered interview-prep app combining two modules — a **DSA Socratic Coach** (adjustable difficulty, hint-driven, never gives full answers) and a **System Design Mock Interview Simulator** (staged Q&A, rubric-graded feedback).

**Who it's for:** Intermediate-to-senior software engineers preparing for FAANG/big-tech interviews. First validated by KayKay's own prep (arrays/linked-lists now, expanding through the same tiers he's working through).

**Why now:** Built using $140 CAD in Claude Fable credits as dev labor (not in-app AI) — the credits expire by Sept 19, giving a real 2-day build window (today, the 17th, through the 19th), executed with minimal human involvement.

**Success criteria for this build:**

- Feature-complete MVP (both modules) within 10 days
- AI coaching logic sits behind a swappable interface, not hardcoded to one model
- Stripe monetization functional in test mode
- Deployed to a staging environment, passing its own test suite

## Architecture: the AI Abstraction Layer

Every point where the app needs AI-generated coaching content goes through one interface, so the underlying model can be swapped later without touching business logic:

```java
public interface CoachingEngine {
    HintResponse generateHint(HintRequest request);
    DesignFeedback gradeDesignAnswer(DesignAnswerRequest request);
}
```

- `HintRequest` carries: problem id, difficulty tier, the user's current attempt/code, and hint depth already used.
- `DesignAnswerRequest` carries: question id, the user's staged design input, and conversation-so-far.
- Both response objects return structured feedback plus an optional follow-up prompt.

**Implementations:**

- `TemplateCoachingEngine` (MVP default) — returns curated, static hints/rubric feedback keyed by problem + step. No live model calls, zero runtime AI cost, fully testable offline.
- `ClaudeCoachingEngine` / `OpenAiCoachingEngine` (future) — calls a live model once monetization justifies the cost.

**Swap mechanism:** a single Spring config value, e.g. `coaching.engine=template|claude|openai`, selects the active bean via `@ConditionalOnProperty`. No controller or service code changes when the engine changes — this is the concrete proof the abstraction works, and should be verified explicitly in testing (see Acceptance Criteria).

## Data Model

Postgres schema, core tables:

- **users** — id, email, password\_hash, created\_at
- **subscriptions** — user\_id, tier (free/paid), status, stripe\_customer\_id, stripe\_subscription\_id, renewed\_at
- **problems** — id, title, category (arrays, linked lists, trees, graphs, DP, ...), difficulty tier
- **problem\_progress** — user\_id, problem\_id, difficulty, attempts, hints\_used, status (solved/in-progress), last\_attempt\_at
- **design\_questions** — id, title, category, seniority level
- **design\_sessions** — user\_id, question\_id, transcript (staged Q&A), rubric\_breakdown (json), overall\_score, completed\_at
- **usage\_counters** — user\_id, date, dsa\_sessions\_used, design\_sessions\_used (drives free-tier gating)

All AI-related fields (hints given, rubric feedback) are stored as plain structured data, not tied to any specific model provider — keeps history valid across engine swaps.

## DSA Socratic Coach Module

**Difficulty tiers:** Easy → Medium → Hard → "FAANG bar".

**Starting categories:** arrays, linked lists (matches KayKay's own current prep stage), expanding to stacks/queues, trees, graphs, and DP as tiers unlock.

**Hint-flow logic:**

1. User submits an attempt or asks for help on a problem.
2. `CoachingEngine.generateHint()` returns a graduated hint — a nudge first, a stronger hint if asked again, a full approach outline at most — but never full code.
3. Hint depth used per problem is tracked and factored into scoring (fewer hints = better performance signal).
4. Difficulty auto-escalates after N consecutive solves at the current tier with minimal hint usage.

**Streak tracking:** daily solve streak displayed on the dashboard, ties into the free-tier daily usage counter.

## System Design Mock Interview Module

**Starter question bank (tagged by seniority level):** URL shortener, rate limiter, chat application, news feed.

**Flow:**

1. Intro prompt sets the problem.
2. User responds in stages: requirements → high-level components → data model → scaling.
3. `CoachingEngine.gradeDesignAnswer()` challenges trade-offs at each stage (asks follow-ups, pushes on weak reasoning) rather than just collecting answers.
4. At the end, a structured rubric score is returned covering: scalability, data modeling, trade-off reasoning, and communication clarity — plus narrative feedback.

Designed to mirror the format of KayKay's own HLD/LLD prep, so the module doubles as his own practice tool.

## Frontend & UX (Angular 19)

- **Dashboard** — streaks, tier progress, quick-resume into either module.
- **DSA practice screen** — problem statement, code/notes area, hint panel (shows graduated hints on request), difficulty selector.
- **System Design screen** — staged chat-like Q&A flow, rubric scorecard shown at the end of each session.
- **Account/Billing screen** — Stripe customer portal link, current usage counters against free-tier limits.

## Monetization

**Free tier:** e.g. 3 DSA problems/day + 1 system design session/week.

**Paid tier ($9–15/mo):** unlimited sessions, saved history/analytics, difficulty-progression tracking.

**Mechanics:**

- Stripe Checkout for subscription creation.
- Stripe webhooks update subscription status in the DB on renewal/cancellation.
- A usage-gating middleware checks tier + daily/weekly counters before serving a session, falling back to an upgrade prompt when limits are hit.

All of this runs in Stripe **test mode** during the 10-day build — going live with real charges is explicitly gated behind the final human review (see Acceptance Criteria).

## Tech Stack & Environment

- **Backend:** Java 21, Spring Boot 3.x, Postgres, JWT auth.
- **Frontend:** Angular 19.
- **Hosting:** none for now — running entirely locally; a Railway/Render deploy is picked back up later (see "Later: Deployment" below).
- **CI/CD:** GitHub Actions once pushed to a repo — test suite + linter run on every commit; nothing merges to main without passing.

**Credentials handoff (one-time, done by KayKay before the autonomous build starts):**

- GitHub repo created (for version control / later CI, even without a hosting target yet)
- Stripe test account created, test API keys generated
- Env vars set: local Postgres connection string, JWT secret, Stripe test keys

Once these exist, no further human account/credential action is needed for the rest of this build window — hosting and production-deployment credentials are picked up later, in the "Later: Deployment" section.

## Build Plan (Compressed: Sept 17–19)

The real deadline is 2 days, not 10 — scope is intentionally narrower so what ships actually works, rather than a broader build left half-finished.

**Today (Sept 17) — one continuous session, foundation + both modules' core backend:**

- Project scaffold, Postgres schema, JWT auth
- `CoachingEngine` interface + `TemplateCoachingEngine` stub
- DSA module: reduced starter bank (\~10–15 problems across arrays/linked lists), hint-flow logic, tests
- System design module: reduced starter bank (\~4 questions), staged flow, rubric grading, tests
- Basic Angular UI for both (functional, not polished)

**Tomorrow (Sept 18) — frontend polish + monetization:**

- Stripe Checkout + webhooks (test mode), usage-gating middleware
- UI polish pass
- CI pipeline running the full test suite

**Sept 19 — final day, hard deadline:**

- Get everything running end-to-end locally (docker-compose for Postgres is fine even without a hosting deploy)
- KayKay's one required review pass (see Acceptance Criteria)
- Anything not stable by end of day is flagged as backlog rather than shipped half-broken

**On doing it all in one go today:** attempting the entire scope (both modules + Stripe + deploy + full tests) in one unbroken session is high risk — long continuous agent runs tend to drift and skip verification when nothing pauses to check the output. The safer bet is today = foundation + backend for both modules (the hardest, most error-prone part, and where correctness matters most), leaving frontend/Stripe/deploy for the 18th and the review for the 19th. That's still only 2 build-days, not 10, and the credit-burning work runs continuously through today and tomorrow — it just isn't gambled on a single all-or-nothing run.

## Acceptance Criteria — Before Real Payments Are Enabled

- All CI tests passing on the final commit.
- KayKay has run through both modules manually at least once.
- A Stripe test-mode charge has been verified end-to-end, and it's confirmed nothing can misfire into a live charge.
- The `CoachingEngine` swap has been proven — flipping the config between two implementations (even two stub versions) actually changes behavior, confirming the abstraction is real and not just interface-only.
- Basic security check: no secrets committed to the repo, auth guards present on all paid-tier endpoints.

This is the one point in the whole build where a human review is non-negotiable — everything before it can run unattended.

## Later: Deployment (Deferred)

Everything above targets a fully working **local** build by Sept 19. Once that's solid and reviewed, these are the remaining steps to actually put it online — no rush, no credit-expiry pressure:

- Create a hosting account (Railway or Render) and provision a managed Postgres instance
- Point the existing CI pipeline at a deploy step (or add one)
- Swap Stripe test keys for live keys only once ready to accept real payments
- Docker deploy to staging, smoke-test there, then promote to production
- Re-run the Acceptance Criteria checklist against the deployed version before announcing it anywhere
