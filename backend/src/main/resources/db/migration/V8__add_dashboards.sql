CREATE TABLE IF NOT EXISTS dashboard (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_dashboard_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS dashboard_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_task_dashboard'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_task_dashboard
            FOREIGN KEY (dashboard_id) REFERENCES dashboard(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_dashboard_user_id ON dashboard(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_dashboard_id ON tasks(dashboard_id);
