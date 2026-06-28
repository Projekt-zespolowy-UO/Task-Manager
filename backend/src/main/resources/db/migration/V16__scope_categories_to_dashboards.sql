-- Ensure every existing user has a workspace before categories become scoped.
INSERT INTO dashboard (name, user_id)
SELECT 'My Dashboard', u.id
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM dashboard d
    WHERE d.user_id = u.id
);

INSERT INTO dashboard_members (dashboard_id, user_id, role, created_at)
SELECT d.id, d.user_id, 'OWNER', CURRENT_TIMESTAMP
FROM dashboard d
ON CONFLICT (dashboard_id, user_id) DO NOTHING;

ALTER TABLE categories
    ADD COLUMN dashboard_id BIGINT;

UPDATE categories c
SET dashboard_id = (
    SELECT MIN(d.id)
    FROM dashboard d
    WHERE d.user_id = c.user_id
)
WHERE c.user_id IS NOT NULL;

-- Legacy global categories cannot be assigned safely to a single workspace.
-- Tasks keep working because the existing foreign key uses ON DELETE SET NULL.
DELETE FROM categories
WHERE dashboard_id IS NULL;

ALTER TABLE categories
    ALTER COLUMN dashboard_id SET NOT NULL;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_dashboard
        FOREIGN KEY (dashboard_id)
        REFERENCES dashboard(id)
        ON DELETE CASCADE;

CREATE INDEX idx_categories_dashboard_id ON categories(dashboard_id);
