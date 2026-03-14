-- Add total_applications_created to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS total_applications_created INTEGER NOT NULL DEFAULT 0;

-- Add missing columns to job_applications table
ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS deadline TIMESTAMP;
ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS event_date TIMESTAMP;

-- Backfill existing users with their lifetime application count
UPDATE users u
SET total_applications_created = (
    SELECT COUNT(*) FROM job_applications ja WHERE ja.user_id = u.id
)
WHERE total_applications_created = 0;
