CREATE TABLE business_categories (
    id   bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(30) NOT NULL,
    name varchar(50) NOT NULL,
    CONSTRAINT uk_business_categories_code UNIQUE (code),
    CONSTRAINT ck_business_categories_code_format CHECK (code ~ '^[A-Z]+(_[A-Z]+)*$')
);

INSERT INTO business_categories (code, name) VALUES
    ('BARBER', 'Barber'),
    ('HAIR_SALON', 'Hair salon'),
    ('BEAUTY', 'Beauty'),
    ('NAILS', 'Nails'),
    ('MASSAGE', 'Massage'),
    ('PHYSIOTHERAPY', 'Physiotherapy');

CREATE TABLE businesses (
    id                      uuid          PRIMARY KEY,
    owner_id                uuid          NOT NULL,
    category_id             bigint        NOT NULL,
    name                    varchar(100)  NOT NULL,
    slug                    varchar(120)  NOT NULL,
    description             varchar(2000),
    phone                   varchar(20)   NOT NULL,
    email                   varchar(255),
    street                  varchar(150)  NOT NULL,
    city                    varchar(80)   NOT NULL,
    postal_code             varchar(6)    NOT NULL,
    timezone                varchar(50)   NOT NULL DEFAULT 'Europe/Warsaw',
    free_cancellation_hours int           NOT NULL DEFAULT 24,
    slot_interval_minutes   int           NOT NULL DEFAULT 15,
    active                  boolean       NOT NULL DEFAULT true,
    created_at              timestamptz   NOT NULL,
    created_by              varchar(255)  NOT NULL,
    updated_at              timestamptz   NOT NULL,
    updated_by              varchar(255)  NOT NULL,
    CONSTRAINT uk_businesses_owner UNIQUE (owner_id),
    CONSTRAINT uk_businesses_slug UNIQUE (slug),
    CONSTRAINT fk_businesses_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_businesses_category FOREIGN KEY (category_id) REFERENCES business_categories (id),
    CONSTRAINT ck_businesses_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_businesses_slug_format CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_businesses_free_cancellation_hours CHECK (free_cancellation_hours BETWEEN 0 AND 168),
    CONSTRAINT ck_businesses_slot_interval CHECK (slot_interval_minutes IN (15, 30, 60))
);

CREATE INDEX ix_businesses_city_category ON businesses (city, category_id);
