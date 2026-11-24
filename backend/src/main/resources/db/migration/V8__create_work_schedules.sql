CREATE TABLE work_schedules (
    id          uuid          PRIMARY KEY,
    employee_id uuid          NOT NULL,
    version     bigint        NOT NULL DEFAULT 0,
    created_at  timestamptz   NOT NULL,
    created_by  varchar(255)  NOT NULL,
    updated_at  timestamptz   NOT NULL,
    updated_by  varchar(255)  NOT NULL,
    CONSTRAINT fk_work_schedules_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT uk_work_schedules_employee UNIQUE (employee_id)
);

CREATE TABLE work_schedule_intervals (
    work_schedule_id uuid      NOT NULL,
    day_of_week      smallint  NOT NULL,
    start_time       time      NOT NULL,
    end_time         time      NOT NULL,
    CONSTRAINT pk_work_schedule_intervals PRIMARY KEY (work_schedule_id, day_of_week, start_time),
    CONSTRAINT fk_work_schedule_intervals_schedule FOREIGN KEY (work_schedule_id) REFERENCES work_schedules (id),
    CONSTRAINT ck_work_schedule_intervals_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_work_schedule_intervals_time_range CHECK (start_time < end_time)
);
