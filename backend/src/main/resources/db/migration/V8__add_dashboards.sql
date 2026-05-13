CREATE TABLE dashboard (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE tasks
ADD COLUMN dashboard_id BIGINT;

ALTER TABLE tasks
ADD CONSTRAINT fk_task_dashboard
FOREIGN KEY (dashboard_id) REFERENCES dashboard(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_dashboard_user_id ON dashboard(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_dashboard_id ON tasks(dashboard_id);
