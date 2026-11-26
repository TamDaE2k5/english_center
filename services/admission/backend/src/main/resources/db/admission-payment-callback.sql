-- Chỉ receipt/audit Admission, không tạo bảng hóa đơn hoặc truy vấn schema Finance.
BEGIN;
ALTER TABLE admission.processed_finance_events ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(64);
CREATE TABLE IF NOT EXISTS admission.finance_callback_audit (
 id BIGSERIAL PRIMARY KEY, event_id VARCHAR(255), enrollment_id INTEGER,
 reason VARCHAR(80) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMIT;
