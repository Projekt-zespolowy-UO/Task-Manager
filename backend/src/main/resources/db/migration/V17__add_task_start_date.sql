ALTER TABLE tasks
    ADD COLUMN start_date DATE;

UPDATE tasks
SET start_date = deadline
WHERE start_date IS NULL
  AND deadline IS NOT NULL;
