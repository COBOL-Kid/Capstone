-- E2E demo data for local Postgres (honestcar database).
-- Run against a database that already has the V1 schema from Flyway:
--   psql -U honestcar -d honestcar -f e2e/scripts/seed-e2e-data.sql
--
-- Login: test.user@example.com / Password123!
-- Vehicles on /home after login:
--   4T1C11AK5LU123456 — 2020 Toyota Camry (45,200 mi)
--   2HGFC2F59JH543210 — 2018 Honda Civic (78,500 mi)
--
-- Safe to re-run: clears prior demo rows by email/VIN before inserting.

DELETE FROM completed_recall
WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = 'test.user@example.com');

DELETE FROM completed_maintenance
WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = 'test.user@example.com');

DELETE FROM user_vin
WHERE user_id IN (SELECT user_id FROM user_detail WHERE user_email = 'test.user@example.com');

DELETE FROM user_detail WHERE user_email = 'test.user@example.com';

DELETE FROM vin
WHERE vin_num IN ('4T1C11AK5LU123456', '2HGFC2F59JH543210');

DELETE FROM vehicle_type
WHERE source_vin IN ('4T1C11AK5LU123456', '2HGFC2F59JH543210');

DELETE FROM maint_labor_line WHERE maint_mileage_id BETWEEN 1 AND 7;
DELETE FROM maint_part_line WHERE maint_mileage_id BETWEEN 1 AND 7;
DELETE FROM maint_mileage_summary WHERE vehicle_type_id IN (1, 2);
DELETE FROM maint_mileage WHERE vehicle_type_id IN (1, 2);
DELETE FROM misc_maint_cost WHERE vehicle_type_id IN (1, 2);
DELETE FROM recall WHERE vehicle_type_id IN (1, 2);
DELETE FROM vehicle_warranty
WHERE (vehicle_year, vehicle_make, vehicle_model) IN (
    ('2020', 'Toyota', 'Camry'),
    ('2018', 'Honda', 'Civic')
);

INSERT INTO user_detail (user_id, user_email, first_name, last_name, user_sms, user_pw, role,
                         failed_login_attempts, lockout_end, email_verified, email_verified_at,
                         created_at, updated_at)
VALUES (1, 'test.user@example.com', 'Test', 'User', '+15551234567',
        '$2y$10$Zy4xFp/QwJaDF5kkE5ob1uzhr8YD3VsqTuV8bFLr.jSeyfEgBUyjq', 'USER', 0, NULL, TRUE,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO vehicle_type (vehicle_type_id, vehicle_make, vehicle_model, vehicle_trim, vehicle_year, vehicle_style,
                          source_vin, origin, body, engine_description, transmission_style, drive_type, owners_manual)
VALUES (1, 'Toyota', 'Camry', 'SE', '2020', '4-Door Sedan', '4T1C11AK5LU123456', 'Japan', 'Sedan',
        '2.5L 4-Cylinder', 'Automatic', 'FWD',
        'https://www.toyota.com/owners/resources/warranty-owners-manuals/camry'),
       (2, 'Honda', 'Civic', 'EX', '2018', '4-Door Sedan', '2HGFC2F59JH543210', 'USA', 'Sedan',
        '2.0L 4-Cylinder', 'CVT', 'FWD',
        'https://owners.honda.com/vehicles/information/2018/Civic');

INSERT INTO vin (vin_num, vehicle_type_id)
VALUES ('4T1C11AK5LU123456', 1),
       ('2HGFC2F59JH543210', 2);

INSERT INTO user_vin (user_id, vin_num, current_mileage, available_image_urls, selected_image_url)
VALUES (1, '4T1C11AK5LU123456', 45200,
        '["https://images.unsplash.com/photo-1621007947382-bcb49c457f54","https://images.unsplash.com/photo-1609521263047-f8f205293f24"]',
        'https://images.unsplash.com/photo-1621007947382-bcb49c457f54'),
       (1, '2HGFC2F59JH543210', 78500,
        '["https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6","https://images.unsplash.com/photo-1494976388531-d1058494cdd8"]',
        'https://images.unsplash.com/photo-1606664515524-ed2f786a0bd6');

INSERT INTO maint_mileage (maint_mileage_id, vehicle_type_id, mileage_due, maint_desc, is_inspect)
VALUES (1, 1, 5000, 'Change - Engine oil', FALSE),
       (2, 1, 15000, 'Rotate - Wheels & tires', FALSE),
       (3, 1, 30000, 'Inspect - Transmission fluid', TRUE),
       (4, 1, 45000, 'Replace - Spark plugs', FALSE),
       (5, 2, 7500, 'Change - Engine oil', FALSE),
       (6, 2, 30000, 'Inspect - Brakes', TRUE),
       (7, 2, 80000, 'Replace - Timing belt and water pump', FALSE);

INSERT INTO maint_labor_line (maint_labor_line_id, maint_mileage_id, time_required_hours, hourly_rate, total_cost,
                              currency)
VALUES (1, 1, 0.50, 85.00, 42.50, 'USD'),
       (2, 2, 0.25, 85.00, 21.25, 'USD'),
       (3, 3, 0.40, 85.00, 34.00, 'USD'),
       (4, 4, 1.20, 85.00, 102.00, 'USD'),
       (5, 5, 0.50, 90.00, 45.00, 'USD'),
       (6, 6, 0.75, 90.00, 67.50, 'USD'),
       (7, 7, 4.50, 90.00, 405.00, 'USD');

INSERT INTO maint_part_line (maint_part_line_id, maint_mileage_id, part_desc, total_cost, currency)
VALUES (1, 1, 'Change - Engine oil', 35.00, 'USD'),
       (2, 4, 'Replace - Spark plugs', 48.00, 'USD'),
       (3, 7, 'Replace - Timing belt and water pump', 520.00, 'USD');

INSERT INTO maint_mileage_summary (maint_mileage_summary_id, vehicle_type_id, mileage_due, total_parts_cost,
                                   total_labor_cost, total_cost, currency)
VALUES (1, 1, 30000, 0.00, 34.00, 34.00, 'USD'),
       (2, 1, 45000, 48.00, 102.00, 150.00, 'USD'),
       (3, 2, 30000, 0.00, 67.50, 67.50, 'USD'),
       (4, 2, 80000, 520.00, 405.00, 925.00, 'USD');

INSERT INTO vehicle_warranty (vehicle_year, vehicle_make, vehicle_model, fetched_at, coverages)
VALUES ('2020', 'Toyota', 'Camry', CURRENT_TIMESTAMP,
        '{"Warranty - Basic (months/miles)": "36/36,000", "Warranty - Corrosion perforation (months/miles)": "60/ unlimited", "Warranty - Powertrain (months/miles)": "60/60,000", "Warranty - Roadside assistance coverage (months/miles)": "36/36,000"}'),
       ('2018', 'Honda', 'Civic', CURRENT_TIMESTAMP,
        '{"Warranty - Basic (months/miles)": "36/36,000", "Warranty - Powertrain (months/miles)": "60/60,000"}');

INSERT INTO misc_maint_cost (misc_maint_cost_id, vehicle_type_id, maint_title, maint_desc, independent_avg,
                             independent_high, independent_low, dealer_avg, dealer_high, dealer_low)
VALUES (1, 1, 'Oil Change', 'Replace engine oil and filter', 65, 95, 45, 110, 145, 85),
       (2, 1, 'Brake Pad Replacement', 'Replace front brake pads and resurface rotors', 275, 380, 210, 425, 550, 340),
       (3, 2, 'Oil Change', 'Replace engine oil and filter', 60, 90, 40, 105, 135, 80),
       (4, 2, 'Timing Belt', 'Replace timing belt and water pump', 650, 900, 520, 980, 1250, 780);

INSERT INTO recall (recall_id, vehicle_type_id, nhtsa_campaign_number, recall_no, report_received_date, component,
                    summary, consequence, remedy, notes, manufacturer, park_it, park_outside, over_the_air_update,
                    model_year, make, model)
VALUES (1, 1, '23V123000', '23TA01', '2023-03-15', 'FUEL SYSTEM, GASOLINE:DELIVERY:FUEL PUMP',
        'Fuel pump may fail prematurely.',
        'Engine stall while driving increases crash risk.',
        'Dealers will replace the fuel pump free of charge.',
        'Contact your Toyota dealer for scheduling.', 'Toyota', FALSE, FALSE, FALSE, '2020', 'Toyota', 'Camry'),
       (2, 1, '22V456000', '22TA04', '2022-08-01', 'ELECTRICAL SYSTEM:SOFTWARE',
        'Infotainment system may freeze during navigation.',
        'Loss of rear camera display while reversing.',
        'Dealer will update infotainment software.',
        NULL, 'Toyota', FALSE, FALSE, TRUE, '2020', 'Toyota', 'Camry'),
       (3, 2, '21V789000', '21HA02', '2021-11-20', 'AIR BAGS:SENSOR:OCCUPANT CLASSIFICATION',
        'Passenger air bag sensor may misclassify occupant weight.',
        'Air bag may not deploy correctly in a crash.',
        'Dealers will recalibrate the occupant classification system.',
        NULL, 'Honda', FALSE, FALSE, FALSE, '2018', 'Honda', 'Civic');

INSERT INTO completed_maintenance (completed_maintenance_id, user_id, vin_num, maint_mileage_id, completed_date,
                                   mileage_completed, cost, notes)
VALUES (1, 1, '4T1C11AK5LU123456', 1, '2020-06-10', 5100, 77.50, 'First oil change at local shop'),
       (2, 1, '4T1C11AK5LU123456', 2, '2021-02-14', 15200, 56.25, 'Tire rotation during 15k visit'),
       (3, 1, '2HGFC2F59JH543210', 5, '2019-04-22', 7800, 99.00, 'Dealer 7.5k service visit');

INSERT INTO completed_recall (completed_recall_id, user_id, vin_num, recall_id, completed_date, repair_shop, cost,
                              notes)
VALUES (1, 1, '4T1C11AK5LU123456', 2, '2022-09-18', 'City Toyota', 0.00,
        'Software update completed during routine service');

ALTER TABLE user_detail ALTER COLUMN user_id RESTART WITH 2;
ALTER TABLE vehicle_type ALTER COLUMN vehicle_type_id RESTART WITH 3;
ALTER TABLE maint_mileage ALTER COLUMN maint_mileage_id RESTART WITH 8;
ALTER TABLE maint_labor_line ALTER COLUMN maint_labor_line_id RESTART WITH 8;
ALTER TABLE maint_part_line ALTER COLUMN maint_part_line_id RESTART WITH 4;
ALTER TABLE maint_mileage_summary ALTER COLUMN maint_mileage_summary_id RESTART WITH 5;
ALTER TABLE misc_maint_cost ALTER COLUMN misc_maint_cost_id RESTART WITH 5;
ALTER TABLE recall ALTER COLUMN recall_id RESTART WITH 4;
ALTER TABLE completed_maintenance ALTER COLUMN completed_maintenance_id RESTART WITH 4;
ALTER TABLE completed_recall ALTER COLUMN completed_recall_id RESTART WITH 2;
