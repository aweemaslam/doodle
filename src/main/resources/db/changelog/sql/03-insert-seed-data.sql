-- liquibase formatted sql

-- changeset aweem:insert-dummy-users splitStatements:false runOnChange:false
-- comment: Inserting 5 dummy users for local development and testing.
-- rollback DELETE FROM users WHERE email IN ('alice.smith@example.com', 'bob.jones@example.com', 'charlie.brown@example.com', 'diana.prince@example.com', 'ethan.hunt@example.com');
INSERT INTO users (email, full_name, default_timezone, created_at, updated_at, is_active)
VALUES
    ('alice.smith@example.com', 'Alice Smith', 'Europe/London', NOW(), NOW(), true),
    ('bob.jones@example.com', 'Bob Jones', 'America/New_York', NOW(), NOW(), true),
    ('charlie.brown@example.com', 'Charlie Brown', 'Asia/Tokyo', NOW(), NOW(), true),
    ('diana.prince@example.com', 'Diana Prince', 'Europe/Berlin', NOW(), NOW(), true),
    ('ethan.hunt@example.com', 'Ethan Hunt', 'UTC', NOW(), NOW(), true);