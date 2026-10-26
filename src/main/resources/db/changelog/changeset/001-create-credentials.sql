-- liquibase formatted sql

-- changeset PC:1792563996999-1
CREATE TABLE credentials
(
    id         BIGSERIAL                   PRIMARY KEY ,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    BIGINT                      NOT NULL UNIQUE ,
    username   VARCHAR(255)                NOT NULL UNIQUE ,
    password   VARCHAR(255)                NOT NULL,
    role       VARCHAR(255)                NOT NULL,
    CONSTRAINT uc_credentials_user         UNIQUE (user_id),
    CONSTRAINT uc_credentials_username     UNIQUE (username),
    CONSTRAINT chk_credentials_role        CHECK (role IN ('ADMIN', 'USER'))
);


