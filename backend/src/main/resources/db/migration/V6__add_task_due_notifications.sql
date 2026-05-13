ALTER TABLE tasks
    ADD COLUMN due_notification_sent_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tasks_due_notifications
    ON tasks(deadline, due_notification_sent_at, user_id);
