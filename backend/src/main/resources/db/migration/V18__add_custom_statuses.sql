CREATE TABLE IF NOT EXISTS custom_statuses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_custom_status_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_custom_status_user_name
        UNIQUE (user_id, name)
);

CREATE INDEX IF NOT EXISTS idx_custom_statuses_user_position
    ON custom_statuses(user_id, position, id);

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS custom_status_id BIGINT;

ALTER TABLE tasks
    ADD CONSTRAINT fk_task_custom_status
        FOREIGN KEY (custom_status_id)
        REFERENCES custom_statuses(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_tasks_custom_status
    ON tasks(custom_status_id);
