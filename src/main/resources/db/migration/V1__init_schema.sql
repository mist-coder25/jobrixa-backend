-- USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    avatar_url VARCHAR(500),
    college VARCHAR(200),
    graduation_year SMALLINT,
    target_ctc_min INTEGER,
    target_ctc_max INTEGER,
    linkedin_url VARCHAR(500),
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_campus_mode BOOLEAN DEFAULT FALSE,
    gmail_connected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- USER SKILLS
CREATE TABLE user_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    skill_name VARCHAR(100) NOT NULL
);

-- COMPANIES
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(255),
    logo_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    industry VARCHAR(100),
    trust_score SMALLINT,
    trust_last_checked TIMESTAMP WITH TIME ZONE,
    UNIQUE(domain)
);

-- RESUMES
CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(100) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    version VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- JOB APPLICATIONS (Core table)
CREATE TABLE job_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID REFERENCES companies(id),
    company_name VARCHAR(255) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    job_url VARCHAR(1000),
    source VARCHAR(50),            -- linkedin / internshala / referral / campus / manual
    status VARCHAR(50) NOT NULL,   -- saved / applied / oa / interview / offer / rejected / ghosted
    priority VARCHAR(20),          -- low / medium / high / dream
    applied_at DATE,
    next_action_date DATE,
    salary_min INTEGER,
    salary_max INTEGER,
    location VARCHAR(200),
    is_remote BOOLEAN DEFAULT FALSE,
    job_description TEXT,
    resume_id UUID REFERENCES resumes(id),
    trust_score SMALLINT,
    has_bond_warning BOOLEAN DEFAULT FALSE,
    bond_details TEXT,
    has_payment_warning BOOLEAN DEFAULT FALSE,
    tags VARCHAR(255) ARRAY,       -- H2 mode fallback: use string array
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- APPLICATION TIMELINE (Activity Log)
CREATE TABLE application_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID REFERENCES job_applications(id) ON DELETE CASCADE,
    event_type VARCHAR(50),        -- status_change / note_added / email_detected / reminder_set
    description TEXT NOT NULL,
    old_value VARCHAR(100),
    new_value VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- APPLICATION NOTES
CREATE TABLE application_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID REFERENCES job_applications(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- INTERVIEW STAGES
CREATE TABLE interview_stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID REFERENCES job_applications(id) ON DELETE CASCADE,
    stage_name VARCHAR(100) NOT NULL,  -- OA / Technical / HR / System Design
    scheduled_at TIMESTAMP WITH TIME ZONE,
    duration_minutes INTEGER,
    result VARCHAR(50),                -- pending / passed / failed
    notes TEXT,
    questions_asked TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- COMMUNITY INTERVIEW EXPERIENCES
CREATE TABLE interview_experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    company_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    round_type VARCHAR(100),
    difficulty SMALLINT,           -- 1-5
    questions_asked TEXT,
    tips TEXT,
    outcome VARCHAR(50),           -- selected / rejected / pending
    is_anonymous BOOLEAN DEFAULT TRUE,
    interview_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- NOTIFICATIONS
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    application_id UUID REFERENCES job_applications(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),              -- reminder / followup / status_update / system
    is_read BOOLEAN DEFAULT FALSE,
    scheduled_for TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- REFRESH TOKENS
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_applications_user_id ON job_applications(user_id);
CREATE INDEX idx_applications_status ON job_applications(status);
CREATE INDEX idx_events_application_id ON application_events(application_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id, is_read);
