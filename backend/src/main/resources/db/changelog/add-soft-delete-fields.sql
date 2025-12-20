-- liquibase formatted sql

-- changeset kiro:add-soft-delete-to-users
ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;

-- changeset kiro:add-soft-delete-to-profiles
ALTER TABLE profiles ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE profiles ADD COLUMN deleted_at TIMESTAMP;
