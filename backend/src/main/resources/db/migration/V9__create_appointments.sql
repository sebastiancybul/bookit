CREATE TABLE appointments (
    id              uuid          PRIMARY KEY,
    business_id     uuid          NOT NULL,
    employee_id     uuid          NOT NULL,
    offering_id     uuid          NOT NULL,
    client_id       uuid          NOT NULL,
    start_time      timestamptz   NOT NULL,
    end_time        timestamptz   NOT NULL,
    blocked_until   timestamptz   NOT NULL,
    status          varchar(20)   NOT NULL,
    offering_name   varchar(100)  NOT NULL,
    price_amount    bigint        NOT NULL,
    currency        char(3)       NOT NULL,
    client_note     varchar(500),
    idempotency_key uuid          NOT NULL,
    cancelled_at    timestamptz,
    cancelled_by    varchar(20),
    version         bigint        NOT NULL DEFAULT 0,
    created_at      timestamptz   NOT NULL,
    created_by      varchar(255)  NOT NULL,
    updated_at      timestamptz   NOT NULL,
    updated_by      varchar(255)  NOT NULL,
    CONSTRAINT fk_appointments_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_appointments_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_appointments_offering FOREIGN KEY (offering_id) REFERENCES offerings (id),
    CONSTRAINT fk_appointments_client FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT uk_appointments_client_idempotency_key UNIQUE (client_id, idempotency_key),
    CONSTRAINT ck_appointments_time_order CHECK (start_time < end_time AND end_time <= blocked_until),
    CONSTRAINT ck_appointments_status CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT ck_appointments_price_amount CHECK (price_amount >= 0),
    CONSTRAINT ck_appointments_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_appointments_cancelled_by CHECK (cancelled_by IN ('CLIENT', 'BUSINESS')),
    CONSTRAINT ck_appointments_cancellation CHECK (
        (status = 'CANCELLED') = (cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
    ),
    CONSTRAINT ex_appointments_employee_overlap EXCLUDE USING gist (
        employee_id WITH =,
        tstzrange(start_time, blocked_until, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED'),
    CONSTRAINT ex_appointments_client_overlap EXCLUDE USING gist (
        client_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX ix_appointments_employee_start ON appointments (employee_id, start_time);
CREATE INDEX ix_appointments_business_start ON appointments (business_id, start_time);
CREATE INDEX ix_appointments_client_start ON appointments (client_id, start_time);
