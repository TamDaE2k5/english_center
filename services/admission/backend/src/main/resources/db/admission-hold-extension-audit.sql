-- Nhật ký riêng Admission; không thay chính sách hóa đơn hoặc dữ liệu cũ.
BEGIN;
CREATE TABLE IF NOT EXISTS admission.hold_extension_audit (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id INTEGER,
    actor_id INTEGER,
    outcome VARCHAR(20) NOT NULL CHECK (outcome IN ('EXTENDED', 'REJECTED')),
    reason_code VARCHAR(80) NOT NULL,
    extended_hours INTEGER,
    previous_deadline TIMESTAMPTZ,
    new_deadline TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMIT;
