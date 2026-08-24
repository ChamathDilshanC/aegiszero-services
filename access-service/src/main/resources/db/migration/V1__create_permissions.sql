CREATE TABLE permissions (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
