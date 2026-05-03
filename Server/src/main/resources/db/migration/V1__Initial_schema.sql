CREATE TABLE user_detail (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(254) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    user_sms VARCHAR(20),
    user_pw VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    failed_login_attempts INTEGER NOT NULL,
    lockout_end TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES user_detail(user_id)
);

CREATE TABLE vehicle_type (
    vehicle_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_make VARCHAR(60) NOT NULL,
    vehicle_model VARCHAR(80) NOT NULL,
    vehicle_trim VARCHAR(120) NOT NULL,
    vehicle_year CHAR(4) NOT NULL,
    vehicle_style VARCHAR(160) NOT NULL,
    source_vin CHAR(17),
    origin VARCHAR(60),
    body VARCHAR(80),
    engine_description VARCHAR(120),
    transmission_style VARCHAR(50),
    drive_type VARCHAR(80),
    owners_manual VARCHAR(500),
    CONSTRAINT uk_vehicle_type UNIQUE (vehicle_year, vehicle_make, vehicle_model, vehicle_trim, vehicle_style)
);

CREATE TABLE vin (
    vin_num CHAR(17) PRIMARY KEY,
    vin_mileage INTEGER NOT NULL,
    vehicle_type_id BIGINT NOT NULL,
    CONSTRAINT fk_vin_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type(vehicle_type_id)
);

CREATE TABLE user_vin (
    user_id BIGINT NOT NULL,
    vin_num CHAR(17) NOT NULL,
    current_mileage INT NOT NULL,
    available_image_urls TEXT NOT NULL,
    selected_image_url VARCHAR(500),
    PRIMARY KEY (user_id, vin_num),
    CONSTRAINT fk_user_vin_user FOREIGN KEY (user_id) REFERENCES user_detail(user_id),
    CONSTRAINT fk_user_vin_vin FOREIGN KEY (vin_num) REFERENCES vin(vin_num)
);

CREATE TABLE maint_mileage (
    maint_mileage_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type_id BIGINT NOT NULL,
    mileage_due INT NOT NULL,
    maint_desc VARCHAR(255) NOT NULL,
    CONSTRAINT uk_maint_mileage UNIQUE (vehicle_type_id, mileage_due, maint_desc),
    CONSTRAINT fk_maint_mileage_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type(vehicle_type_id)
);

CREATE TABLE maint_cost (
    maint_cost_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type_id BIGINT,
    maint_title VARCHAR(120) NOT NULL,
    maint_desc TEXT,
    independent_avg INT,
    independent_high INT,
    independent_low INT,
    dealer_avg INT,
    dealer_high INT,
    dealer_low INT,
    CONSTRAINT uk_maint_cost UNIQUE (vehicle_type_id, maint_title),
    CONSTRAINT fk_maint_cost_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type(vehicle_type_id)
);

CREATE TABLE recall (
    recall_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type_id BIGINT NOT NULL,
    nhtsa_campaign_number VARCHAR(20) NOT NULL,
    recall_no VARCHAR(20),
    report_received_date DATE NOT NULL,
    component VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    consequence TEXT NOT NULL,
    remedy TEXT NOT NULL,
    notes TEXT,
    manufacturer VARCHAR(120),
    park_it BOOLEAN NOT NULL,
    park_outside BOOLEAN NOT NULL,
    over_the_air_update BOOLEAN NOT NULL,
    model_year CHAR(4),
    make VARCHAR(60),
    model VARCHAR(80),
    CONSTRAINT uk_recall UNIQUE (vehicle_type_id, nhtsa_campaign_number),
    CONSTRAINT fk_recall_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_type(vehicle_type_id)
);

CREATE TABLE completed_recall (
    completed_recall_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vin_num CHAR(17) NOT NULL,
    recall_id BIGINT NOT NULL,
    completed_date DATE NOT NULL,
    repair_shop VARCHAR(80),
    cost DOUBLE,
    notes TEXT,
    CONSTRAINT uk_completed_recall UNIQUE (user_id, vin_num, recall_id),
    CONSTRAINT fk_completed_recall_user_vin FOREIGN KEY (user_id, vin_num) REFERENCES user_vin(user_id, vin_num),
    CONSTRAINT fk_completed_recall_recall FOREIGN KEY (recall_id) REFERENCES recall(recall_id)
);

CREATE TABLE completed_maintenance (
    completed_maintenance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vin_num CHAR(17) NOT NULL,
    maint_mileage_id BIGINT NOT NULL,
    completed_date DATE NOT NULL,
    mileage_completed INT NOT NULL,
    cost DOUBLE,
    notes TEXT,
    CONSTRAINT uk_completed_maintenance UNIQUE (user_id, vin_num, maint_mileage_id),
    CONSTRAINT fk_completed_maintenance_user_vin FOREIGN KEY (user_id, vin_num) REFERENCES user_vin(user_id, vin_num),
    CONSTRAINT fk_completed_maintenance_maint_mileage FOREIGN KEY (maint_mileage_id) REFERENCES maint_mileage(maint_mileage_id)
);