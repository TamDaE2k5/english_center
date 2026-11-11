-- Yêu cầu xác nhận, không phải phiếu thu SUCCESS hoặc bằng chứng Finance đã nhận tiền.
BEGIN;
CREATE TABLE IF NOT EXISTS admission.cash_confirmation_requests (
 id BIGSERIAL PRIMARY KEY,
 enrollment_id INTEGER NOT NULL REFERENCES admission.enrollments(id),
 invoice_code VARCHAR(50) NOT NULL,
 receipt_number VARCHAR(80) NOT NULL UNIQUE,
 amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
 requested_by INTEGER NOT NULL REFERENCES admission.users(id),
 requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 notes VARCHAR(2000),
 status VARCHAR(40) NOT NULL DEFAULT 'NEEDS_FINANCE_CONFIRMATION'
   CHECK (status IN ('NEEDS_FINANCE_CONFIRMATION')),
 UNIQUE (enrollment_id, invoice_code)
);
-- Tách audit nghiệp vụ khỏi auth_audit_logs để không thay constraint/migration audit cũ.
-- Không FK enrollment_id: phải lưu được cả lần gửi ID không tồn tại.
CREATE TABLE IF NOT EXISTS admission.cash_confirmation_audit (
 id BIGSERIAL PRIMARY KEY,
 enrollment_id INTEGER,
 actor_id INTEGER REFERENCES admission.users(id),
 occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 outcome VARCHAR(20) NOT NULL CHECK (outcome IN ('REQUESTED','REPLAYED','REJECTED')),
 reason_code VARCHAR(80) NOT NULL,
 receipt_number VARCHAR(80),
 amount_text VARCHAR(80)
);
CREATE INDEX IF NOT EXISTS idx_cash_audit_enrollment ON admission.cash_confirmation_audit(enrollment_id,occurred_at);
COMMIT;
