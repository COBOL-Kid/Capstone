---
sessionId: session-260429-222941-ie3q
isActive: true
---

# Requirements

### Overview & Goals
Replace the four existing Flyway migrations (V1–V4) with a single consolidated `V1__init.sql` that produces the final schema state and is PostgreSQL-dialect-compatible. Confirm the rest of the project is already Postgres-compatible.

### Scope
**In scope**
- Single new `V1__init.sql` representing the post-V4 schema.
- Delete obsolete V1–V4 migration files.
- Drop `spring.flyway.baseline-on-migrate` / `baseline-version` from `application.properties`.
- Add FK indexes (PostgreSQL does not auto-create them).
- Audit code/queries for non-Postgres SQL.

**Out of scope**
- Java/entity code changes (none needed).
- Test config (H2 + PostgreSQL mode + Flyway disabled stays as-is).

### Functional Requirements
- A fresh PostgreSQL database boots cleanly: Flyway applies V1, Hibernate `validate` passes.

# Technical Design

### Current Implementation
- Migrations under `Server/src/main/resources/db/migration/`: V1 base schema, V2 adds `vehicle_style` to identity, V3 adds `user_vin` photo columns, V4 hardens `"user"` (lowercase email, widen columns, add `created_at`/`updated_at`, unique index on email).
- Entities under `com/capstone/models` use `@Column(columnDefinition=...)` with PG-native types; `StringListJsonConverter` stores `List<String>` as `text` JSON.
- Repositories use only JPQL (no `nativeQuery`).
- `application.properties` runs Hibernate `validate` against `PostgreSQLDialect`; tests use H2 PG-mode with Flyway disabled.

### Key Decisions
- Delete V1–V4 entirely and replace with one `V1__init.sql` (project not in production).
- Remove `baseline-on-migrate` / `baseline-version` (only relevant for adopting Flyway on an existing DB).
- Add explicit indexes on every FK column.
- Keep `available_image_urls` as `text` (matches converter and `columnDefinition="text"`).
- Keep `"user"` table name double-quoted (PG reserved word).
- Use table-level `UNIQUE` constraint for `user_email` instead of separate `CREATE UNIQUE INDEX`.

### Proposed Changes
1. Create `Server/src/main/resources/db/migration/V1__init.sql` containing all 9 tables in their final state, with FK indexes.
2. Delete `V1__create_application_schema.sql`, `V2__add_vehicle_style_to_vehicle_type_identity.sql`, `V3__add_user_vehicle_photos.sql`, `V4__harden_user_accounts.sql`.
3. Edit `Server/src/main/resources/application.properties`: remove the two `spring.flyway.baseline-*` lines.

### Postgres Compatibility Audit (no code changes required)
- All `columnDefinition` strings use PG types (`char`, `varchar`, `text`, `int`, `integer`, `boolean`).
- `Instant` → `timestamp with time zone`, `LocalDate` → `date`, `Double` → `double precision`.
- All custom queries are JPQL.
- Driver `org.postgresql:postgresql` and `flyway-database-postgresql` are already in `build.gradle.kts`.

### File Structure
```
Server/src/main/resources/
├── application.properties              (modified — drop baseline props)
└── db/migration/
    └── V1__init.sql                    (new)
        # V1–V4 old files                 — DELETED
```

### Risks
- Dev DBs already migrated through V4 will mismatch the new V1 checksum/description — drop & recreate (acceptable pre-prod).
- Any drift between V1 and a `@Column(columnDefinition=...)` would fail `ddl-auto=validate`; mitigated by deriving each column directly from the entities.

# Testing

### Validation Approach
- Boot Spring Boot against a clean PostgreSQL DB (`DB_URL/DB_USER/DB_PASS` set); confirm Flyway records `V1__init` and Hibernate `validate` passes.
- Run `./gradlew :Server:test`; H2 path is unaffected (Flyway disabled, `ddl-auto=create-drop`).

### Key Scenarios
- Fresh deploy: empty DB → V1 applied → app starts.
- Repeat boot: no new migrations applied; `flyway_schema_history` unchanged.

### Edge Cases
- Reserved word `user` remains quoted everywhere in SQL.
- `available_image_urls` default `'[]'` survives a row insert that omits the column (matches converter expectations).

# Delivery Steps

###   Step 1: Write consolidated V1__init.sql for PostgreSQL
A single `Server/src/main/resources/db/migration/V1__init.sql` exists that creates the full final-state schema and applies cleanly on a fresh Postgres DB.

- Create all 9 tables: `"user"`, `vehicle_type`, `vin`, `user_vin`, `maint_mileage`, `maint_cost`, `recall`, `completed_maintenance`, `completed_recall`.
- Use Postgres-native types (`BIGSERIAL`, `bigint`, `char(17)`, `varchar(N)`, `text`, `boolean`, `date`, `timestamp with time zone`, `double precision`).
- Bake V2 changes in: `vehicle_type.vehicle_style varchar(160) NOT NULL DEFAULT 'UNKNOWN'`, included in `uk_vehicle_type_identity`.
- Bake V3 changes in: `user_vin.available_image_urls text NOT NULL DEFAULT '[]'`, `user_vin.selected_image_url varchar(500)`.
- Bake V4 changes in: `user_email varchar(254)`, `first_name`/`last_name varchar(100)`, `user_sms varchar(20)`, `created_at`/`updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP`, table-level `UNIQUE (user_email)`.
- Keep `"user"` double-quoted (reserved word).
- Add FK indexes: `vin(vehicle_type_id)`, `user_vin(vin_num)`, `maint_mileage(vehicle_type_id)`, `maint_cost(vehicle_type_id)`, `recall(vehicle_type_id)`, `completed_maintenance(maint_mileage_id)`, `completed_recall(recall_id)`.

###   Step 2: Remove obsolete V1–V4 migrations
The `db/migration` directory contains only the new `V1__init.sql`.

- Delete `Server/src/main/resources/db/migration/V1__create_application_schema.sql`.
- Delete `Server/src/main/resources/db/migration/V2__add_vehicle_style_to_vehicle_type_identity.sql`.
- Delete `Server/src/main/resources/db/migration/V3__add_user_vehicle_photos.sql`.
- Delete `Server/src/main/resources/db/migration/V4__harden_user_accounts.sql`.

###   Step 3: Trim Flyway baseline config from application.properties
`application.properties` no longer contains baseline settings that are irrelevant for a fresh first deploy.

- In `Server/src/main/resources/application.properties`, remove `spring.flyway.baseline-on-migrate=true`.
- Remove `spring.flyway.baseline-version=1`.
- Leave all other properties untouched (datasource, JPA, dialect, JWT).

###   Step 4: Validate boot against PostgreSQL and run test suite
Confirmed the consolidated migration applies cleanly and existing tests still pass.

- Boot the app with `DB_URL/DB_USER/DB_PASS` pointing at a clean Postgres DB; verify Flyway logs `V1__init` applied and Hibernate `validate` succeeds.
- Verify `flyway_schema_history` contains a single `V1` row with the new description.
- Run `./gradlew :Server:test` and confirm all tests pass (H2 path uses `create-drop`, Flyway disabled — unaffected).
- If any column drift causes `validate` to fail, adjust the SQL column definition (entities are the source of truth).