# PrepPilot — Build Status

Last updated: 2026-09-17 (late) · Branch `main` · 15 commits · Backend 65 tests passing · Frontend 8 specs passing

Overall: **~85% of the Sept 17–19 plan complete**. All engineering scope for Sept 17 and Sept 18 is done;
what remains is the Sept 19 verification work that needs a running Postgres, real Stripe test keys and KayKay's review.

## Summary by phase

| Phase | Status | Complete |
|---|---|---|
| Sept 17 — foundation + both modules' backend + basic UI | Done | 100% |
| Sept 18 — Stripe, usage gating, UI polish, CI | Done (CI not yet executed on GitHub) | 97% |
| Sept 19 — end-to-end local run, review, acceptance criteria | Local E2E done; Stripe charge + review pending | 35% |
| Later — deployment | Deferred by design | 0% |

## Detailed task table

| # | Task | Area | Status | % | Notes / evidence |
|---|---|---|---|---|---|
| 1 | Project scaffold (Maven, Spring Boot 3.5.16, Java 25, docker-compose Postgres, H2 test profile) | Backend | Done | 100% | Commit `e7bd5a2`, `2687a49`, `298c517`. `mvn test` runs offline on H2 in PostgreSQL mode. |
| 2 | Postgres schema (7 tables from SPEC) via Flyway + JPA entities validated on startup | Backend | Done | 100% | `V1__schema.sql`; `SchemaMigrationTest`. JSON columns are large VARCHAR for H2/PG portability. |
| 3 | JWT auth: register / login / me, stateless bearer filter, BCrypt | Backend | Done | 100% | `AuthControllerTest` (5), `JwtServiceTest` (4). All `/api/**` except auth + webhook require a token. |
| 4 | `CoachingEngine` interface + request/response records | Backend | Done | 100% | `coaching/` package. |
| 5 | `TemplateCoachingEngine` (curated hints + rubric, offline, deterministic) | Backend | Done | 100% | `coaching/hints.json` (15 problems × 3 depths), `coaching/design.json` (4 questions × 4 stages). Test asserts hints never contain code. |
| 6 | Config-driven engine swap (`coaching.engine=template\|claude\|openai`) proven with two implementations | Backend | Done | 100% | `CoachingEngineSwapTemplateTest` + `CoachingEngineSwapEchoTest` flip the property and get a different engine. Satisfies the acceptance criterion. |
| 7 | DSA starter bank (~10–15 problems, arrays + linked lists, all four tiers) | Backend | Done | 100% | 15 problems seeded in `V2__seed_problems.sql`. |
| 8 | DSA hint flow: graduated hints, depth tracked, refuses hints after solve | Backend | Done | 100% | `DsaServiceTest`, `DsaControllerTest`. |
| 9 | Difficulty auto-escalation (3 consecutive solves at a tier with ≤1 hint → next tier, per category) | Backend | Done | 100% | Tested incl. heavy-hint blocking and FAANG_BAR ceiling. |
| 10 | Daily solve streak | Backend | Done | 100% | Fixed-clock tests for today/yesterday/gap cases. |
| 11 | System design starter bank (4 questions, seniority-tagged) | Backend | Done | 100% | `V3__seed_design_questions.sql`: URL shortener (MID), rate limiter (SENIOR), chat (SENIOR), news feed (STAFF). |
| 12 | Staged design flow (intro → requirements → components → data model → scaling) with per-stage challenge | Backend | Done | 100% | `DesignServiceTest`, `DesignControllerTest`. Transcript stored as provider-agnostic JSON. |
| 13 | Rubric grading (scalability, data modeling, trade-offs, communication) + narrative | Backend | Done | 100% | Aggregation unit-tested; overall 0–100 stored on the session. |
| 14 | Usage counters (per user per day) | Backend | Done | 100% | Incremented on first touch of a problem and on session start. |
| 15 | Basic Angular 19 UI: login, dashboard, DSA list/problem, design list/session | Frontend | Done | 100% | Commit `3e6be96`. Standalone components + signals, dev proxy to `:8080`. |
| 16 | Stripe Checkout (test mode) + Customer Portal | Backend | Done (code) | 90% | Behind `BillingGateway`; fake in tests. **Not exercised against a real Stripe test account — no keys in this environment.** |
| 17 | Stripe webhooks: signature verification, checkout/subscription/invoice events → DB | Backend | Done | 100% | `BillingServiceTest` signs payloads the way Stripe does; replay window, bad-signature, unknown-customer cases covered. |
| 18 | Usage-gating middleware (free: 3 DSA/day, 1 design/week; paid+active unlimited; 402 on limit) | Backend | Done | 100% | `UsageGateTest`; HTTP 402 asserted in `BillingControllerTest`. |
| 19 | Live-charge safety: refuse live key, ignore `livemode` events unless explicitly allowed | Backend | Done | 100% | Tested. Part of the "nothing can misfire into a live charge" criterion. |
| 20 | Account/Billing screen (plan, usage vs limits, upgrade/portal) + 402 upgrade prompts | Frontend | Done | 100% | `BillingComponent` + spec; prompts on DSA problem and design list pages. |
| 21 | UI polish pass | Frontend | Done | 100% | Loading states on every page, toast notifications, inline form validation, tier/level colour pills, lock badges, stage scores on the interview tracker, sticky nav, focus states, dark mode, mobile table scroll. |
| 22 | CI pipeline (backend tests, frontend tests + build, secrets grep) | DevOps | Done (not yet run) | 90% | `.github/workflows/ci.yml`. **Repo has no remote yet, so it has never executed on GitHub.** |
| 23 | Run everything end-to-end locally (`docker compose up`, backend, frontend) | Verification | Done | 100% | 2026-09-17: Postgres 16 in Docker, 3 Flyway migrations applied, backend + Angular dev server up; 18/19 scripted journey checks passed via the `:4200` proxy (register → hints → solve → 402 gate → 4-stage design session → rubric 85/100). The one miss was the webhook signature check, which answers 503 by design when no webhook secret is configured. UI verified served/compiled, not yet clicked through in a browser. |
| 24 | Stripe test-mode charge verified end-to-end (Checkout → webhook → tier upgrade → gating lifted) | Verification | Pending | 0% | Needs `STRIPE_SECRET_KEY`, `STRIPE_PRICE_ID`, `STRIPE_WEBHOOK_SECRET` and `stripe listen`. |
| 25 | KayKay manual run-through of both modules | Review | Pending | 0% | Non-negotiable human step per SPEC. |
| 26 | Acceptance checklist sign-off (CI green, security check, engine swap proven) | Review | Partial | 50% | Engine swap ✔, no-secrets grep ✔ (local), auth guards ✔ (tests). CI-green and manual review outstanding. |
| 27 | Hosting (Railway/Render), managed Postgres, deploy step, live Stripe keys | Deployment | Deferred | 0% | Explicitly out of scope until after review. |
| 28 | `ClaudeCoachingEngine` / `OpenAiCoachingEngine` live-model implementations | Future | Not started | 0% | Only the seam exists; add a `@ConditionalOnProperty` bean per provider. |
| 29 | Paid-only content: HARD/FAANG_BAR problems, STAFF design questions locked for free users | Backend + UI | Done | 100% | `PaidFeaturesTest`; `locked` flag in list DTOs; UI shows 🔒 Pro badges and an Unlock link. Configurable via `FREE_MAX_TIER`, `FREE_MAX_SENIORITY`. |
| 30 | Paid-only full history; free users see a rolling 7-day window | Backend + UI | Done | 100% | Dashboard `recent` and design session list are windowed; counts stay accurate. `FREE_HISTORY_DAYS`. |

## Completed — what exists and how to check it

- **Run tests**: `cd backend && mvn test` (61 tests, no DB needed) · `cd frontend && npm test -- --watch=false --browsers=ChromeHeadless` (5 specs).
- **API surface**: `/api/auth/*`, `/api/me`, `/api/dsa/problems[/{id}/attempts|hints|solve]`, `/api/dsa/progress`,
  `/api/design/questions`, `/api/design/sessions[/{id}/answers]`, `/api/billing/status|checkout|portal|webhook`.
- **Config knobs**: `COACHING_ENGINE`, `JWT_SECRET`, `DATABASE_*`, `STRIPE_*`, `FREE_DSA_PER_DAY`, `FREE_DESIGN_PER_WEEK`.
- **Docs**: `README.md` (run instructions, Stripe setup, CI), `SPEC.md` (original build spec).

## Pending — what is needed to close each item

| Item | Blocker / input needed | Owner | Effort |
|---|---|---|---|
| 23 Browser click-through | Servers already run cleanly; open http://localhost:4200, register, try both modules and the billing page | KayKay | ~20 min |
| 24 Stripe test charge | Stripe test keys exported; a recurring test price; `stripe listen --forward-to localhost:8080/api/billing/webhook`; pay with card 4242 4242 4242 4242 | KayKay for keys, agent can drive | ~1 h |
| 22 CI actually green | `git remote add` + push to GitHub; fix anything environment-specific (Chrome headless on Ubuntu, Maven download time) | KayKay to create/push repo | ~30 min |
| 25/26 Review + sign-off | Manual pass against SPEC "Acceptance Criteria"; anything unstable → backlog | KayKay | ~1 h |
| 27 Deployment | Hosting account + managed Postgres; then Dockerfile, deploy job, live keys (last) | Later | — |

## Known limitations / backlog

- No Java 21 on the build machine; project targets Java 25 (Boot 3.5.16 / Framework 6.2.19 officially support it).
- Angular CLI warns Node 24 is unsupported; CI pins Node 22.
- `transcript` / `rubric_breakdown` are VARCHAR-JSON, not `jsonb` (portability with H2). Vendor-specific migration can change this later.
- Template grading is keyword-coverage based; it is deterministic and honest about that, but a live engine will grade more nuanced answers better.
- Weekly design limit uses a rolling 7-day window across `usage_counters`, not calendar weeks.
- Root `settings.json` is untracked and gitignored (looks intended for `.claude/settings.json`).
