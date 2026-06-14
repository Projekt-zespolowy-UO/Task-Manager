-- Add status_id column to tasks table and establish relationship with task_statuses
ALTER TABLE tasks
    ADD COLUMN status_id BIGINT;

-- Add foreign key constraint
ALTER TABLE tasks
    ADD CONSTRAINT fk_task_status
        FOREIGN KEY (status_id)
        REFERENCES task_statuses(id)
        ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX idx_tasks_status_id ON tasks(status_id);

-- Drop the old status VARCHAR column to avoid confusion
ALTER TABLE tasks
    DROP COLUMN IF EXISTS status;
