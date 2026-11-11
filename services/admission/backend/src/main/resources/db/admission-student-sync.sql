-- Outbox trong schema Admission; không truy cập schema Student.
BEGIN;
CREATE TABLE IF NOT EXISTS admission.student_sync_outbox (
 user_id INTEGER PRIMARY KEY REFERENCES admission.users(id),
 enrollment_id INTEGER NOT NULL REFERENCES admission.enrollments(id),
 full_name TEXT NOT NULL, email TEXT NOT NULL, phone TEXT, date_of_birth DATE,
 state VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','IN_FLIGHT','RETRY','SUCCEEDED','REVIEW','EXHAUSTED','CANCELLED')),
 attempts INTEGER NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 5),
 next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 lease_until TIMESTAMPTZ, claim_token VARCHAR(36),
 student_profile_id INTEGER, last_error VARCHAR(80),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_student_sync_due ON admission.student_sync_outbox(next_attempt_at) WHERE state IN ('PENDING','RETRY');
-- FAILED legacy có thể đã được Student nhận: giữ đối soát, không tự gửi lại.
INSERT INTO admission.student_sync_outbox(user_id,enrollment_id,full_name,email,phone,date_of_birth,state,last_error)
 SELECT DISTINCT ON (u.id) u.id,e.id,u.full_name,u.email,u.phone,u.date_of_birth,'REVIEW','LEGACY_UNCONFIRMED'
 FROM admission.enrollments e JOIN admission.users u ON u.id=e.student_id
 WHERE e.status='APPROVED' AND NOT COALESCE(e.is_synced_to_student,FALSE)
 ORDER BY u.id,e.id ON CONFLICT DO NOTHING;
COMMIT;
