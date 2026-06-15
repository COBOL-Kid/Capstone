# Honest Car

A web app to help auto owners stay ahead of maintenance, recalls, and warranty coverage — all in one dashboard.

**Live site:** [honest-car.co](https://honest-car.co)

This project was created during my cohort’s “Web App in a Week” competition, a fast-paced challenge to design, build, and deliver a working app under tight time constraints. I was proud to be one of two winners of the competition.

Since then, I’ve reworked the codebase as I’ve grown as a developer. My professional background is in mainframe development, and while the stacks are different, a lot of the fundamentals—structured problem solving, attention to data integrity, and building reliable systems—carried over.

## Features

- **Maintenance planning** — upcoming services by mileage with parts and labor estimates; log completed work.
- **Recall tracking** — open safety recalls for your vehicle; mark them resolved when addressed.
- **Warranty clarity** — coverage types with estimated expiration dates.
- **Account management** — email verification, secure sessions, and vehicle onboarding by VIN.

## Tech stack

| Layer | Stack |
|-------|-------|
| Frontend | Angular 22 SPA (`Client/`) |
| Backend | Spring Boot 4.1 / Java 25 (`Server/`) |
| Database | PostgreSQL 16+ (Flyway migrations) |
| Production hosting | Firebase Hosting → Cloud Run (`honest-car-server`, GraalVM native image) |
| Email | Mailjet (verification codes) |
| Vehicle data | Auto.dev, Vehicle Databases |

Authentication uses HttpOnly cookie-based JWTs with Spring Security CSRF (SPA mode). The Angular dev server and Firebase Hosting both proxy `/api/**` to the backend so the browser stays same-origin.

## Repository layout

```
Capstone/
├── Client/          # Angular frontend
├── Server/          # Spring Boot backend
├── firebase.json    # Hosting rewrites (/api → Cloud Run)
└── .github/         # Firebase Hosting CI (frontend only)
```

## Local development

### Prerequisites

- **JDK 25**
- **Node.js 24** and **pnpm 11** (`corepack prepare pnpm@11.6.0 --activate`)
- **PostgreSQL 16+** (local install, Docker, or Supabase)
- A repo-root `.env` file — copy variable names from `Server/src/main/resources/application.properties`

Set `MAILJET_ENABLED=false` if you don’t have Mailjet keys. Browse-only development works with placeholder vehicle-provider keys; full vehicle lookup and email flows need Auto.dev, Vehicle Databases, and Mailjet credentials.

Example database URL: `jdbc:postgresql://localhost:5432/honestcar`

### Run services

| Service | Command | Port |
|---------|---------|------|
| Backend (JVM) | `cd Server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` | 8080 |
| Backend (native image) | `cd Server && docker build -t honest-car-server .` then `docker run -p 8080:8080 honest-car-server` | 8080 |
| Frontend | `cd Client && pnpm start` | 4200 |
| Health check | `curl http://localhost:8080/actuator/health` | — |

Run the backend and frontend in separate terminals. With `SPRING_PROFILES_ACTIVE=dev`, Spring loads `.env` from the repo root (or `Server/.env`), sets `security.cookies.secure=false`, and defaults `app.public-url` to `http://localhost:4200`. The Angular dev server proxies `/api/**` to `http://localhost:8080`.

**Note:** The first login POST may fail once if the `XSRF-TOKEN` cookie has not been issued yet; a retry succeeds.

## Testing and formatting

| Area | Format | Test | Build |
|------|--------|------|-------|
| Client | `pnpm exec prettier --write .` | `pnpm test` | `pnpm build` |
| Server | `./gradlew spotlessApply` | `./gradlew test` | `./gradlew build` |

Dependency scanning: `cd Server && ./gradlew dependencyCheck` (OWASP); `cd Client && pnpm audit`.

Optional live API smoke tests: `cd Server && ./gradlew liveTest` (requires real provider and Mailjet env vars).

## Deployment

- **Frontend:** Firebase Hosting serves `Client/dist/Client/browser` and rewrites `/api/**` to Cloud Run. Deploy manually with `pnpm --dir Client deploy:hosting`. Pushes to `main` also deploy via GitHub Actions.
- **Backend:** GraalVM native image built from `Server/Dockerfile`, deployed to Cloud Run (`honest-car-server`, `us-central1`). Secrets and config come from GCP Secret Manager at deploy time—not from a local `.env`.

Production URL defaults to `https://honest-car.co`.

## Support

Questions or feedback: [support@honest-car.co](mailto:support@honest-car.co)
