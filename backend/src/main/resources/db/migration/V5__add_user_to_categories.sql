ALTER TABLE categories
    ADD COLUMN user_id BIGINT;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories(user_id);
