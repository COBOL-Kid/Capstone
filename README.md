# Honest Car

A web app to help auto owners stay ahead of maintenance, recalls, and warranty coverage.

> **Service status: retired.** [honest-car.co](https://honest-car.co) is no longer live. Running the app depends on paid vehicle-data and email APIs, and that cost was no longer sustainable, so the hosted deployment has been shut down. The code remains here as a portfolio and reference project; see the local development notes below to run it yourself.

This project was created during my cohort’s “Web App in a Week” competition, a fast-paced challenge to design, build, and deliver a working app under tight time constraints. I was proud to be one of two winners of the competition.

Since then, I’ve reworked the codebase as I’ve grown as a developer. My professional background is in mainframe development, and while the stacks are different, a lot of the fundamentals carried over.

## Features

- **Maintenance planning** Upcoming services by mileage with parts and labor estimates; log completed work.
- **Recall tracking** Open safety recalls for your vehicle; mark them resolved when addressed.
- **Warranty clarity** Coverage types with estimated expiration dates.
- **Account management** Email verification, secure sessions, and vehicle onboarding by VIN.

## Tech stack

| Layer | Stack |
|-------|-------|
| Frontend | Angular 22 SPA (`Client/`) |
| Backend | Spring Boot 4.1 / Java 25 (`Server/`) |
| Database | PostgreSQL 16+ (Flyway migrations) |
| Production hosting | Firebase Hosting → Cloud Run (`honest-car-server`, IBM Semeru Runtime 25 JVM) — previously deployed; now shut down |
| Email | Mailjet (verification codes) |
| Vehicle data | Auto.dev, Vehicle Databases |

## Running locally

Backend and frontend run as two local processes; see `AGENTS.md` for full setup details. At a minimum you'll need the JDK 25 toolchain, Node 24 with pnpm, and a PostgreSQL 16+ database.

**Third-party keys are required for the full experience.** The vehicle-data providers (Auto.dev, Vehicle Databases) and Mailjet are paid services. With placeholder keys and `MAILJET_ENABLED=false` the app still runs in browse-only mode, but VIN onboarding, maintenance/recall/warranty data, and email verification will not work.

