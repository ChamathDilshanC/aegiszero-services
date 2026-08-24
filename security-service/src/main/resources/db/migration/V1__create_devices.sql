CREATE TABLE devices (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL,
    fingerprint  VARCHAR(255) NOT NULL,
    device_name  VARCHAR(255),
    trusted      BOOLEAN NOT NULL DEFAULT FALSE,
    blocked      BOOLEAN NOT NULL DEFAULT FALSE,
    last_ip      VARCHAR(64),
    last_seen_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, fingerprint)
);

CREATE INDEX idx_devices_user_id ON devices (user_id);
