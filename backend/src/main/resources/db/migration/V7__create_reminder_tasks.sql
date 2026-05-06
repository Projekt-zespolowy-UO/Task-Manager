CREATE TABLE reminder_tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    reminder_time TIMESTAMP NOT NULL,
    completed BOOLEAN NOT NULL
);