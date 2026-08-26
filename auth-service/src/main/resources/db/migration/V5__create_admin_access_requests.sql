CREATE TABLE admin_access_requests (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL,
    email               VARCHAR(255) NOT NULL,
    first_name          VARCHAR(255),
    last_name           VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approve_token_hash  VARCHAR(255) NOT NULL UNIQUE,
    reject_token_hash   VARCHAR(255) NOT NULL UNIQUE,
    expires_at          TIMESTAMPTZ NOT NULL,
    decided_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_access_requests_user_id ON admin_access_requests (user_id);
