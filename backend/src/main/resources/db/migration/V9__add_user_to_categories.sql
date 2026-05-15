ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_categories_user'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT fk_categories_user
            FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories(user_id);
