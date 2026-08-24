CREATE TABLE user_roles (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    role_id     UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
