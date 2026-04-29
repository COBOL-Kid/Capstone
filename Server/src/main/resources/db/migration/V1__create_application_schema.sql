CREATE TABLE "user" (
    user_id BIGSERIAL PRIMARY KEY,
    user_email varchar(50) NOT NULL,
    first_name varchar(50),
    last_name varchar(50),
    user_sms varchar(12),
    user_pw varchar(100) NOT NULL,
    role varchar(20) NOT NULL
);

CREATE TABLE vehicle_type (
    vehicle_type_id BIGSERIAL PRIMARY KEY,
    vehicle_make varchar(60) NOT NULL,
    vehicle_model varchar(80) NOT NULL,
    vehicle_trim varchar(120) NOT NULL,
    vehicle_year char(4) NOT NULL,
    vehicle_style varchar(160) DEFAULT 'UNKNOWN' NOT NULL,
    source_vin char(17),
    origin varchar(60),
    body varchar(80),
    engine_description varchar(120),
    transmission_style varchar(50),
    drive_type varchar(80),
    owners_manual varchar(500),
    CONSTRAINT uk_vehicle_type_identity UNIQUE (
        vehicle_year,
        vehicle_make,
        vehicle_model,
        vehicle_trim,
        vehicle_style
    )
);

CREATE TABLE vin (
    vin_num char(17) PRIMARY KEY,
    vin_mileage integer NOT NULL,
    vehicle_type_id bigint NOT NULL,
    CONSTRAINT fk_vin_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (vehicle_type_id)
);

CREATE TABLE user_vin (
    user_id bigint NOT NULL,
    vin_num char(17) NOT NULL,
    current_mileage int NOT NULL,
    PRIMARY KEY (user_id, vin_num),
    CONSTRAINT fk_user_vin_user FOREIGN KEY (user_id) REFERENCES "user" (user_id),
    CONSTRAINT fk_user_vin_vin FOREIGN KEY (vin_num) REFERENCES vin (vin_num)
);

CREATE TABLE maint_mileage (
    maint_mileage_id BIGSERIAL PRIMARY KEY,
    vehicle_type_id bigint NOT NULL,
    mileage_due int NOT NULL,
    maint_desc varchar(255) NOT NULL,
    CONSTRAINT fk_maint_mileage_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (vehicle_type_id),
    CONSTRAINT uk_maint_mileage_identity UNIQUE (vehicle_type_id, mileage_due, maint_desc)
);

CREATE TABLE maint_cost (
    maint_cost_id BIGSERIAL PRIMARY KEY,
    vehicle_type_id bigint,
    maint_title varchar(120) NOT NULL,
    maint_desc text,
    independent_avg int,
    independent_high int,
    independent_low int,
    dealer_avg int,
    dealer_high int,
    dealer_low int,
    CONSTRAINT fk_maint_cost_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (vehicle_type_id),
    CONSTRAINT uk_maint_cost_identity UNIQUE (vehicle_type_id, maint_title)
);

CREATE TABLE recall (
    recall_id BIGSERIAL PRIMARY KEY,
    vehicle_type_id bigint NOT NULL,
    nhtsa_campaign_number varchar(20) NOT NULL,
    recall_no varchar(20),
    report_received_date date NOT NULL,
    component varchar(255) NOT NULL,
    summary text NOT NULL,
    consequence text NOT NULL,
    remedy text NOT NULL,
    notes text,
    manufacturer varchar(120),
    park_it boolean DEFAULT false NOT NULL,
    park_outside boolean DEFAULT false NOT NULL,
    over_the_air_update boolean DEFAULT false NOT NULL,
    model_year char(4),
    make varchar(60),
    model varchar(80),
    CONSTRAINT fk_recall_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type (vehicle_type_id),
    CONSTRAINT uk_recall_identity UNIQUE (vehicle_type_id, nhtsa_campaign_number)
);

CREATE TABLE completed_maintenance (
    completed_maintenance_id BIGSERIAL PRIMARY KEY,
    user_id bigint NOT NULL,
    vin_num char(17) NOT NULL,
    maint_mileage_id bigint NOT NULL,
    completed_date date NOT NULL,
    mileage_completed int NOT NULL,
    cost double precision,
    notes text,
    CONSTRAINT fk_completed_maintenance_user_vin FOREIGN KEY (user_id, vin_num) REFERENCES user_vin (user_id, vin_num),
    CONSTRAINT fk_completed_maintenance_maint_mileage FOREIGN KEY (maint_mileage_id)
        REFERENCES maint_mileage (maint_mileage_id),
    CONSTRAINT uk_completed_maintenance_identity UNIQUE (user_id, vin_num, maint_mileage_id)
);

CREATE TABLE completed_recall (
    completed_recall_id BIGSERIAL PRIMARY KEY,
    user_id bigint NOT NULL,
    vin_num char(17) NOT NULL,
    recall_id bigint NOT NULL,
    completed_date date NOT NULL,
    repair_shop varchar(80),
    cost double precision,
    notes text,
    CONSTRAINT fk_completed_recall_user_vin FOREIGN KEY (user_id, vin_num) REFERENCES user_vin (user_id, vin_num),
    CONSTRAINT fk_completed_recall_recall FOREIGN KEY (recall_id) REFERENCES recall (recall_id),
    CONSTRAINT uk_completed_recall_identity UNIQUE (user_id, vin_num, recall_id)
);
