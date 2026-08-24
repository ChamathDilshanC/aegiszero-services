CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY,
    event_type     VARCHAR(100) NOT NULL,
    actor_id       VARCHAR(255),
    target_id      VARCHAR(255),
    ip_address     VARCHAR(64),
    device_id      VARCHAR(255),
    correlation_id VARCHAR(255),
    occurred_at    TIMESTAMPTZ NOT NULL,
    metadata       TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_event_type ON audit_logs (event_type);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs (actor_id);
CREATE INDEX idx_audit_logs_target_id ON audit_logs (target_id);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at);
