ALTER TABLE user_detail
    ADD CONSTRAINT uk_user_detail_user_email UNIQUE (user_email);