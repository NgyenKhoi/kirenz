-- liquibase formatted sql

-- changeset kiro:add-last-seen-to-users
ALTER TABLE users ADD COLUMN last_seen TIMESTAMP;

COMMENT ON COLUMN users.last_seen IS 'Last time the user was seen online';
