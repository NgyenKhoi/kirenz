-- Remove duplicate birthday column from profiles table
-- We keep date_of_birth and remove birthday

ALTER TABLE profiles DROP COLUMN IF EXISTS birthday;