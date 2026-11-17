CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(255) NOT NULL,
    password_hash varchar(100) NOT NULL,
    first_name    varchar(50)  NOT NULL,
    last_name     varchar(50)  NOT NULL,
    phone         varchar(20)  NOT NULL,
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL,
    created_by    varchar(255) NOT NULL,
    updated_at    timestamptz  NOT NULL,
    updated_by    varchar(255) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE roles (
    id   bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(50) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    user_id uuid   NOT NULL,
    role_id bigint NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

INSERT INTO roles (name) VALUES
    ('ROLE_CLIENT'),
    ('ROLE_EMPLOYEE'),
    ('ROLE_OWNER'),
    ('ROLE_ADMIN');
