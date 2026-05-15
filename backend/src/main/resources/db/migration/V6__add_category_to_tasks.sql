-- tasks.category_id already exists in V1__init_tables.sql and points to categories.
-- Keep this migration as a no-op so Flyway history stays stable without trying to
-- recreate the same column/constraint on fresh databases.
SELECT 1;
