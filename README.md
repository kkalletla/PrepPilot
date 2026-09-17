# PrepPilot — AI Interview Coach

Two modules: a **DSA Socratic Coach** (graduated hints, never full answers) and a
**System Design Mock Interview Simulator** (staged Q&A, rubric-graded). See `SPEC.md`.

## Backend (Java 25, Spring Boot 3.5, Postgres)

```bash
docker compose up -d postgres          # local Postgres on :5432
cd backend && mvn spring-boot:run      # API on :8080
mvn test                               # runs against in-memory H2 (PostgreSQL mode)
```

Environment variables (all have local defaults): `DATABASE_URL`, `DATABASE_USER`,
`DATABASE_PASSWORD`, `JWT_SECRET`, `COACHING_ENGINE` (`template` | `claude` | `openai`).

### Stripe (test mode)

Set `STRIPE_SECRET_KEY` (must be `sk_test_…`), `STRIPE_PRICE_ID` (a recurring price in your test
account) and `STRIPE_WEBHOOK_SECRET`. Without a secret key the billing endpoints answer 503 and
everything else works. The app refuses to boot with a live key unless `stripe.allow-live=true`,
and ignores `livemode: true` webhook events until then.

Forward webhooks locally with the Stripe CLI:

```bash
stripe listen --forward-to localhost:8080/api/billing/webhook   # prints the whsec_… to export
```

Free tier: 3 DSA problems/day, 1 design session per rolling 7 days (`FREE_DSA_PER_DAY`,
`FREE_DESIGN_PER_WEEK`). Paid + active = unlimited. Limits hit → HTTP 402 with an upgrade prompt.

## Frontend (Angular 19)

```bash
cd frontend && npm install
npm start                              # dev server on :4200, proxies /api to :8080
npm test -- --watch=false --browsers=ChromeHeadless
```

Screens: login/register, dashboard (streak, tier progress, quick-resume), DSA practice
(statement, notes area, graduated hint panel, difficulty filter), and the staged system-design
session with a rubric scorecard at the end, and Account/Billing (plan, usage vs limits, Stripe
Checkout and Customer Portal).

## CI

`.github/workflows/ci.yml` runs the backend suite (JDK 25), the frontend specs + production build
(Node 22, headless Chrome) and a secrets grep on every push and pull request.
