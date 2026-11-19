ALTER TABLE businesses
    ADD COLUMN public_id varchar(8) NOT NULL,
    ADD CONSTRAINT uk_businesses_public_id UNIQUE (public_id),
    ADD CONSTRAINT ck_businesses_public_id_format CHECK (public_id ~ '^[a-z0-9]{8}$');
