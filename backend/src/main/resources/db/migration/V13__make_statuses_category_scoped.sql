-- Add category_id to task_statuses to make statuses per-category instead of per-dashboard
ALTER TABLE task_statuses
ADD COLUMN category_id BIGINT;

-- Add foreign key constraint to categories
ALTER TABLE task_statuses
ADD CONSTRAINT fk_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id)
    ON DELETE CASCADE;

-- Create index for category_id lookups
CREATE INDEX idx_task_statuses_category_id ON task_statuses(category_id);

-- Drop the old dashboard constraint since statuses are now category-scoped
ALTER TABLE task_statuses
DROP CONSTRAINT fk_dashboard;

-- Drop the old dashboard_id column
ALTER TABLE task_statuses
DROP COLUMN dashboard_id;

-- Drop the old index
DROP INDEX IF EXISTS idx_task_statuses_dashboard_id;
