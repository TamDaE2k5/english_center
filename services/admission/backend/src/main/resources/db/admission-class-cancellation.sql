-- Admission giữ yêu cầu xử lý; chưa đánh dấu đã tạo refund tại Finance.
CREATE TABLE IF NOT EXISTS admission.class_cancellation_requests (
 id BIGSERIAL PRIMARY KEY,
 enrollment_id INTEGER NOT NULL UNIQUE REFERENCES admission.enrollments(id),
 class_id INTEGER NOT NULL REFERENCES admission.classes(id),
 invoice_code VARCHAR(50),
 reason TEXT NOT NULL,
 requested_by INTEGER REFERENCES admission.users(id),
 status VARCHAR(40) NOT NULL DEFAULT 'NEEDS_FINANCE_REVIEW' CHECK(status IN ('NEEDS_FINANCE_REVIEW','ACKNOWLEDGED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
