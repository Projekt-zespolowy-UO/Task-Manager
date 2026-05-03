UPDATE users
SET email = LOWER(TRIM(email))
WHERE email IS NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        WHERE username IS NULL
           OR email IS NULL
           OR password IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot enforce NOT NULL on users: username, email, or password contains NULL values';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM users
        GROUP BY email
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot create unique users.email index: duplicate normalized email values exist';
    END IF;
END $$;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN password SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_category_id ON tasks(category_id);
