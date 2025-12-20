-- liquibase formatted sql

-- changeset hoahtm:seed-data
INSERT INTO users (email, password_hash, created_at, updated_at) VALUES
('sarah.johnson@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NOW() - INTERVAL '6 months', NOW()),
('mike.chen@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NOW() - INTERVAL '4 months', NOW()),
('emma.wilson@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', NOW() - INTERVAL '3 months', NOW());

-- Insert sample profiles
INSERT INTO profiles (user_id, full_name, avatar_url, bio, birthday, updated_at) VALUES
(1, 'Sarah Johnson', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Sarah', 'Digital artist & designer 🎨 Creating beautiful things', '1995-03-15', NOW()),
(2, 'Mike Chen', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Mike', 'Software engineer | Coffee enthusiast ☕', '1992-07-22', NOW()),
(3, 'Emma Wilson', 'https://api.dicebear.com/7.x/avataaars/svg?seed=Emma', 'Travel photographer 📸 Capturing moments around the world', '1998-11-08', NOW());
