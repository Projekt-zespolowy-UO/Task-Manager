UPDATE users
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN password SET NOT NULL;

CREATE UNIQUE INDEX ux_users_email ON users(email);
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_category_id ON tasks(category_id);
