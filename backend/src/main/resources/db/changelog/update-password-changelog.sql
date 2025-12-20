-- liquibase formatted sql

--changeset hoahtm:update-password

UPDATE users 
SET password_hash = '$2a$12$VBw0HYZ41OxnjnUKqZ9GyuGEQSwHfFqPC9K0hqMVTX.vXf87slAyK'
WHERE email IN (
    'sarah.johnson@example.com',
    'mike.chen@example.com', 
    'emma.wilson@example.com',
    'khoi@gmail.com'
);

-- Verify the update
SELECT id, email, 
       CASE 
           WHEN password_hash = '$2a$12$VBw0HYZ41OxnjnUKqZ9GyuGEQSwHfFqPC9K0hqMVTX.vXf87slAyK' 
           THEN 'password123' 
           ELSE 'other' 
       END as password_status
FROM users;
