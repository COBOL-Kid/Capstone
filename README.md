# Honest Car

A web app to help auto owners stay ahead of maintenance, recalls, and warranty coverage.

**Live site:** [honest-car.co](https://honest-car.co)

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
| Production hosting | Firebase Hosting → Cloud Run (`honest-car-server`, IBM Semeru Runtime 25 JVM) |
| Email | Mailjet (verification codes) |
| Vehicle data | Auto.dev, Vehicle Databases |
