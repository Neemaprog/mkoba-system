-- Update existing database to make group_id nullable for admin users
ALTER TABLE users ALTER COLUMN group_id DROP NOT NULL;

-- Update the role constraint to include ADMIN
ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'CHAIRPERSON', 'MEMBER', 'SECRETARY', 'TREASURER'));
