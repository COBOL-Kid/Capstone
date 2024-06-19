CREATE OR REPLACE PROCEDURE set_known_good_state()
LANGUAGE 'plpgsql'
AS $BODY$
BEGIN

ALTER TABLE IF EXISTS public."Record" DROP CONSTRAINT IF EXISTS "FK_Vin";

ALTER TABLE IF EXISTS public."Vin" DROP CONSTRAINT IF EXISTS "FK_OWNER";

ALTER TABLE IF EXISTS public."Vin" DROP CONSTRAINT IF EXISTS "FK_VEHICLEINFO";

ALTER TABLE IF EXISTS public."Reminder" DROP CONSTRAINT IF EXISTS "FK_VIN";

ALTER TABLE IF EXISTS public."Reminder" DROP CONSTRAINT IF EXISTS "FK_Record";



DROP TABLE IF EXISTS public."Owner";

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

DROP TABLE IF EXISTS public."VehicleInfo";

CREATE TABLE IF NOT EXISTS public."VehicleInfo"
(
    "VehicleInfoId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
    "Year" integer NOT NULL,
    "Make" text NOT NULL,
    "Model" text NOT NULL,
    "Image" text,
    PRIMARY KEY ("VehicleInfoId")
);

DROP TABLE IF EXISTS public."Record";

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

DROP TABLE IF EXISTS public."Vin";

CREATE TABLE IF NOT EXISTS public."Vin"
(
    "VinId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
    "OwnerId" integer NOT NULL,
    "VehicleInfoId" integer NOT NULL,
    "Vin" text NOT NULL,
    "Mileage" integer NOT NULL,
    CONSTRAINT "PK_Vin" PRIMARY KEY ("VinId")
);

DROP TABLE IF EXISTS public."Reminder";

CREATE TABLE IF NOT EXISTS public."Reminder"
(
    "ReminderId" integer NOT NULL GENERATED ALWAYS AS IDENTITY,
    "VinId" integer NOT NULL,
    "RecordId" integer,
    "Description" text NOT NULL,
    "ReminderDate" date NOT NULL,
    CONSTRAINT "PK_Reminder" PRIMARY KEY ("ReminderId")
);

ALTER TABLE IF EXISTS public."Record"
    ADD CONSTRAINT "FK_Vin" FOREIGN KEY ("VinId")
    REFERENCES public."Vin" ("VinId") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public."Vin"
    ADD CONSTRAINT "FK_OWNER" FOREIGN KEY ("OwnerId")
    REFERENCES public."Owner" ("OwnerId") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public."Vin"
    ADD CONSTRAINT "FK_VEHICLEINFO" FOREIGN KEY ("VehicleInfoId")
    REFERENCES public."VehicleInfo" ("VehicleInfoId") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public."Reminder"
    ADD CONSTRAINT "FK_VIN" FOREIGN KEY ("VinId")
    REFERENCES public."Vin" ("VinId") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


ALTER TABLE IF EXISTS public."Reminder"
    ADD CONSTRAINT "FK_Record" FOREIGN KEY ("RecordId")
    REFERENCES public."Record" ("RecordId") MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    NOT VALID;


INSERT INTO public."Owner" ("FirstName", "LastName", "Email", "UserName", "Password")
VALUES 
('John', 'Doe', 'john.doe@example.com', 'johndoe', 'password123'),
('Jane', 'Smith', 'jane.smith@example.com', 'janesmith', 'password456');


INSERT INTO public."VehicleInfo" ("Year", "Make", "Model", "Image")
VALUES 
(2020, 'Toyota', 'Camry', 'toyota_camry_2020.jpg'),
(2019, 'Honda', 'Civic', 'honda_civic_2019.jpg');


INSERT INTO public."Vin" ("OwnerId", "VehicleInfoId", "Vin", "Mileage")
VALUES 
(1, 1, '1HGCM82633A123456', 15000),
(2, 2, '2HGCM82633A654321', 20000);


INSERT INTO public."Record" ("VinId", "Description", "Notes", "DateCompleted", "DueMileage", "CompletedMileage", "Cost")
VALUES 
(1, 'Oil Change', 'Changed oil and filter', '2023-01-15', 16000, 15000, 50),
(2, 'Tire Rotation', 'Rotated all four tires', '2023-02-20', 21000, 20000, 30);


INSERT INTO public."Reminder" ("VinId", "RecordId", "Description", "ReminderDate")
VALUES 
(1, 1, 'Next oil change', '2023-06-15'),
(2, 2, 'Next tire rotation', '2023-08-20');

END;
$BODY$;