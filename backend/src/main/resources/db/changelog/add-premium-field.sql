--liquibase formatted sql

--changeset kiro:add-premium-field
ALTER TABLE users ADD COLUMN is_premium BOOLEAN DEFAULT FALSE;

--rollback ALTER TABLE users DROP COLUMN is_premium;
