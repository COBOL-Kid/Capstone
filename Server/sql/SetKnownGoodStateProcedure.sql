CREATE OR REPLACE PROCEDURE set_known_good_state()
LANGUAGE 'plpgsql'
AS $BODY$
BEGIN
    -- Drop constraints
    ALTER TABLE IF EXISTS public."Record" DROP CONSTRAINT IF EXISTS "FK_Vin";
    ALTER TABLE IF EXISTS public."Vin" DROP CONSTRAINT IF EXISTS "FK_Owner";
    ALTER TABLE IF EXISTS public."Vin" DROP CONSTRAINT IF EXISTS "FK_Vehicle";
    ALTER TABLE IF EXISTS public."Reminder" DROP CONSTRAINT IF EXISTS "FK_VIN";

    -- Drop tables
    DROP TABLE IF EXISTS public."Owner";
    DROP TABLE IF EXISTS public."Vehicle";
    DROP TABLE IF EXISTS public."Record";
    DROP TABLE IF EXISTS public."Vin";
    DROP TABLE IF EXISTS public."Reminder";

    -- Create tables
    CREATE TABLE IF NOT EXISTS public."Owner"
    (
        "OwnerId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
        "FirstName" text NOT NULL,
        "LastName" text NOT NULL,
        "Email" text NOT NULL,
        "UserName" text NOT NULL,
        "Password" text NOT NULL,
        CONSTRAINT "PK_Owner" PRIMARY KEY ("OwnerId")
    );

    CREATE TABLE IF NOT EXISTS public."Vehicle"
    (
        "VehicleId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
        "Year" integer NOT NULL,
        "Make" text NOT NULL,
        "Model" text NOT NULL,
        "Image" text,
        CONSTRAINT "PK_Vehicle" PRIMARY KEY ("VehicleId")
    );

    CREATE TABLE IF NOT EXISTS public."Record"
    (
        "RecordId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
        "VinId" integer NOT NULL,
        "Description" text NOT NULL,
        "Notes" text,
        "DateCompleted" date NOT NULL,
        "DueMileage" integer NOT NULL,
        "CompletedMileage" integer,
        "Cost" integer NOT NULL,
        CONSTRAINT "PK_Record" PRIMARY KEY ("RecordId")
    );

    CREATE TABLE IF NOT EXISTS public."Vin"
    (
        "VinId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
        "Vin" text NOT NULL,
        "Mileage" integer NOT NULL,
        "OwnerId" integer NOT NULL,
        "VehicleId" integer NOT NULL,
        CONSTRAINT "PK_Vin" PRIMARY KEY ("VinId")
    );

    CREATE TABLE IF NOT EXISTS public."Reminder"
    (
        "ReminderId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
        "Description" text NOT NULL,
        "ReminderDate" date,
        "VinId" integer NOT NULL,
        CONSTRAINT "PK_Reminder" PRIMARY KEY ("ReminderId")
    );

    -- Add constraints
    ALTER TABLE IF EXISTS public."Record"
        ADD CONSTRAINT "FK_Vin" FOREIGN KEY ("VinId")
        REFERENCES public."Vin" ("VinId") MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
        NOT VALID;

    ALTER TABLE IF EXISTS public."Vin"
        ADD CONSTRAINT "FK_Owner" FOREIGN KEY ("OwnerId")
        REFERENCES public."Owner" ("OwnerId") MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
        NOT VALID;

    ALTER TABLE IF EXISTS public."Vin"
        ADD CONSTRAINT "FK_Vehicle" FOREIGN KEY ("VehicleId")
        REFERENCES public."Vehicle" ("VehicleId") MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID;

    ALTER TABLE IF EXISTS public."Reminder"
        ADD CONSTRAINT "FK_VIN" FOREIGN KEY ("VinId")
        REFERENCES public."Vin" ("VinId") MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE CASCADE
        NOT VALID;

    -- Insert test data
    INSERT INTO public."Owner" ("FirstName", "LastName", "Email", "UserName", "Password")
    VALUES ('John', 'Doe', 'john.doe@example.com', 'johndoe', 'password123');

    INSERT INTO public."Vehicle" ("Year", "Make", "Model", "Image")
    VALUES (2020, 'Toyota', 'Camry', 'image_url');

    INSERT INTO public."Vin" ("Vin", "Mileage", "OwnerId", "VehicleId")
    VALUES ('1HGCM82633A123456', 12000, 1, 1);

    INSERT INTO public."Record" ("VinId", "Description", "Notes", "DateCompleted", "DueMileage", "CompletedMileage", "Cost")
    VALUES (1, 'Oil Change', 'Changed oil and filter', '2023-01-01', 15000, 12000, 50);

    INSERT INTO public."Reminder" ("Description", "ReminderDate", "VinId")
    VALUES ('Next oil change', '2023-06-01', 1);

END;
$BODY$;