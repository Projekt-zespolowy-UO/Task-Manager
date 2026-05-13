-- Add foreign key to tasks table
ALTER TABLE tasks
ADD COLUMN category_id BIGINT NOT NULL,
ADD CONSTRAINT fk_category
FOREIGN KEY (category_id)
REFERENCES task_categories(id);