-- Pre-flight: assumes no two existing rows collide once user_email is lowercased
-- and trimmed. If they do, CREATE UNIQUE INDEX below will fail and roll back the
-- entire migration (PostgreSQL DDL is transactional). Run this query manually
-- before deploying if existing data is suspect:
--   SELECT lower(btrim(user_email)), COUNT(*) FROM "user"
--   GROUP BY 1 HAVING COUNT(*) > 1;

UPDATE "user"
SET user_email = lower(btrim(user_email));

ALTER TABLE "user"
    ALTER COLUMN user_email TYPE varchar(254);

ALTER TABLE "user"
    ALTER COLUMN first_name TYPE varchar(100);

ALTER TABLE "user"
    ALTER COLUMN last_name TYPE varchar(100);

ALTER TABLE "user"
    ALTER COLUMN user_sms TYPE varchar(20);

ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_email
    ON "user" (user_email);
