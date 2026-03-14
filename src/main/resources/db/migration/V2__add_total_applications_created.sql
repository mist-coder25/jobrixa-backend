ALTER TABLE users ADD COLUMN IF NOT EXISTS total_applications_created INTEGER NOT NULL DEFAULT 0;

UPDATE users u
SET total_applications_created = (
    SELECT COUNT(*) FROM job_applications ja WHERE ja.user_id = u.id
)
WHERE total_applications_created = 0 OR total_applications_created IS NULL;
