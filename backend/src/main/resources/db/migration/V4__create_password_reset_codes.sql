CREATE TABLE password_reset_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_password_reset_codes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_password_reset_codes_active_user
    ON password_reset_codes(user_id)
    WHERE used_at IS NULL;

CREATE INDEX idx_password_reset_codes_user_created_at
    ON password_reset_codes(user_id, created_at);

CREATE INDEX idx_password_reset_codes_expires_at
    ON password_reset_codes(expires_at);
