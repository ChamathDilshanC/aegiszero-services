CREATE TABLE recovery_codes (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    code_hash  VARCHAR(255) NOT NULL UNIQUE,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recovery_codes_user_id ON recovery_codes (user_id);
