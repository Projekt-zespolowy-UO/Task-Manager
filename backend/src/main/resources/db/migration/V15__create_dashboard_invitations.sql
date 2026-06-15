-- Create dashboard_invitations table
CREATE TABLE IF NOT EXISTS dashboard_invitations (
    id BIGSERIAL PRIMARY KEY,
    dashboard_id BIGINT NOT NULL,
    invited_user_id BIGINT NOT NULL,
    invited_by_user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_dashboard_invitations_dashboard
        FOREIGN KEY (dashboard_id) REFERENCES dashboard(id) ON DELETE CASCADE,
    CONSTRAINT fk_dashboard_invitations_invited_user
        FOREIGN KEY (invited_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_dashboard_invitations_invited_by_user
        FOREIGN KEY (invited_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dashboard_invitations_dashboard_id ON dashboard_invitations(dashboard_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_invitations_invited_user_id ON dashboard_invitations(invited_user_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_invitations_invited_by_user_id ON dashboard_invitations(invited_by_user_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_invitations_status ON dashboard_invitations(status);
