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

## Learned Workspace Facts

- Monorepo layout: `Client/` (Angular 22 frontend) and `Server/` (Spring Boot 4.1.0 / Java backend). Server JSON uses Jackson 3 (`tools.jackson` / `JsonMapper`); JJWT via `jjwt-gson` (not `jjwt-jackson`).
- Git `origin` is GitHub only (`https://github.com/COBOL-Kid/Capstone.git`; GitLab remote removed).
- Production deployment target is Google Cloud Run (GCP project `honest-car-498923`, region `us-central1`); server image `us-central1-docker.pkg.dev/honest-car-498923/cloud-run-source-deploy/honest-car-server`. Build from `Server/` with `docker build -t <image>:latest .`, push to Artifact Registry, then `gcloud run services update honest-car-server --region=us-central1 --image=<image>:latest` (Cloud Run does not always pick up a new `:latest` tag automatically). Secrets and config come from Secret Manager / service env vars at deploy time, not baked into the image or a local `.env` file. Native image container has no CLI args (`ENTRYPOINT ./capstone-server`); `SPRING_PROFILES_ACTIVE=prod` and `PORT=8080` are set in the Dockerfile—do not set `PORT` manually on Cloud Run when the container port is 8080.
- Auth uses HttpOnly cookies for access and refresh tokens with CSRF protection (JWTs not in `localStorage`); login rate limit is 5 attempts per IP per 15 minutes (`security.login.max-attempts-per-ip`, `security.login.rate-limit-window-minutes`).
- Database is PostgreSQL; schema is managed by a single Flyway migration `V1__Initial_schema.sql` (V2 was merged before production). Server tests use in-memory H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`) with Hibernate `PostgreSQLDialect`.
- Local dev uses the `dev` Spring profile (`SPRING_PROFILES_ACTIVE=dev`) to load `.env`, disable secure cookies, and default `app.public-url` to `http://localhost:4200`.
- Firebase Hosting CI (only GitHub Actions in repo): `firebase-hosting-pull-request.yml` (PR preview channels) and `firebase-hosting-merge.yml` (push to `main` → `live`). Workflows build with `pnpm install` + `pnpm build` in `Client/`; `FirebaseExtended/action-hosting-deploy` deploys—do not run `deploy:hosting` in CI (double deploy). Firebase/GCP project `honest-car-498923` (same project as Cloud Run); GitHub secret `FIREBASE_SERVICE_ACCOUNT_HONEST_CAR_498923`.
- Public support contact email is `support@honest-car.co`.
- Run Spotless after Java changes and Prettier after TypeScript changes.
- Prod profile (`SPRING_PROFILES_ACTIVE=prod`): `server.port=${PORT:8080}` for Cloud Run, `app.public-url` defaults to `https://honest-car.co` (`APP_PUBLIC_URL` override), structured JSON stdout logging (`logging.structured.format.console=logstash`), `server.forward-headers-strategy=framework`, and `GCP_PROJECT_ID` for Cloud Logging trace correlation.
- Local dev: Angular uses same-origin API URLs (`Client/src/app/core/api/api.config.ts`); `Client/proxy.conf.json` proxies `/api/**` to `http://localhost:8080`. Production Firebase Hosting (`firebase.json`, project `honest-car-498923`) rewrites `/api/**` to Cloud Run `honest-car-server` in `us-central1` (`run.serviceId` is the service name, not the project ID; Hosting only resolves Cloud Run in the same Firebase/GCP project). Custom domain `honest-car.co` is connected in Firebase Console (Hosting → Custom domains). No server CORS config. Deploy frontend: `pnpm --dir Client deploy:hosting`.
- Server Gradle uses `implementation(platform(SpringBootPlugin.BOM_COORDINATES))` instead of `io.spring.dependency-management`; `flyway-database-postgresql` is pinned at `11.15.0` above the BOM. Local JVM dev uses JDK 25; GraalVM native images are built only via `Server/Dockerfile` (`docker build`), not `./gradlew nativeCompile` on the host. On Windows there is no `gradlew.bat`—invoke `./gradlew` via Git Bash `sh` (e.g. `"C:\Program Files\Git\bin\sh.exe" ./gradlew test`). Dockerfile runs `sed -i 's/\r$//' gradlew` before invoking Gradle (Windows CRLF). Avoid BuildKit-only `RUN --mount=type=cache` in the Dockerfile—Google Cloud Build's default Docker builder does not enable BuildKit; rely on multi-stage layer caching instead.

## Cursor Cloud specific instructions

### One-time VM prerequisites (not in the update script)

- **Node.js 24 LTS**: use nvm (`nvm install 24 && nvm alias default 24`). Cloud Agent VMs ship `/exec-daemon/node` (v22) earlier on `PATH` than nvm; prepend Node 24 to `PATH` in `~/.bashrc`, e.g. `export PATH="$HOME/.nvm/versions/node/v24.16.0/bin:$PATH"`, then `corepack prepare pnpm@11.3.0 --activate`. Verify with `node -v` → `v24.x`.
- **JDK 25**: local dev and tests use any JDK 25 (Gradle toolchain in `Server/build.gradle.kts`). Native image builds use GraalVM inside `Server/Dockerfile`; no host GraalVM install required.
- **PostgreSQL 16+** (Docker `postgres:16`, local install, or **Supabase**): dev uses Flyway on boot against `DB_URL` from `/.env`. Start local Postgres with `sudo pg_ctlcluster 16 main start` (create `honestcar` DB/user if needed). Local example: `jdbc:postgresql://localhost:5432/honestcar`. Supabase **transaction pooler** example: `jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:6543/postgres?user=postgres.<project-ref>&password=<pass>&sslmode=require&prepareThreshold=0` with matching `DB_USER=postgres.<project-ref>`. Use `prepareThreshold=0` on port `6543` (PgBouncer). Direct `db.<project-ref>.supabase.co:5432` is IPv6-only and often fails on local networks.
- **Injected `DB_*` env vars**: Cloud Agent secrets for `DB_URL`/`DB_USER`/`DB_PASS` override `/.env` at runtime. If they point at a non-PostgreSQL URL, export PostgreSQL values on the `bootRun` command line (or unset them) so Spring connects to local Postgres.
- **Repo-root `/.env`**: gitignored; required for `SPRING_PROFILES_ACTIVE=dev` (`application-dev.properties` imports `../.env`). Copy variable names from `Server/src/main/resources/application.properties`. Set `MAILJET_ENABLED=false` when Mailjet keys are unavailable (signup returns 503 until email is configured).
- **pnpm 11**: `packageManager` in `Client/package.json` is `pnpm@11.6.0`; activate via `corepack prepare pnpm@11.6.0 --activate` (corepack auto-fetches the pinned version when running `pnpm` inside `Client/`).

### Running services

| Service | Command | Port |
|---------|---------|------|
| Backend (JVM dev) | `cd Server && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` (prepend `DB_URL=jdbc:postgresql://localhost:5432/honestcar DB_USER=honestcar DB_PASS=honestcar` when injected secrets override `/.env`) | 8080 |
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
- **Shared harness:** `Server/src/test/java/com/capstone/support/IntegrationTestProperties.java` exposes `h2CreateDrop(dbName)` (Hibernate `create-drop`, Flyway off) and `h2FlywaySeed(dbName)` (Flyway V1 seed data, Hibernate `validate`). `MailjetTestSupport.stubSuccessfulSend` stubs `@MockitoBean MailjetClient` for flows that send email.
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

- **Two H2 + Flyway setups:**
  - **Default test config** (`Server/src/test/resources/application.properties`): `flyway.enabled=false`, Hibernate `create-drop`; used by `@SpringBootTest` tests without overrides.
  - **Legacy seeded tests** (`VehicleReadDaoTest`, `MaintenanceDashboardIntegrationTest`, `MigrationValidationTest`): inline `@SpringBootTest(properties=…)` with `spring.flyway.enabled=true` and seed SQL at `classpath:db/migration` (`Server/src/test/resources/db/V1_inital_schema.sql`).
  - **New seeded integration tests** via `IntegrationTestProperties.h2FlywaySeed(...)`: Flyway loads `classpath:integration-test-db/migration` (`Server/src/test/resources/integration-test-db/migration/V1__Initial_schema.sql`) so seed data does not conflict with the legacy `db/migration` path.
- **Test JWT secret must be valid Base64.** `JwtService` decodes `security.jwt.secret` with `Decoders.BASE64`. Test config uses `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=` (same key material as `JwtServiceTest`). Integration tests that issue real JWTs inherit this from `application.properties` or `IntegrationTestProperties.JWT_SECRET`.
- **Repository query guard:** `RepositoryQueryArchitectureTest` fails CI if a repository method combines `@Modifying` with a `SELECT` `@Query`, or returns primitive `boolean` from a nullable scalar `SELECT`. Add new `*RepositoryJPA` interfaces with custom `@Query` to that test's scan list.
- **MockMvc (Spring Boot 4):** `@AutoConfigureMockMvc` lives in `org.springframework.boot.webmvc.test.autoconfigure`; dependency is `spring-boot-starter-webmvc-test`. CSRF is enforced on mutating auth endpoints—bootstrap `XSRF-TOKEN` via a GET (e.g. `/actuator/health`) before POSTing in MockMvc tests.
- **Transactional deletes in repository tests:** `@Modifying` / derived delete methods need an active transaction; annotate the test method with `@Transactional` when calling `deleteByUser`, `deleteOrphanedVins`, etc.

### Client testing conventions

- Auth specs live under `Client/src/app/core/auth/` and `Client/src/app/core/http/`. Interceptor specs (`unauthorized.interceptor.spec.ts`, `credentials.interceptor.spec.ts`) use `provideHttpClient(withInterceptors([...]))` + `HttpTestingController`, matching `auth.service.spec.ts`.
- Registration/login error handling is tested against plain-text error bodies returned by `GlobalExceptionHandler` (e.g. 500 → `"Sometimes things just don't go as planned."`).

Production backend images are built from `Server/Dockerfile`, which compiles a GraalVM native executable and packages it in a `debian:bookworm-slim` container for Cloud Run. `nativeCompile` is gated behind `NATIVE_IMAGE_BUILD=true` (set in the Dockerfile). Native AOT uses the `prod` profile by default. Conditional beans (for example Mailjet) are fixed at AOT build time via the placeholder env vars in `Server/build.gradle.kts`.

Server tests use in-memory H2 in PostgreSQL compatibility mode for Flyway integration tests (not a real Postgres instance). One integration test (`VehicleOnboardingProviderBurstIntegrationTest`) can be timing-sensitive on slow VMs. Client `local-date.spec.ts` asserts UTC vs local calendar dates and may fail when the VM timezone is UTC.

### External APIs (optional for browse-only dev)

Full vehicle and email flows need Auto.dev, Vehicle Databases, and Mailjet keys in `/.env`. Browse-only (landing, services, auth UI) works with placeholder provider keys and `MAILJET_ENABLED=false`.

### Non-obvious gotchas (verified in setup)

- **`spotlessCheck` also formats Spring Boot AOT-generated sources.** The spring-boot AOT plugin registers `build/generated/aotSources` into the main source set, and Spotless (no explicit `target()`) scans it; `./gradlew spotlessCheck` therefore triggers `processAot` and FAILS on the generated code even on a clean checkout (true on both `main` and `version-bump`). The hand-written `src/` tree is clean — confirm by checking that no `src/main` or `src/test` paths appear in the violation list; generated-only failures can be ignored for source linting.
- **Injected `DB_*` secrets point to a real Supabase Postgres.** With the Cloud Agent secrets present, `bootRun` connects to Supabase out of the box (reports Postgres 17.x, catalog `postgres`). To avoid writing to that shared DB during local testing, override `DB_URL`/`DB_USER`/`DB_PASS` to local Postgres on the `bootRun` command line.
- **Flyway checksum mismatch on a reused local DB.** A local `honestcar` DB migrated in a previous session can have a different V1 checksum than the current branch (migrations were consolidated into V1), causing `FlywayValidateException` at startup. Fix by resetting the local DB: `sudo -u postgres psql -c "DROP DATABASE honestcar;"` then recreate it with owner `honestcar` and let Flyway re-apply V1.
- **Email is mandatory for signup.** `/api/auth/register` always calls Mailjet (the verification code is stored bcrypt-hashed, so it cannot be recovered from the DB). Without a reachable inbox, seed a verified user directly into `user_detail` (`email_verified=true`, `user_pw` = a BCrypt hash) and use `/api/auth/authenticate` / the login UI to reach the authenticated dashboard. The SPA's first login POST can fail once if the `XSRF-TOKEN` cookie hasn't been issued yet; a retry succeeds.
- **Custom `@Query` on `*RepositoryJPA` can fail at runtime, not compile time.** A refactor that adds `SELECT` JPQL to `@Modifying` delete methods or returns nullable scalars for primitive `boolean` caused registration HTTP 500. Prefer Spring Data derived query method names (`existsBy…`, `deleteBy…`) when possible; when adding custom `@Query`, add a repository integration test and update `RepositoryQueryArchitectureTest`.
- **`VehicleReadDaoTest` / `MaintenanceDashboardIntegrationTest` can fail intermittently** on slow or UTC-timezone VMs (assertion / seed lookup issues). Failures in those classes are often environmental, not caused by unrelated auth/repository changes—confirm with the targeted suites above first.
