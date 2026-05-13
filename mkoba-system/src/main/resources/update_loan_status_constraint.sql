-- Update loans table constraint to include PENDING_ACCOUNTANT_CONFIRMATION
-- First, drop the existing constraint
ALTER TABLE loans DROP CONSTRAINT IF EXISTS loans_status_check;

-- Then, add the updated constraint with all valid statuses
ALTER TABLE loans 
ADD CONSTRAINT loans_status_check 
CHECK (status IN ('PENDING', 'PENDING_ACCOUNTANT_CONFIRMATION', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'DEFAULTED'));

-- Verify the constraint was added
SELECT conname, consrc FROM pg_constraint WHERE conname = 'loans_status_check';
