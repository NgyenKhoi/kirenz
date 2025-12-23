--liquibase formatted sql

--changeset profile-fields:1
ALTER TABLE profiles 
ADD COLUMN location VARCHAR(255),
ADD COLUMN website VARCHAR(255),
ADD COLUMN date_of_birth DATE,
ADD COLUMN created_at TIMESTAMP DEFAULT now();

--rollback ALTER TABLE profiles DROP COLUMN location, DROP COLUMN website, DROP COLUMN date_of_birth, DROP COLUMN created_at;