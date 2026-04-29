ALTER TABLE user_vin
    ADD COLUMN IF NOT EXISTS available_image_urls text NOT NULL DEFAULT '[]';

ALTER TABLE user_vin
    ADD COLUMN IF NOT EXISTS selected_image_url varchar(500);
