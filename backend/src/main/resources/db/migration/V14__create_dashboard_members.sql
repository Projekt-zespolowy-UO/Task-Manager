-- Create dashboard_members table
CREATE TABLE IF NOT EXISTS dashboard_members (
    id BIGSERIAL PRIMARY KEY,
    dashboard_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_dashboard_members_dashboard
        FOREIGN KEY (dashboard_id) REFERENCES dashboard(id) ON DELETE CASCADE,
    CONSTRAINT fk_dashboard_members_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_dashboard_user
        UNIQUE (dashboard_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_dashboard_members_dashboard_id ON dashboard_members(dashboard_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_members_user_id ON dashboard_members(user_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_members_role ON dashboard_members(role);

-- Migrate existing dashboard owners to dashboard_members
INSERT INTO dashboard_members (dashboard_id, user_id, role, created_at)
SELECT id, user_id, 'OWNER', NOW()
FROM dashboard
ON CONFLICT (dashboard_id, user_id) DO NOTHING;
