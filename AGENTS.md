## Learned User Preferences

- Prefer HttpOnly cookie-based access tokens with Spring CSRF over memory-only tokens with Bearer headers.
- Use Gradle and pnpm scripts for dependency scanning; do not add GitHub Actions or GitLab CI unless asked.
- Keep `security.cookies.secure=true` in base config; override to `false` only in the `dev` profile (and test config).
- Load local `.env` via `spring.config.import` in `application-dev.properties` only, not in base config (production runs on Cloud Run).
- Consolidate Flyway migrations into V1 when the schema has not shipped to production yet.
- Do not edit attached plan files when implementing a plan; follow the plan and update code only.
- Prefer GCP-native observability (Cloud Logging, Error Reporting, trace correlation); no third-party APM or Sentry unless asked.
- Angular backend mutations must use HttpClient (not `fetch`) so XSRF and credentials interceptors apply.

## Learned Workspace Facts

- Monorepo layout: `Client/` (Angular frontend) and `Server/` (Spring Boot/Java backend).
- Production deployment target is Google Cloud Run; secrets and config come from service env vars, not a local `.env` file.
- Auth uses HttpOnly cookies for access and refresh tokens with CSRF protection; JWTs are not stored in `localStorage`.
- Login rate limit is 5 attempts per IP per 15 minutes (`security.login.max-attempts-per-ip`, `security.login.rate-limit-window-minutes`).
- Database schema is managed by a single Flyway migration `V1__Initial_schema.sql` (V2 was merged before production).
- Local dev uses the `dev` Spring profile (`SPRING_PROFILES_ACTIVE=dev`) to load `.env` and disable secure cookies.
- No CI pipeline config exists in the repo today.
- Public support contact email is `support@honest-car.co`.
- Run Spotless after Java changes and Prettier after TypeScript changes.
- Prod profile (`SPRING_PROFILES_ACTIVE=prod`): structured JSON stdout logging (`logging.structured.format.console=logstash`), `server.forward-headers-strategy=framework`, and `GCP_PROJECT_ID` for Cloud Logging trace correlation.
- Local dev: Angular calls `http://localhost:8080` directly when the hostname is `localhost` (`Client/src/app/core/api/api.config.ts`); there is no `proxy.conf.json` in the repo. CORS is configured for `http://localhost:4200`.
- Local JVM dev uses any installed JDK 25 toolchain; GraalVM native images are built only via `Server/Dockerfile` (`docker build`), not `./gradlew nativeCompile` on the host.

## Cursor Cloud specific instructions

### One-time VM prerequisites (not in the update script)

- **JDK 25**: local dev and tests use any JDK 25 (Gradle toolchain in `Server/build.gradle.kts`). Native image builds use GraalVM inside `Server/Dockerfile`; no host GraalVM install required.
- **MySQL 8**: local dev uses Flyway on boot against `DB_URL` from `/.env`. Example: database `honestcar`, user `honestcar` / password `honestcar_dev`.
- **Repo-root `/.env`**: gitignored; required for `SPRING_PROFILES_ACTIVE=dev` (`application-dev.properties` imports `../.env`). Copy variable names from `Server/src/main/resources/application.properties`. Set `MAILJET_ENABLED=false` when Mailjet keys are unavailable (signup returns 503 until email is configured).
- **pnpm 11**: `packageManager` is `pnpm@11.3.0`; activate via `corepack prepare pnpm@11.3.0 --activate`.

### Running services

| Service | Command | Port |
|---------|---------|------|
| Backend (JVM dev) | `cd Server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` | 8080 |
| Backend (native image) | `cd Server && docker build -t honest-car-server .` then `docker run ... honest-car-server` | 8080 |
| Frontend | `cd Client && pnpm start --host 0.0.0.0` | 4200 |
| Health | `curl http://localhost:8080/actuator/health` | — |

Use tmux for long-running dev servers. Do **not** pass `pnpm start -- --host` (double `--` breaks `ng serve`).

### Lint / test / build

| Area | Lint | Test | Build |
|------|------|------|-------|
| Client | `pnpm exec prettier --check .` | `pnpm test` | `pnpm build` |
| Server (JVM) | `./gradlew spotlessCheck` | `./gradlew test` | `./gradlew build` |
| Server (native) | `./gradlew spotlessCheck` | `./gradlew test` | `docker build -t honest-car-server .` (from `Server/`) |

Production backend images are built from `Server/Dockerfile`, which compiles a GraalVM native executable and packages it in a `debian:bookworm-slim` container for Cloud Run. `nativeCompile` is gated behind `NATIVE_IMAGE_BUILD=true` (set in the Dockerfile). Native AOT uses the `prod` profile by default. Conditional beans (for example Mailjet) are fixed at AOT build time via the placeholder env vars in `Server/build.gradle.kts`.

Server tests use in-memory H2 (no MySQL). One integration test (`VehicleOnboardingProviderBurstIntegrationTest`) can be timing-sensitive on slow VMs. Client `local-date.spec.ts` asserts UTC vs local calendar dates and may fail when the VM timezone is UTC.

### External APIs (optional for browse-only dev)

Full vehicle and email flows need Auto.dev, Vehicle Databases, and Mailjet keys in `/.env`. Browse-only (landing, services, auth UI) works with placeholder provider keys and `MAILJET_ENABLED=false`.
