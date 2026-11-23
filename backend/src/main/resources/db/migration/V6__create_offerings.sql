CREATE TABLE offerings (
    id               uuid          PRIMARY KEY,
    business_id      uuid          NOT NULL,
    name             varchar(100)  NOT NULL,
    description      varchar(1000),
    price_amount     bigint        NOT NULL,
    currency         char(3)       NOT NULL DEFAULT 'PLN',
    duration_minutes int           NOT NULL,
    buffer_minutes   int           NOT NULL DEFAULT 0,
    active           boolean       NOT NULL DEFAULT true,
    created_at       timestamptz   NOT NULL,
    created_by       varchar(255)  NOT NULL,
    updated_at       timestamptz   NOT NULL,
    updated_by       varchar(255)  NOT NULL,
    CONSTRAINT fk_offerings_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT ck_offerings_price_amount CHECK (price_amount >= 0),
    CONSTRAINT ck_offerings_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_offerings_duration CHECK (duration_minutes BETWEEN 5 AND 480 AND duration_minutes % 5 = 0),
    CONSTRAINT ck_offerings_buffer CHECK (buffer_minutes BETWEEN 0 AND 120 AND buffer_minutes % 5 = 0)
);

CREATE UNIQUE INDEX uk_offerings_business_name_active
    ON offerings (business_id, lower(name))
    WHERE active;
