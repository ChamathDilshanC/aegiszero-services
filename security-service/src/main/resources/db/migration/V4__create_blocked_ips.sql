CREATE TABLE blocked_ips (
    id         UUID PRIMARY KEY,
    ip_address VARCHAR(64) NOT NULL UNIQUE,
    reason     VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
