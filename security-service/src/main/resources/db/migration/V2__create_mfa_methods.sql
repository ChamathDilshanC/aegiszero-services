CREATE TABLE mfa_methods (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    type       VARCHAR(30) NOT NULL,
    secret     VARCHAR(255),
    enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, type)
);

CREATE INDEX idx_mfa_methods_user_id ON mfa_methods (user_id);
