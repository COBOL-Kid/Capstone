# Honest Car E2E Regression Suite

Playwright browser tests for the Honest Car SPA against the local Angular dev server and Spring Boot backend. Scenario IDs map to `PLAYWRIGHT_REGRESSION_TEST_SCENARIOS.md` at the repo root.

## Prerequisites

1. **PostgreSQL** with the `honestcar` database and V1 schema applied (via Flyway on first `bootRun`).
2. **Seed data** (required before every full run; re-run after unverified verification tests mutate seed state):

   ```bash
   psql -U honestcar -d honestcar -f e2e/scripts/seed-e2e-data.sql
   ```

   | User                          | Password       | Purpose                                     |
   | ----------------------------- | -------------- | ------------------------------------------- |
   | `test.user@example.com`       | `Password123!` | Verified user with Camry + Civic            |
   | `empty.garage@example.com`    | `Password123!` | Verified user, empty garage                 |
   | `unverified.user@example.com` | `Password123!` | Unverified user; verification code `123456` |
   | `password.change@example.com` | `Password123!` | Reserved for destructive account tests      |

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

Tests always run with **one worker** (`--workers=1`). Authenticated specs log in as shared seed users (`test.user@example.com`, etc.); password login revokes that user's refresh tokens, so parallel browser contexts race and flake (garage never loads, add-vehicle button timeouts). Do not raise worker count without giving each worker its own seed user.

Other commands:

| Script             | Purpose                          |
| ------------------ | -------------------------------- |
| `pnpm test:ui`     | Interactive Playwright UI mode   |
| `pnpm test:headed` | Run with visible browser         |
| `pnpm test:debug`  | Step-through debugger            |
| `pnpm report`      | Open the HTML report after a run |

## Project layout

| Path                                | Purpose                                                                      |
| ----------------------------------- | ---------------------------------------------------------------------------- |
| `fixtures/`                         | Test data, per-test API auth, UI helpers, DB helpers                         |
| `fixtures/authenticated.fixture.ts` | Logs in via API before each authenticated test (avoids stale refresh tokens) |
| `support/global-setup.ts`           | Health checks for `:8080`/`:4200` and re-seeds Postgres                      |
| `scripts/seed-e2e-data.sql`         | Idempotent Postgres seed for all E2E users                                   |
| `tests/public/`                     | Public pages, CSRF bootstrap, signed-out account drawer                      |
| `tests/auth/`                       | Login, logout, session, auth guards                                          |
| `tests/garage/`                     | Garage/home flows (verified, empty, unverified)                              |
| `tests/vehicle-detail/`             | Vehicle detail page and modals                                               |
| `tests/vehicle-onboarding/`         | Add-vehicle flows (seed + route mocks)                                       |
| `tests/maintenance/`                | Maintenance complete/incomplete flows                                        |
| `tests/recalls/`                    | Recall complete/incomplete flows                                             |
| `tests/account/`                    | Account drawer and change modals                                             |
| `tests/errors/`                     | Session resilience and error handling                                        |

## Playwright projects

| Project                  | Auth                                         | Scope                                                 |
| ------------------------ | -------------------------------------------- | ----------------------------------------------------- |
| `chromium`               | None                                         | Public pages, sign-in/sign-up, auth guard             |
| `chromium-authenticated` | Per-test API login (`authenticated.fixture`) | Garage, vehicle detail, maintenance, recalls, account |
| `chromium-empty-garage`  | Per-test API login as empty-garage user      | Empty garage, add/delete vehicle, VEH-003 (fixme)     |
| `chromium-unverified`    | Per-test refresh-token login + seed reset    | Verification banner (serial)                          |

Global setup re-runs the seed script before each test run so `AUTH-012` and maintenance/recall serial suites start from a known DB state.

## Auth model

- Bootstrap issues `XSRF-TOKEN`; mutating API calls require `X-XSRF-TOKEN`.
- Sessions use the HttpOnly `__session` cookie.
- Authenticated tests log in via API in a fresh browser context before each test (Playwright `storageState` files were removed because Angular bootstrap calls `/api/auth/refresh`, which rotates refresh tokens and left later tests unauthenticated).
- The suite runs serially (`workers: 1`) so two tests never authenticate as the same seed user at the same time.
- Unverified tests use a seeded refresh token (`e2e-unverified-refresh-token`) because password login requires Mailjet for email verification.

## Coverage notes

- **P0/P1** scenarios from the regression doc are implemented with UI assertions; provider-dependent onboarding uses seeded VINs or Playwright route mocks.
- **P2** (rate limits, responsive/a11y smoke, live providers) are not in the default suite.
- **Fixme (app bug):** `VEH-003` and `VEH-004` — vehicle detail page stays on the loading skeleton after API 404/500 even though error state is set in the component.
- Re-run the seed script manually if you run tests without global setup or need to reset after local DB experiments.

## Troubleshooting

- **Global setup error:** Start both Server and Client before running tests.
- **Login or garage failures:** Re-run the seed script.
- **Unverified failures after a prior run:** Re-run the seed script to reset `unverified.user@example.com`.
- **Vehicle onboarding against local Postgres:** JDBC URL needs `?stringtype=unspecified` on Postgres.
