## Learned User Preferences

- Prefer HttpOnly cookie-based access tokens with Spring CSRF over memory-only tokens with Bearer headers.
- Use Gradle and pnpm scripts for dependency scanning; do not add GitHub Actions or GitLab CI unless asked.
- Keep `security.cookies.secure=true` in base config; override to `false` only in the `dev` profile (and test config).
- Load local `.env` via `spring.config.import` in `application-dev.properties` only, not in base config (production runs on Cloud Run).
- Consolidate Flyway migrations into V1 when the schema has not shipped to production yet.
- Do not edit attached plan files when implementing a plan; follow the plan and update code only.
- Prefer GCP-native observability (Cloud Logging, Error Reporting, trace correlation); no third-party APM or Sentry unless asked.
- Angular backend mutations must use HttpClient (not `fetch`) so XSRF and credentials interceptors apply.
- Spring Data JPA `*RepositoryJPA` interfaces must omit `@Repository`; `JpaRepository` extensions are auto-registered during repository scanning.
- Production backend uses IBM Semeru Runtime 25 JVM on ICR UBI minimal images (`icr.io/appcafe/ibm-semeru-runtimes`), not GraalVM native image.

## Repository Overview

- **Layout:** `Client/` (Angular 22 SPA) and `Server/` (Spring Boot 4.1.0 / Java 25 backend).
- **JSON:** Server uses Jackson 3 (`tools.jackson` / `JsonMapper`). JWTs use JJWT via `jjwt-gson` (not `jjwt-jackson`).
- **Git:** `origin` is GitHub only (`https://github.com/COBOL-Kid/Capstone.git`; GitLab remote removed).
- **Support contact:** `support@honest-car.co` (see `Client/src/app/pages/about-page/about-page.html`).

## Authentication & Security

- Access and refresh tokens are HttpOnly cookies (`accessToken`, `refreshToken`); JWTs are not stored in `localStorage`.
- Spring Security uses CSRF SPA mode (`SecurityConfig`); Angular sends the `XSRF-TOKEN` cookie on mutating requests.
- SPA bootstrap on load: `AppBootstrapService` (`provideAppInitializer` in `app.config.ts`) calls `GET /api/auth/csrf`, then `validateSession()`, before the app renders—issues the CSRF cookie and hydrates navbar auth state on cold visits.
- Bootstrap loading splash: branded static HTML inside `<app-root>` in `Client/src/index.html` with `Client/public/bootstrap-splash.css` linked in `<head>` (not bundled `styles.css`) so first paint shows a loading screen during JS download and app-initializer HTTP calls; Angular replaces it when the root `App` component renders.
- Login rate limit: 5 attempts per IP per 15 minutes (`security.login.max-attempts-per-ip`, `security.login.rate-limit-window-minutes`).
- No server-side CORS configuration (same-origin in prod via Firebase Hosting rewrites; dev proxy in Angular).

## Database

- **Production/dev:** PostgreSQL. Schema is a single Flyway migration: `Server/src/main/resources/db/migration/V1__Initial_schema.sql` (V2 was merged before production; schema only, no seed data).
- **Tests:** In-memory H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`); Flyway disabled in default test config; Hibernate `PostgreSQLDialect`. Seeded Flyway tests use a separate copy at `Server/src/test/resources/integration-test-db/migration/V1__Initial_schema.sql`.
- **Primary user table:** `user_detail` (not `users`).

## Local Development

- **Spring profile:** `SPRING_PROFILES_ACTIVE=dev` loads `.env`, sets `security.cookies.secure=false`, and defaults `app.public-url` to `http://localhost:4200`.
- **`.env` import:** `application-dev.properties` imports `optional:file:../.env[.properties]` (repo root) and `optional:file:.env[.properties]` (`Server/.env`). Copy variable names from `Server/src/main/resources/application.properties`. Set `MAILJET_ENABLED=false` when Mailjet keys are unavailable.
- **Frontend API URLs:** Same-origin via `Client/src/app/core/api/api.config.ts` (`resolveBackendOrigin` returns `window.location.origin`).
- **Dev proxy:** `Client/proxy.conf.json` forwards `/api/**` to `http://localhost:8080`.

| Service | Command | Port |
|---------|---------|------|
| Backend (JVM) | `cd Server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` | 8080 |
| Backend (container) | `cd Server && docker build -t honest-car-server .` then `docker run -p 8080:8080 honest-car-server` | 8080 |
| Frontend | `cd Client && pnpm start` (use `pnpm start --host 0.0.0.0` on Cloud Agent VMs) | 4200 |
| Health | `curl http://localhost:8080/actuator/health` | — |

Run backend and frontend in separate terminals (tmux on Cloud Agent VMs). Do **not** pass `pnpm start -- --host` (double `--` breaks `ng serve`).

**Browse-only dev** works with placeholder vehicle-provider keys and `MAILJET_ENABLED=false`. Full vehicle and email flows need Auto.dev, Vehicle Databases, and Mailjet keys in `.env`.

## Testing, Formatting & Dependency Scanning

| Area | Format (after edits) | Lint check | Test | Build |
|------|----------------------|------------|------|-------|
| Client | `pnpm exec prettier --write .` | `pnpm exec prettier --check .` | `pnpm test` (Vitest via `@angular/build:unit-test`) | `pnpm build` |
| Server | `./gradlew spotlessApply` | `./gradlew spotlessCheck` | `./gradlew test` | `./gradlew build` or `docker build -t honest-car-server .` (from `Server/`) |

**Targeted Server regression suites** (auth/repository work):

```bash
cd Server && ./gradlew test \
  --tests "com.capstone.data.*" \
  --tests "com.capstone.authentication.*" \
  --tests "com.capstone.domain.UserDeletionIntegrationTest"
```

**Targeted Client auth/interceptor tests** (`ng test` does not accept bare filenames after `--`; use `--include`):

```bash
cd Client && pnpm exec ng test --watch=false \
  --include='src/app/core/auth/unauthorized.interceptor.spec.ts' \
  --include='src/app/core/http/credentials.interceptor.spec.ts' \
  --include='src/app/core/auth/auth.service.spec.ts'
```

### Server testing conventions

- **Mocked service tests do not execute JPA queries.** `AuthenticationServiceTest`, `EmailVerificationServiceTest`, and similar tests mock `*RepositoryJPA` beans, so broken `@Query` / `@Modifying` JPQL (e.g. `SELECT` on a delete method, nullable scalar returned for primitive `boolean`) will not be caught. Add repository integration tests or service integration tests that use real repositories for any custom repository method.
- **Shared harness:** `Server/src/test/java/com/capstone/support/IntegrationTestProperties.java` exposes `h2CreateDrop(dbName)` (Hibernate `create-drop`, Flyway off) and `h2FlywaySeed(dbName)` (Flyway V1 seed data, Hibernate `ddl-auto=none`). `MailjetTestSupport.stubSuccessfulSend` stubs `@MockitoBean MailjetClient` for flows that send email.
- **`@DynamicPropertySource` required for harness properties.** `IntegrationTestProperties.h2CreateDrop(...)` cannot be passed to `@SpringBootTest(properties = …)` (annotation values must be compile-time constants). Register properties at runtime:

```java
@DynamicPropertySource
static void integrationTestProperties(DynamicPropertyRegistry registry) {
  for (String property : IntegrationTestProperties.h2CreateDrop("my-test-db")) {
    int separator = property.indexOf('=');
    registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
  }
}
```

- **Three H2 + Flyway setups:**
  - **Default test config** (`Server/src/test/resources/application.properties`): `spring.config.import=` (blocks dev `.env` import), `flyway.enabled=false`, Hibernate `create-drop`; used by `@SpringBootTest` tests without overrides.
  - **Flyway schema validation** (`MigrationValidationTest`): inline `@SpringBootTest(properties=…)` with `spring.flyway.enabled=true` and default `classpath:db/migration` (production `Server/src/main/resources/db/migration/V1__Initial_schema.sql`; schema only, no seed rows).
  - **Flyway seeded integration tests** via `IntegrationTestProperties.h2FlywaySeed(...)`: Flyway loads `classpath:integration-test-db/migration` (`Server/src/test/resources/integration-test-db/migration/V1__Initial_schema.sql`; duplicates production schema plus seed data). Used by auth/repository integration tests (`UserDeletionIntegrationTest`, `*RepositoryJPATest`, `AuthenticationControllerIntegrationTest`, `VehicleReadDaoTest`, `MaintenanceDashboardIntegrationTest`, etc.). Any test that queries seed users/VINs must use this harness (or set `spring.flyway.locations=classpath:integration-test-db/migration`).
  - When editing production `V1__Initial_schema.sql`, keep `integration-test-db/migration/V1__Initial_schema.sql` in sync (schema + seeds).
- **Test JWT secret must be valid Base64.** `JwtService` decodes `security.jwt.secret` with `Decoders.BASE64`. Test config uses `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=` (same key material as `JwtServiceTest`). Integration tests that issue real JWTs inherit this from `application.properties` or `IntegrationTestProperties.JWT_SECRET`.
- **Repository query guard:** `RepositoryQueryArchitectureTest` fails `./gradlew test` if a repository method combines `@Modifying` with a `SELECT` `@Query`, or returns primitive `boolean` from a nullable scalar `SELECT`. Add new `*RepositoryJPA` interfaces with custom `@Query` to that test's scan list.
- **Custom `@Query` on `*RepositoryJPA` can fail at runtime, not compile time.** Prefer Spring Data derived query method names (`existsBy…`, `deleteBy…`) when possible; when adding custom `@Query`, add a repository integration test and update `RepositoryQueryArchitectureTest`.
- **MockMvc (Spring Boot 4):** `@AutoConfigureMockMvc` lives in `org.springframework.boot.webmvc.test.autoconfigure`; dependency is `spring-boot-starter-webmvc-test`. CSRF is enforced on mutating auth endpoints—bootstrap `XSRF-TOKEN` via a GET (e.g. `/api/auth/csrf` or `/actuator/health`) before POSTing in MockMvc tests.
- **Transactional deletes in repository tests:** `@Modifying` / derived delete methods need an active transaction; annotate the test method with `@Transactional` when calling `deleteByUser`, `deleteOrphanedVins`, etc.

### Client testing conventions

- Auth specs live under `Client/src/app/core/auth/` and `Client/src/app/core/http/`. Interceptor specs (`unauthorized.interceptor.spec.ts`, `credentials.interceptor.spec.ts`) use `provideHttpClient(withInterceptors([...]))` + `HttpTestingController`, matching `auth.service.spec.ts`.
- Registration/login error handling is tested against plain-text error bodies returned by `GlobalExceptionHandler` (e.g. 500 → `"Sometimes things just don't go as planned."`).

- **Dependency scanning:** `cd Server && ./gradlew dependencyCheck` (OWASP); `cd Client && pnpm audit` or `pnpm audit:ci`.
- **Optional live API smoke tests:** `cd Server && ./gradlew liveTest` (tagged `live`; needs real provider/Mailjet env vars).
- **Gradle:** Uses `implementation(platform(SpringBootPlugin.BOM_COORDINATES))` instead of `io.spring.dependency-management`; `flyway-database-postgresql` pinned at `11.15.0`. JDK 25 toolchain. On Windows there is no `gradlew.bat`—invoke `./gradlew` via Git Bash `sh`. Dockerfile runs `sed -i 's/\r$//' gradlew` before invoking Gradle (Windows CRLF).
- **Known flaky tests:** `VehicleOnboardingProviderBurstIntegrationTest` (timing-sensitive on slow VMs). Client `local-date.spec.ts` may fail when the VM timezone is UTC.

## Deployment & CI

- **GCP project:** `honest-car-498923` (Firebase Hosting and Cloud Run share this project).
- **Backend (Cloud Run):** Service `honest-car-server`, region `us-central1`. Image `us-central1-docker.pkg.dev/honest-car-498923/cloud-run-source-deploy/honest-car-server`. Build from `Server/` with `docker build -t <image>:latest .`, push to Artifact Registry, then `gcloud run services update honest-car-server --region=us-central1 --image=<image>:latest` (Cloud Run may not pick up a repushed `:latest` tag automatically). Secrets and config come from Secret Manager / service env vars at deploy time—not baked into the image or a local `.env`.
- **JVM container:** `ENTRYPOINT java -jar /app/app.jar` on `icr.io/appcafe/ibm-semeru-runtimes:open-25-jre-ubi-minimal`; build stage uses `open-25-jdk-ubi-minimal` and `./gradlew bootJar`. Only `SPRING_PROFILES_ACTIVE=prod` is set in the Dockerfile—do not bake `ENV PORT=8080`; Cloud Run injects `PORT` at runtime and prod binds via `server.port=${PORT:8080}` (`EXPOSE 8080` is informational). UBI minimal images need `USER root` before `microdnf`; builder stage installs `findutils` (Gradle needs `xargs`). Apply Spring Boot BOM to `developmentOnly` so `bootJar` resolves devtools. Conditional beans (e.g. Mailjet) resolve at runtime from deploy-time env vars. Avoid BuildKit-only `RUN --mount=type=cache` in the Dockerfile (Google Cloud Build's default builder does not enable BuildKit).
- **Prod profile:** `server.port=${PORT:8080}`, `app.public-url` defaults to `https://honest-car.co` (`APP_PUBLIC_URL` override), structured JSON stdout logging (`logging.structured.format.console=logstash`), `server.forward-headers-strategy=framework`, `GCP_PROJECT_ID` for Cloud Logging trace correlation.
- **Frontend (Firebase Hosting):** `firebase.json` and `.firebaserc` live at the repo root; hosting serves `Client/dist/Client/browser` and rewrites `/api/**` to Cloud Run `honest-car-server` in `us-central1` (`run.serviceId` is the service name). Custom domain `honest-car.co` is configured in Firebase Console. Deploy: `pnpm --dir Client deploy:hosting` (builds Client, then runs Firebase CLI from repo root).
- **CI (frontend only; no Server workflow):** `firebase-hosting-pull-request.yml` (PR preview channels) and `firebase-hosting-merge.yml` (push to `main` → live). Workflows run `pnpm install --frozen-lockfile` + `pnpm build` in `Client/`; `FirebaseExtended/action-hosting-deploy` deploys—do not run `deploy:hosting` in CI (double deploy). GitHub secret: `FIREBASE_SERVICE_ACCOUNT_HONEST_CAR_498923`. Server verification is local: `./gradlew test` (and `liveTest` when needed).

## Cursor Cloud Agent Setup

### One-time VM prerequisites

- **Node.js 24 LTS:** `nvm install 24 && nvm alias default 24`. Cloud Agent VMs ship `/exec-daemon/node` (v22) earlier on `PATH` than nvm; prepend Node 24 in `~/.bashrc`, e.g. `export PATH="$HOME/.nvm/versions/node/v24.16.0/bin:$PATH"`.
- **pnpm 11:** `Client/package.json` pins `pnpm@11.6.0` via `packageManager`; activate with `corepack prepare pnpm@11.6.0 --activate` (CI workflows currently pin `11.3.0` in `pnpm/action-setup`).
- **JDK 25:** Gradle toolchain in `Server/build.gradle.kts` (`vendor = IBM` for IBM Semeru). Production container images use ICR `ibm-semeru-runtimes` Open Edition tags.
- **PostgreSQL 16+:** Docker `postgres:16`, local install, or Supabase. Start local Postgres: `sudo pg_ctlcluster 16 main start` (create `honestcar` DB/user if needed). Example URL: `jdbc:postgresql://localhost:5432/honestcar`. Supabase transaction pooler: port `6543` with `prepareThreshold=0` and `sslmode=require`; direct `db.<project-ref>.supabase.co:5432` is IPv6-only and often fails locally.

### Cloud Agent gotchas

- **Injected `DB_*` secrets** override repo-root `.env` at runtime and may point at a shared Supabase Postgres (Postgres 17.x, catalog `postgres`). Override on the `bootRun` command line to use local Postgres, e.g. prepend `DB_URL='jdbc:postgresql://localhost:5432/honestcar?stringtype=unspecified' DB_USER=honestcar DB_PASS=honestcar`.
- **Local Postgres needs `?stringtype=unspecified` in the JDBC URL.** `VehicleWarranty.coverages` persists via a `String` `AttributeConverter` (`MapStringJsonConverter`) into a `json` column. Without `stringtype=unspecified`, pgjdbc binds the value as `varchar` and VIN onboarding (`POST /api/vin`) fails with **500** (`column "coverages" is of type json but expression is of type character varying`) after all external provider calls succeed. The Supabase pooler `DB_URL` does not set this, so always add it when overriding to local Postgres. Browse/login flows work without it; only vehicle onboarding triggers the warranty insert.
- **Flyway checksum mismatch** on a reused local DB: drop and recreate `honestcar` if V1 was applied from an older branch (`sudo -u postgres psql -c "DROP DATABASE honestcar;"`, then recreate with owner `honestcar`).
- **Signup requires email delivery:** `/api/auth/register` persists the user, then sends a verification code via Mailjet (`mailjet.enabled=true` creates the `MailjetClient` bean). The code is bcrypt-hashed in `email_verification_code` and cannot be recovered from the DB. With `MAILJET_ENABLED=false`, `MailjetEmailClient` is absent and register returns **503** (`EmailDeliveryException` → `"Unable to send verification email right now. Please try again later."`) after creating an unverified user. Without a reachable inbox, seed a verified user in `user_detail` (`email_verified=true`, `user_pw` = BCrypt hash) and log in via `/api/auth/authenticate`.
