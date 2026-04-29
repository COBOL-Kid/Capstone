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
    ADD COLUMN IF NOT EXISTS created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE "user"
    ADD COLUMN IF NOT EXISTS updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_email
    ON "user" (user_email);
