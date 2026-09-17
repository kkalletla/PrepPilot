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
