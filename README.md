# HonestCar

A web app to keep auto owners informed.

This project was created during my cohort’s “Web App in a Week” competition, a fast-paced challenge to design, build, and deliver a working app under tight time constraints. I was proud to be one of two winners of the competition.

Since then, I’ve reworked the codebase as I’ve grown as a developer. My professional background is in mainframe development, and while the stacks are different, a lot of the fundamentals—structured problem solving, attention to data integrity, and building reliable systems—carried over.

## Local development

**Prerequisites:** Node.js 24 LTS (`>=24.15.0`; use the repo-root `.nvmrc`), JDK 25, PostgreSQL 16+, pnpm 11 (`corepack prepare pnpm@11.3.0 --activate`), and a repo-root `.env` file (see `Server/src/main/resources/application.properties` for variable names). Set `MAILJET_ENABLED=false` if you don’t have Mailjet keys.

| Service | Command | Port |
|---------|---------|------|
| Backend | `cd Server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` | 8080 |
| Frontend | `cd Client && pnpm start` | 4200 |
| Health check | `curl http://localhost:8080/actuator/health` | — |

Run the backend and frontend in separate terminals. The Angular dev server proxies `/api/**` to `http://localhost:8080`.
