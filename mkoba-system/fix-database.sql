-- Fix database schema issues
-- Update existing users to have status
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;

-- Add status column if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) 
NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE'));

-- Verify the fix
SELECT COUNT(*) as total_users, 
       COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_users
FROM users;
