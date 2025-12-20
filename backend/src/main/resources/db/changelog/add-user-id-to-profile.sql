-- liquibase formatted sql

-- changeset kiro:add-user-id-to-profile-1
ALTER TABLE profiles
ADD COLUMN user_id BIGINT;

-- changeset kiro:add-user-id-to-profile-2
ALTER TABLE profiles
ADD CONSTRAINT fk_profiles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- changeset kiro:add-user-id-to-profile-3
ALTER TABLE profiles
ADD CONSTRAINT uk_profiles_user_id UNIQUE (user_id);
