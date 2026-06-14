CREATE TABLE task_statuses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    system_key VARCHAR(50),
    dashboard_id BIGINT NOT NULL,

    CONSTRAINT fk_dashboard
        FOREIGN KEY (dashboard_id)
        REFERENCES dashboard(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_task_statuses_dashboard_id ON task_statuses(dashboard_id);
