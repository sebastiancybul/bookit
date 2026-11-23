CREATE TABLE employees (
    id           uuid          PRIMARY KEY,
    business_id  uuid          NOT NULL,
    user_id      uuid,
    display_name varchar(80)   NOT NULL,
    position     varchar(80),
    active       boolean       NOT NULL DEFAULT true,
    created_at   timestamptz   NOT NULL,
    created_by   varchar(255)  NOT NULL,
    updated_at   timestamptz   NOT NULL,
    updated_by   varchar(255)  NOT NULL,
    CONSTRAINT fk_employees_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_employees_user UNIQUE (user_id),
    CONSTRAINT ck_employees_inactive_unlinked CHECK (active OR user_id IS NULL)
);

CREATE UNIQUE INDEX uk_employees_business_display_name_active
    ON employees (business_id, lower(display_name))
    WHERE active;

CREATE TABLE employee_offerings (
    employee_id uuid NOT NULL,
    offering_id uuid NOT NULL,
    CONSTRAINT pk_employee_offerings PRIMARY KEY (employee_id, offering_id),
    CONSTRAINT fk_employee_offerings_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_employee_offerings_offering FOREIGN KEY (offering_id) REFERENCES offerings (id)
);

CREATE INDEX ix_employee_offerings_offering ON employee_offerings (offering_id);
