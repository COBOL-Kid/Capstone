# Honest Car E2E Regression Suite

Playwright browser tests for the Honest Car SPA against the local Angular dev server and Spring Boot backend.

## Prerequisites

1. **PostgreSQL** with the `honestcar` database and V1 schema applied (via Flyway on first `bootRun`).
2. **Seed data** for garage tests:

   ```bash
   psql -U honestcar -d honestcar -f e2e/scripts/seed-e2e-data.sql
   ```

   Test user: `test.user@example.com` / `Password123!`

3. **Backend** (port 8080):

   ```bash
   cd Server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
   ```

4. **Frontend** (port 4200, proxies `/api/**` to the backend):

   ```bash
   cd Client && pnpm start
   ```

## Setup

```bash
cd e2e
pnpm install
pnpm run install:browsers
```

## Run tests

```bash
cd e2e
pnpm test
```

Other commands:

| Script             | Purpose                          |
| ------------------ | -------------------------------- |
| `pnpm test:ui`     | Interactive Playwright UI mode   |
| `pnpm test:headed` | Run with visible browser         |
| `pnpm test:debug`  | Step-through debugger            |
| `pnpm report`      | Open the HTML report after a run |

## Project layout

| Path                        | Purpose                                              |
| --------------------------- | ---------------------------------------------------- |
| `fixtures/`                 | Shared test data and API auth helpers                |
| `support/global-setup.ts`   | Fail-fast health checks for `:8080` and `:4200`      |
| `scripts/seed-e2e-data.sql` | Idempotent Postgres seed for demo user and vehicles  |
| `tests/smoke/`              | Unauthenticated smoke tests                          |
| `tests/auth/`               | Login flow and auth setup (`storageState`)           |
| `tests/garage/`             | Authenticated garage tests (reuse `.auth/user.json`) |

## Auth model

- Bootstrap issues `XSRF-TOKEN`; mutating API calls require `X-XSRF-TOKEN`.
- Sessions use the HttpOnly `__session` cookie.
- `tests/auth/auth.setup.ts` logs in via API (mirrors `SessionCookieIntegrationTest`) and writes `.auth/user.json` for garage specs.

## Troubleshooting

- **Global setup error:** Start both Server and Client before running tests.
- **Login or garage failures:** Re-run the seed script; the test user may be missing.
- **Vehicle onboarding tests (future):** Local JDBC URL needs `?stringtype=unspecified` on Postgres.
