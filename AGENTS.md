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
- Local dev: `ng serve` proxies `/api` to the backend (`proxy.conf.json`) so API calls stay same-origin for cookies and XSRF.
