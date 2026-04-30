## Goal

This project is not yet in production, so there is no need to preserve the historical V1→V4 migration chain. Replace those four files with a single, idempotently-clean `V1__init.sql` that produces the final schema and verify the rest of the project is PostgreSQL-dialect-compatible.

## Investigation Summary

### Migration files today (`Server/src/main/resources/db/migration`)
- `V1__create_application_schema.sql` — initial 9-table schema.
- `V2__add_vehicle_style_to_vehicle_type_identity.sql` — adds `vehicle_type.vehicle_style` (default `'UNKNOWN'`, NOT NULL) and rebuilds the unique constraint to include it.
- `V3__add_user_vehicle_photos.sql` — adds `user_vin.available_image_urls text NOT NULL DEFAULT '[]'` and `user_vin.selected_image_url varchar(500)`.
- `V4__harden_user_accounts.sql` — normalizes `"user".user_email` to lowercase/trimmed, widens email to `varchar(254)`, names/SMS column widening, adds `created_at`/`updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP`, and creates `UNIQUE INDEX uk_user_email ON "user" (user_email)`.

### JPA entities (`Server/src/main/java/com/capstone/models`)
- `User`, `VehicleType`, `Vin`, `UserVin` (composite PK via `UserVinId`), `MaintMileage`, `MaintCost`, `Recall`, `CompletedMaintenance`, `CompletedRecall`, plus the `StringListJsonConverter` that stores `List<String>` as a JSON `text` column.
- `User.createdAt`/`updatedAt` are `Instant` → `timestamp with time zone`. `Recall.reportReceivedDate`/`CompletedMaintenance.completedDate`/`CompletedRecall.completedDate` are `LocalDate` → `date`. Booleans on `Recall` map to `boolean`.
- `application.properties` uses `spring.jpa.hibernate.ddl-auto=validate` against `org.hibernate.dialect.PostgreSQLDialect`, so the consolidated schema must match each entity exactly.

### Repositories / queries
- `Server/src/main/java/com/capstone/data/*RepositoryJPA.java` — all custom queries are JPQL (no `nativeQuery=true`), so they are dialect-agnostic.
- No `btrim`, `regexp_*`, `::regclass`, etc. appear anywhere in `src/main/java`.

### Tests
- `src/test/resources/application.properties` runs against H2 in PostgreSQL mode with `ddl-auto=create-drop` and `spring.flyway.enabled=false`. This stays as-is, so the consolidated migration only affects production / dev runs against PostgreSQL.

### Postgres-compatibility review of the rest of the codebase
- All schema types used (`bigserial`, `bigint`, `int`, `integer`, `char(n)`, `varchar(n)`, `text`, `boolean`, `date`, `timestamp with time zone`, `double precision`) are native PostgreSQL types.
- `"user"` is a reserved word in PostgreSQL — already correctly quoted as `"user"` in V1/V4. The consolidated migration must keep the same quoting.
- `BIGSERIAL` aligns with `@GeneratedValue(strategy = GenerationType.IDENTITY)` on PostgreSQL (creates an `int8` column with an attached sequence).
- `available_image_urls text NOT NULL DEFAULT '[]'` works under PostgreSQL with the existing `StringListJsonConverter`. No `jsonb` change is necessary; keeping `text` matches the Hibernate validation against `columnDefinition = "text"`.
- `runtimeOnly("org.flywaydb:flyway-database-postgresql")` and `runtimeOnly(libs.postgresql)` are already present in `Server/build.gradle.kts`.

## Decisions (confirmed with the user)
1. Delete `V1`–`V4` and replace them with a single new `V1__init.sql`.
2. Remove `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` from `application.properties` (no longer relevant for a fresh deploy).
3. Add explicit indexes on every foreign-key column, since PostgreSQL does not auto-create them.

## Proposed Changes

### 1) Replace migrations with a single `V1__init.sql`

Path: `Server/src/main/resources/db/migration/V1__init.sql` (delete `V1__create_application_schema.sql`, `V2__add_vehicle_style_to_vehicle_type_identity.sql`, `V3__add_user_vehicle_photos.sql`, `V4__harden_user_accounts.sql`).

The new V1 reproduces the final state of the schema after V1+V2+V3+V4, written in clean PostgreSQL dialect:

```sql
-- USER ----------------------------------------------------------------
CREATE TABLE "user" (
    user_id     BIGSERIAL PRIMARY KEY,
    user_email  varchar(254) NOT NULL,
    first_name  varchar(100),
    last_name   varchar(100),
    user_sms    varchar(20),
    user_pw     varchar(100) NOT NULL,
    role        varchar(20)  NOT NULL,
    created_at  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_email UNIQUE (user_email)
);

-- VEHICLE_TYPE --------------------------------------------------------
CREATE TABLE vehicle_type (
    vehicle_type_id     BIGSERIAL PRIMARY KEY,
    vehicle_make        varchar(60)  NOT NULL,
    vehicle_model       varchar(80)  NOT NULL,
    vehicle_trim        varchar(120) NOT NULL,
    vehicle_year        char(4)      NOT NULL,
    vehicle_style       varchar(160) NOT NULL DEFAULT 'UNKNOWN',
    source_vin          char(17),
    origin              varchar(60),
    body                varchar(80),
    engine_description  varchar(120),
    transmission_style  varchar(50),
    drive_type          varchar(80),
    owners_manual       varchar(500),
    CONSTRAINT uk_vehicle_type_identity UNIQUE (
        vehicle_year, vehicle_make, vehicle_model, vehicle_trim, vehicle_style
    )
);

-- VIN -----------------------------------------------------------------
CREATE TABLE vin (
    vin_num         char(17) PRIMARY KEY,
    vin_mileage     integer  NOT NULL,
    vehicle_type_id bigint   NOT NULL,
    CONSTRAINT fk_vin_vehicle_type FOREIGN KEY (vehicle_type_id)
        REFERENCES vehicle_type (vehicle_type_id)
);
CREATE INDEX ix_vin_vehicle_type_id ON vin (vehicle_type_id);

-- USER_VIN ------------------------------------------------------------
CREATE TABLE user_vin (
    user_id              bigint   NOT NULL,
    vin_num              char(17) NOT NULL,
    current_mileage      int      NOT NULL,
    available_image_urls text     NOT NULL DEFAULT '[]',
    selected_image_url   varchar(500),
    PRIMARY KEY (user_id, vin_num),
    CONSTRAINT fk_user_vin_user FOREIGN KEY (user_id) REFERENCES "user" (user_id),
    CONSTRAINT fk_user_vin_vin  FOREIGN KEY (vin_num) REFERENCES vin (vin_num)
);
CREATE INDEX ix_user_vin_vin_num ON user_vin (vin_num);

-- MAINT_MILEAGE -------------------------------------------------------
CREATE TABLE maint_mileage (
    maint_mileage_id BIGSERIAL PRIMARY KEY,
    vehicle_type_id  bigint        NOT NULL,
    mileage_due      int           NOT NULL,
    maint_desc       varchar(255)  NOT NULL,
    CONSTRAINT fk_maint_mileage_vehicle_type FOREIGN KEY (vehicle_type_id)
        REFERENCES vehicle_type (vehicle_type_id),
    CONSTRAINT uk_maint_mileage_identity UNIQUE (vehicle_type_id, mileage_due, maint_desc)
);
CREATE INDEX ix_maint_mileage_vehicle_type_id ON maint_mileage (vehicle_type_id);

-- MAINT_COST ----------------------------------------------------------
CREATE TABLE maint_cost (
    maint_cost_id    BIGSERIAL PRIMARY KEY,
    vehicle_type_id  bigint,
    maint_title      varchar(120) NOT NULL,
    maint_desc       text,
    independent_avg  int,
    independent_high int,
    independent_low  int,
    dealer_avg       int,
    dealer_high      int,
    dealer_low       int,
    CONSTRAINT fk_maint_cost_vehicle_type FOREIGN KEY (vehicle_type_id)
        REFERENCES vehicle_type (vehicle_type_id),
    CONSTRAINT uk_maint_cost_identity UNIQUE (vehicle_type_id, maint_title)
);
CREATE INDEX ix_maint_cost_vehicle_type_id ON maint_cost (vehicle_type_id);

-- RECALL --------------------------------------------------------------
CREATE TABLE recall (
    recall_id              BIGSERIAL PRIMARY KEY,
    vehicle_type_id        bigint        NOT NULL,
    nhtsa_campaign_number  varchar(20)   NOT NULL,
    recall_no              varchar(20),
    report_received_date   date          NOT NULL,
    component              varchar(255)  NOT NULL,
    summary                text          NOT NULL,
    consequence            text          NOT NULL,
    remedy                 text          NOT NULL,
    notes                  text,
    manufacturer           varchar(120),
    park_it                boolean       NOT NULL DEFAULT false,
    park_outside           boolean       NOT NULL DEFAULT false,
    over_the_air_update    boolean       NOT NULL DEFAULT false,
    model_year             char(4),
    make                   varchar(60),
    model                  varchar(80),
    CONSTRAINT fk_recall_vehicle_type FOREIGN KEY (vehicle_type_id)
        REFERENCES vehicle_type (vehicle_type_id),
    CONSTRAINT uk_recall_identity UNIQUE (vehicle_type_id, nhtsa_campaign_number)
);
CREATE INDEX ix_recall_vehicle_type_id ON recall (vehicle_type_id);

-- COMPLETED_MAINTENANCE ----------------------------------------------
CREATE TABLE completed_maintenance (
    completed_maintenance_id BIGSERIAL PRIMARY KEY,
    user_id           bigint   NOT NULL,
    vin_num           char(17) NOT NULL,
    maint_mileage_id  bigint   NOT NULL,
    completed_date    date     NOT NULL,
    mileage_completed int      NOT NULL,
    cost              double precision,
    notes             text,
    CONSTRAINT fk_completed_maintenance_user_vin
        FOREIGN KEY (user_id, vin_num) REFERENCES user_vin (user_id, vin_num),
    CONSTRAINT fk_completed_maintenance_maint_mileage
        FOREIGN KEY (maint_mileage_id) REFERENCES maint_mileage (maint_mileage_id),
    CONSTRAINT uk_completed_maintenance_identity
        UNIQUE (user_id, vin_num, maint_mileage_id)
);
CREATE INDEX ix_completed_maintenance_maint_mileage_id
    ON completed_maintenance (maint_mileage_id);

-- COMPLETED_RECALL ----------------------------------------------------
CREATE TABLE completed_recall (
    completed_recall_id BIGSERIAL PRIMARY KEY,
    user_id        bigint   NOT NULL,
    vin_num        char(17) NOT NULL,
    recall_id      bigint   NOT NULL,
    completed_date date     NOT NULL,
    repair_shop    varchar(80),
    cost           double precision,
    notes          text,
    CONSTRAINT fk_completed_recall_user_vin
        FOREIGN KEY (user_id, vin_num) REFERENCES user_vin (user_id, vin_num),
    CONSTRAINT fk_completed_recall_recall
        FOREIGN KEY (recall_id) REFERENCES recall (recall_id),
    CONSTRAINT uk_completed_recall_identity
        UNIQUE (user_id, vin_num, recall_id)
);
CREATE INDEX ix_completed_recall_recall_id ON completed_recall (recall_id);
```

Notes:
- `"user"` is kept double-quoted so PostgreSQL accepts the reserved word — matching `@Entity(name = "USER")` on `User.java`.
- `uk_user_email` is created as a table-level unique constraint instead of a separate `CREATE UNIQUE INDEX` (functionally equivalent, simpler in a single fresh-state file).
- `vehicle_style` is defined NOT NULL with default `'UNKNOWN'` and is part of `uk_vehicle_type_identity` from the start, no separate UPDATE step needed.
- The V4 lowercase/trim normalization step is irrelevant on a fresh DB because the application normalizes emails via `EmailNormalizer.normalize(...)` (`Server/src/main/java/com/capstone/Authentication/EmailNormalizer.java`).

### 2) Trim `application.properties`

Path: `Server/src/main/resources/application.properties`

Remove:
```
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

These exist solely to support attaching Flyway to a database that already had non-Flyway tables. With a single fresh-deploy V1, they are unnecessary.

### 3) Codebase Postgres-compatibility verification (no code changes expected)

- All JPA `@Column(columnDefinition = ...)` strings use Postgres-native types (`char(17)`, `varchar(N)`, `text`, `int`, `integer`).
- All custom queries in `com.capstone.data` are JPQL → no dialect lock-in.
- The Jackson-based `StringListJsonConverter` stores into a `text` column, which is fully Postgres-compatible.
- Hibernate dialect (`org.hibernate.dialect.PostgreSQLDialect`) and driver (`org.postgresql:postgresql`) are already wired in.
- Tests still use H2 in PostgreSQL mode with Flyway disabled, so the consolidated migration does not affect test runs.

If during validation any drift is found between an entity column definition and the consolidated SQL, that single column will be tweaked in the SQL (entities are the source of truth), but no Java code change is anticipated based on this review.

## File Structure

```
Server/src/main/resources/
├── application.properties              (modified — drop baseline props)
└── db/migration/
    └── V1__init.sql                    (new — consolidated schema)
        # V1__create_application_schema.sql                — DELETED
        # V2__add_vehicle_style_to_vehicle_type_identity.sql — DELETED
        # V3__add_user_vehicle_photos.sql                  — DELETED
        # V4__harden_user_accounts.sql                     — DELETED
```

## Risks
- **Existing dev databases**: anyone with a DB built from V1–V4 already applied will see Flyway's `flyway_schema_history` referencing `V1__create_application_schema`, and a different checksum/description for `V1__init`. Since the project is pre-production, this is acceptable — drop the schema and re-run. Document this in the commit message / README so dev setups are recreated cleanly.
- **`ddl-auto=validate` mismatch**: any drift between the consolidated SQL and a `@Column(columnDefinition=…)` would fail boot. Mitigation: cross-check each entity column against the new V1 (already done above).
- **Reserved word `user`**: must remain double-quoted everywhere it appears in the SQL.

## Testing / Validation
- Boot the Spring Boot app against an empty PostgreSQL database with `DB_URL/DB_USER/DB_PASS` set and confirm:
  - Flyway applies `V1__init` cleanly and records it in `flyway_schema_history`.
  - Hibernate `validate` passes (app starts without `SchemaManagementException`).
- Run the existing test suite (`./gradlew :Server:test`); H2 still uses `create-drop` and Flyway is disabled there, so it should be unaffected.
- (Optional, manual) Run `./gradlew :Server:bootRun` against a Postgres container to perform a full end-to-end sanity check.
