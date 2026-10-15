-- Migration V72: Create course_invites and course_invite_activations for student access links
CREATE TABLE IF NOT EXISTS course_invites (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    cohort_id BIGINT REFERENCES cohorts(id) ON DELETE SET NULL,
    max_uses INT NOT NULL DEFAULT 1,
    used_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE,
    note VARCHAR(255),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_course_invites_code ON course_invites(code);
CREATE INDEX IF NOT EXISTS idx_course_invites_course ON course_invites(course_id);

CREATE TABLE IF NOT EXISTS course_invite_activations (
    id BIGSERIAL PRIMARY KEY,
    invite_id BIGINT NOT NULL REFERENCES course_invites(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_course_invite_activation UNIQUE (invite_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_course_invite_activations_user ON course_invite_activations(user_id);
