-- ============================================================================
-- FILE: init-db.sql
-- PROJECT: English Center Management System
-- DESCRIPTION: Khởi tạo toàn bộ CSDL PostgreSQL với 4 Schemas độc lập.
-- ARCHITECT: Leader (Generated from all module docs)
-- VERSION: 2.0 (Master Schema - Full DDL)
-- DATE: 2026-11-08
--
-- NGUYÊN TẮC TUYỆT ĐỐI:
-- 1. KHÔNG có Physical FK chéo schema (admission / student / learning / finance).
-- 2. Tất cả liên kết liên-service là Logical FK (INTEGER) — chỉ lưu ID, không REFERENCES.
-- 3. Mỗi service chỉ truy cập schema của chính mình.
-- 4. Giao tiếp liên-service qua REST HTTP, không JOIN chéo DB.
-- ============================================================================

-- ============================================================================
-- BƯỚC 0: TẠO DATABASE (chạy với quyền superuser trước khi chạy file này)
-- ============================================================================
-- CREATE DATABASE english_center WITH ENCODING='UTF8' LC_COLLATE='en_US.UTF-8' LC_CTYPE='en_US.UTF-8';
-- \c english_center

-- ============================================================================
-- BƯỚC 1: TẠO 4 SCHEMAS & HÀM TRIGGER DÙNG CHUNG
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS admission;
CREATE SCHEMA IF NOT EXISTS student;
CREATE SCHEMA IF NOT EXISTS learning;
CREATE SCHEMA IF NOT EXISTS finance;

-- Hàm trigger tự động cập nhật cột updated_at khi bản ghi thay đổi (TASK-ADM-01 BaseEntity)
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================================
-- SCHEMA 1: ADMISSION
-- SERVICE: admission-service (Java Spring Boot, Port 8081)
-- CHỨC NĂNG: Xác thực định danh, Tuyển sinh, Khảo thí, Quản lý phòng/lớp/khóa học
-- BẢNG: 14 bảng (12 gốc + test_attempts + processed_finance_events) + triggers + indexes + seed data
-- ============================================================================

-- ---- 1.1. Bảng Người dùng & Phân quyền (Users & IAM) ----
CREATE TABLE IF NOT EXISTS admission.users (
    id                  SERIAL PRIMARY KEY,
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    role                VARCHAR(50)  NOT NULL CHECK (role IN ('ADMIN', 'STAFF', 'TEACHER', 'STUDENT')),
    phone               VARCHAR(50),
    gender              VARCHAR(20)  CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    date_of_birth       DATE,
    avatar_url          TEXT,
    must_change_password BOOLEAN     NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified_at   TIMESTAMPTZ,                       -- v3.6: xác thực email khi đăng ký học viên
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  admission.users IS 'Tài khoản hệ thống: ADMIN, STAFF, TEACHER, STUDENT';
COMMENT ON COLUMN admission.users.must_change_password IS 'TRUE khi tài khoản mới được tạo bởi ADMIN; bắt buộc đổi mật khẩu lần đầu đăng nhập';

-- ---- 1.2. Bảng Refresh Token (Phiên đăng nhập) ----
CREATE TABLE IF NOT EXISTS admission.refresh_tokens (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES admission.users(id) ON DELETE CASCADE,
    token       VARCHAR(512) UNIQUE NOT NULL,
    token_hash  VARCHAR(512) UNIQUE,                       -- Tương thích TASK-ADM-01 / TASK-ADM-13 (đồng bộ tự động với token)
    expires_at  TIMESTAMPTZ NOT NULL,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.refresh_tokens IS 'JWT Refresh Token – hỗ trợ Refresh Token Rotation, thu hồi chủ động khi logout';

-- ---- 1.3. Bảng Password Reset Token ----
CREATE TABLE IF NOT EXISTS admission.password_reset_tokens (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES admission.users(id) ON DELETE CASCADE,
    token       VARCHAR(512) UNIQUE NOT NULL,
    token_hash  VARCHAR(512) UNIQUE,                       -- Tương thích TASK-ADM-01 / TASK-ADM-13 (đồng bộ tự động với token)
    expires_at  TIMESTAMPTZ NOT NULL,
    is_used     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.password_reset_tokens IS 'Token đặt lại mật khẩu qua email, hết hạn sau 15 phút';

-- Trigger đồng bộ 2 chiều giữa token và token_hash cho refresh_tokens & password_reset_tokens
CREATE OR REPLACE FUNCTION admission.sync_token_and_hash()
RETURNS TRIGGER AS $$
BEGIN
    NEW.token      := COALESCE(NEW.token, NEW.token_hash);
    NEW.token_hash := COALESCE(NEW.token_hash, NEW.token);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_refresh_tokens_sync ON admission.refresh_tokens;
CREATE TRIGGER trg_refresh_tokens_sync
BEFORE INSERT OR UPDATE ON admission.refresh_tokens
FOR EACH ROW EXECUTE FUNCTION admission.sync_token_and_hash();

DROP TRIGGER IF EXISTS trg_reset_tokens_sync ON admission.password_reset_tokens;
CREATE TRIGGER trg_reset_tokens_sync
BEFORE INSERT OR UPDATE ON admission.password_reset_tokens
FOR EACH ROW EXECUTE FUNCTION admission.sync_token_and_hash();

-- ---- 1.4. Bảng Nhật ký Kiểm toán Bảo mật (Auth Audit Log) ----
CREATE TABLE IF NOT EXISTS admission.auth_audit_logs (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER REFERENCES admission.users(id) ON DELETE SET NULL,
    email       VARCHAR(255),                              -- Email thử đăng nhập (dùng khi user_id = NULL, TASK-ADM-12)
    action      VARCHAR(50) NOT NULL CHECK (action IN (
                    'LOGIN_SUCCESS', 'LOGIN_FAILED',
                    'REFRESH_TOKEN', 'LOGOUT',
                    'PASSWORD_CHANGED', 'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED'
                )),
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    details     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.auth_audit_logs IS 'Ghi log tất cả sự kiện bảo mật; user_id = NULL khi account bị xóa (SET NULL) hoặc email không tồn tại';

-- ---- 1.5. Bảng Phòng học (Rooms) ----
CREATE TABLE IF NOT EXISTS admission.rooms (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(50) UNIQUE NOT NULL,
    name        VARCHAR(100) NOT NULL,
    capacity    INTEGER NOT NULL CHECK (capacity > 0),
    branch_name VARCHAR(100) DEFAULT 'Cơ sở chính',
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.rooms IS 'Phòng học tại các cơ sở; kiểm tra xung đột lịch trước khi mở lớp';

-- ---- 1.6. Bảng Khóa học & Lộ trình Đào tạo (Courses) ----
CREATE TABLE IF NOT EXISTS admission.courses (
    id                      SERIAL PRIMARY KEY,
    code                    VARCHAR(50) UNIQUE NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    level                   VARCHAR(50) NOT NULL CHECK (UPPER(level) IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    target_output           VARCHAR(100),
    duration_weeks          INTEGER NOT NULL CHECK (duration_weeks > 0),
    tuition_amount          NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (tuition_amount >= 0),
    prerequisite_course_id  INTEGER REFERENCES admission.courses(id) ON DELETE SET NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.courses IS 'Danh mục khóa học; self-reference prerequisite_course_id cho lộ trình đào tạo';

-- ---- 1.7. Bảng Lớp học & Thời khóa biểu (Classes) ----
CREATE TABLE IF NOT EXISTS admission.classes (
    id              SERIAL PRIMARY KEY,
    course_id       INTEGER NOT NULL REFERENCES admission.courses(id) ON DELETE RESTRICT,
    room_id         INTEGER REFERENCES admission.rooms(id) ON DELETE RESTRICT,
    teacher_id      INTEGER REFERENCES admission.users(id) ON DELETE RESTRICT,
    code            VARCHAR(50) UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    day_pattern     VARCHAR(50) NOT NULL CHECK (day_pattern IN ('MON_WED_FRI', 'TUE_THU_SAT', 'SAT_SUN', 'DAILY', 'CUSTOM')),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    min_students    INTEGER DEFAULT 10 CHECK (min_students > 0),
    max_students    INTEGER DEFAULT 25,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    room            VARCHAR(100),                          -- Snapshot tên/mã phòng (tương thích contract.md & skeleton)
    teacher_name    VARCHAR(255),                          -- Snapshot tên giảng viên (tương thích contract.md & skeleton)
    schedule        VARCHAR(255),                          -- Snapshot lịch học dạng chuỗi (tương thích contract.md)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_class_dates CHECK (end_date > start_date),
    CONSTRAINT chk_class_time  CHECK (end_time > start_time),
    CONSTRAINT chk_class_capacity CHECK (max_students >= min_students)
);
COMMENT ON TABLE admission.classes IS 'Lớp học; teacher_id là FK tới users(role=TEACHER)';

-- ---- 1.8. Bảng Tuyển sinh & Giữ chỗ 48h (Enrollments) ----
CREATE TABLE IF NOT EXISTS admission.enrollments (
    id                  SERIAL PRIMARY KEY,
    -- student_id là FK tới admission.users(id) role=STUDENT (vì học viên đăng ký qua user account)
    student_id          INTEGER NOT NULL REFERENCES admission.users(id) ON DELETE RESTRICT,
    class_id            INTEGER NOT NULL REFERENCES admission.classes(id) ON DELETE RESTRICT,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
                            'PENDING', 'APPROVED', 'REJECTED',
                            'EXPIRED', 'CANCELLED', 'CANCELLED_REFUND_PENDING'
                        )),
    hold_expires_at     TIMESTAMPTZ,
    invoice_code        VARCHAR(50),
    source              VARCHAR(50) DEFAULT 'ONLINE' CHECK (source IN ('ONLINE', 'WALK_IN', 'HOTLINE', 'FANPAGE', 'REFERRAL')),
    approval_method     VARCHAR(50) CHECK (approval_method IN ('ONLINE_PAYMENT', 'MANUAL_CASH', 'STAFF_OVERRIDE')),
    approved_by_user_id INTEGER REFERENCES admission.users(id) ON DELETE SET NULL,
    note                TEXT,
    notes               TEXT,                              -- Alias tương thích TASK-ADM-44 (đồng bộ tự động với note)
    enrolled_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at         TIMESTAMPTZ,
    -- v3.6 & TASK-ADM-47
    enrollment_channel       VARCHAR(20) NOT NULL DEFAULT 'ONLINE' CHECK (enrollment_channel IN ('ONLINE', 'AT_COUNTER')),
    hold_extended_by_user_id INTEGER REFERENCES admission.users(id) ON DELETE SET NULL,
    hold_extension_reason    TEXT,
    student_sync_status      VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (student_sync_status IN ('NOT_STARTED', 'PENDING', 'SYNCED', 'FAILED')),
    is_synced_to_student     BOOLEAN NOT NULL DEFAULT FALSE, -- Tương thích TASK-ADM-47 (đồng bộ với student_sync_status)
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, class_id)
);
COMMENT ON TABLE admission.enrollments IS 'Đơn tuyển sinh; giữ chỗ 48h qua hold_expires_at. INV-ADM-01: không duyệt nếu lớp đã đủ sĩ số';

CREATE OR REPLACE FUNCTION admission.sync_enrollment_compat()
RETURNS TRIGGER AS $$
BEGIN
    NEW.note  := COALESCE(NEW.note, NEW.notes);
    NEW.notes := COALESCE(NEW.notes, NEW.note);
    IF NEW.student_sync_status = 'SYNCED' THEN
        NEW.is_synced_to_student := TRUE;
    ELSIF NEW.is_synced_to_student = TRUE AND NEW.student_sync_status = 'NOT_STARTED' THEN
        NEW.student_sync_status := 'SYNCED';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_enrollments_compat ON admission.enrollments;
CREATE TRIGGER trg_enrollments_compat
BEFORE INSERT OR UPDATE ON admission.enrollments
FOR EACH ROW EXECUTE FUNCTION admission.sync_enrollment_compat();

-- ---- 1.9. Bảng Bộ đề thi Khảo thí Đầu vào (Test Sets) ----
CREATE TABLE IF NOT EXISTS admission.test_sets (
    id               SERIAL PRIMARY KEY,
    code             VARCHAR(50) UNIQUE,                   -- Mã bộ đề (vd PLACEMENT-GEN-01, TASK-ADM-30 / API_CATALOG)
    title            VARCHAR(255) NOT NULL,
    description      TEXT,                                 -- Mô tả bộ đề khảo thí
    duration_minutes INTEGER NOT NULL DEFAULT 45 CHECK (duration_minutes > 0),
    total_max_score  NUMERIC(5, 2) NOT NULL DEFAULT 100.0 CHECK (total_max_score > 0),
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.test_sets IS 'Bộ đề khảo thí; chỉ bộ đề is_active=TRUE mới được phân phối cho học viên';

-- ---- 1.10. Bảng Câu hỏi Khảo thí 3 Kỹ năng (Test Questions) ----
CREATE TABLE IF NOT EXISTS admission.test_questions (
    id             SERIAL PRIMARY KEY,
    test_set_id    INTEGER NOT NULL REFERENCES admission.test_sets(id) ON DELETE CASCADE,
    skill_type     VARCHAR(50) NOT NULL CHECK (skill_type IN ('LISTENING', 'READING', 'GRAMMAR')),
    order_index    INTEGER NOT NULL DEFAULT 1,
    display_order  INTEGER NOT NULL DEFAULT 1,             -- Tương thích TASK-ADM-30 (đồng bộ với order_index)
    audio_url      TEXT,
    passage_text   TEXT,
    question_text  TEXT NOT NULL,
    option_a       TEXT NOT NULL,
    option_b       TEXT NOT NULL,
    option_c       TEXT NOT NULL,
    option_d       TEXT NOT NULL,
    correct_option CHAR(1) NOT NULL CHECK (correct_option IN ('A', 'B', 'C', 'D')),
    score_weight   NUMERIC(4, 2) NOT NULL DEFAULT 1.0 CHECK (score_weight > 0),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.test_questions IS 'Câu hỏi trắc nghiệm 4 đáp án; correct_option bị giấu khỏi API public/start';

CREATE OR REPLACE FUNCTION admission.sync_question_order()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.order_index IS DISTINCT FROM 1 AND NEW.display_order = 1 THEN
        NEW.display_order := NEW.order_index;
    ELSIF NEW.display_order IS DISTINCT FROM 1 AND NEW.order_index = 1 THEN
        NEW.order_index := NEW.display_order;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_test_questions_order ON admission.test_questions;
CREATE TRIGGER trg_test_questions_order
BEFORE INSERT OR UPDATE ON admission.test_questions
FOR EACH ROW EXECUTE FUNCTION admission.sync_question_order();

-- ---- 1.11. Bảng Ma trận Quy đổi Điểm → Khóa học Khuyến nghị ----
CREATE TABLE IF NOT EXISTS admission.test_recommendation_rules (
    id                     SERIAL PRIMARY KEY,
    test_set_id            INTEGER NOT NULL REFERENCES admission.test_sets(id) ON DELETE CASCADE,
    min_score              NUMERIC(5, 2) NOT NULL,
    max_score              NUMERIC(5, 2) NOT NULL,
    assessed_level         VARCHAR(50)   NOT NULL,
    recommended_course_id  INTEGER NOT NULL REFERENCES admission.courses(id) ON DELETE CASCADE,
    advice_text            TEXT,
    advice_note            TEXT,                           -- Tương thích TASK-ADM-32 (đồng bộ với advice_text)
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_score_range CHECK (max_score > min_score AND min_score >= 0)
);
COMMENT ON TABLE admission.test_recommendation_rules IS 'Quy tắc đề xuất khóa học theo dải điểm; không được chồng lấn (kiểm tra ở app layer)';

CREATE OR REPLACE FUNCTION admission.sync_rule_advice()
RETURNS TRIGGER AS $$
BEGIN
    NEW.advice_text := COALESCE(NEW.advice_text, NEW.advice_note);
    NEW.advice_note := COALESCE(NEW.advice_note, NEW.advice_text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_test_rules_advice ON admission.test_recommendation_rules;
CREATE TRIGGER trg_test_rules_advice
BEFORE INSERT OR UPDATE ON admission.test_recommendation_rules
FOR EACH ROW EXECUTE FUNCTION admission.sync_rule_advice();

-- ---- 1.11b. Bảng Lượt làm bài thi (Test Attempts) — v3.6, INV-TST-02 ----
CREATE TABLE IF NOT EXISTS admission.test_attempts (
    id           SERIAL PRIMARY KEY,
    user_id      INTEGER NOT NULL REFERENCES admission.users(id) ON DELETE RESTRICT,
    test_set_id  INTEGER NOT NULL REFERENCES admission.test_sets(id) ON DELETE RESTRICT,
    status       VARCHAR(20) NOT NULL DEFAULT 'STARTED' CHECK (status IN ('STARTED', 'SUBMITTED', 'EXPIRED')),
    started_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attempt_time CHECK (expires_at > started_at)
);
COMMENT ON TABLE admission.test_attempts IS 'Lượt thi theo thời gian server; chống nộp bài sau hạn và chấm lại cùng lượt';

-- ---- 1.12. Bảng Kết quả Làm bài Test Đầu vào (Test Submissions) ----
CREATE TABLE IF NOT EXISTS admission.test_submissions (
    id                     SERIAL PRIMARY KEY,
    attempt_id             INTEGER UNIQUE REFERENCES admission.test_attempts(id) ON DELETE RESTRICT,  -- INV-TST-02
    user_id                INTEGER NOT NULL REFERENCES admission.users(id) ON DELETE CASCADE,
    test_set_id            INTEGER NOT NULL REFERENCES admission.test_sets(id) ON DELETE RESTRICT,
    full_name              VARCHAR(255),                   -- Snapshot họ tên thí sinh (TASK-ADM-31)
    email                  VARCHAR(255),                   -- Snapshot email thí sinh (TASK-ADM-31)
    phone                  VARCHAR(50),                    -- Snapshot SĐT thí sinh (TASK-ADM-31)
    listening_score        NUMERIC(5, 2) NOT NULL DEFAULT 0.0 CHECK (listening_score >= 0),
    reading_score          NUMERIC(5, 2) NOT NULL DEFAULT 0.0 CHECK (reading_score >= 0),
    grammar_score          NUMERIC(5, 2) NOT NULL DEFAULT 0.0 CHECK (grammar_score >= 0),
    total_score            NUMERIC(5, 2) NOT NULL,
    assessed_level         VARCHAR(50),
    recommended_course_id  INTEGER REFERENCES admission.courses(id) ON DELETE SET NULL,
    answers_detail         JSONB,
    started_at             TIMESTAMPTZ,                    -- Thời điểm bắt đầu làm bài (TASK-ADM-31)
    submitted_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.test_submissions IS 'Kết quả bài khảo thí; answers_detail lưu JSON {question_id, selected, is_correct}[]';

-- ---- 1.13. Bảng Sự kiện Finance đã xử lý (Idempotency callback) — v3.6, INV-ENR-02 ----
CREATE TABLE IF NOT EXISTS admission.processed_finance_events (
    id                SERIAL PRIMARY KEY,
    enrollment_id     INTEGER NOT NULL REFERENCES admission.enrollments(id) ON DELETE RESTRICT,
    finance_event_id  VARCHAR(255) UNIQUE NOT NULL,
    paid_at           TIMESTAMPTZ NOT NULL,
    processed_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE admission.processed_finance_events IS 'Callback Finance trùng event_id trả 200 OK, không duyệt lại; thanh toán đến muộn (INV-ENR-03) cũng được ghi ở đây để đối soát';

-- ---- Gắn trigger set_updated_at cho toàn bộ bảng trong Schema ADMISSION ----
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'users', 'refresh_tokens', 'password_reset_tokens', 'auth_audit_logs',
        'rooms', 'courses', 'classes', 'enrollments', 'test_sets',
        'test_questions', 'test_recommendation_rules', 'test_attempts',
        'test_submissions', 'processed_finance_events'
    ] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON admission.%I', t, t);
        EXECUTE format('CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON admission.%I FOR EACH ROW EXECUTE FUNCTION public.set_updated_at()', t, t);
    END LOOP;
END $$;

-- ---- Indexes cho Schema ADMISSION ----
CREATE INDEX IF NOT EXISTS idx_enrollments_created_channel ON admission.enrollments (enrolled_at, enrollment_channel);
CREATE INDEX IF NOT EXISTS idx_enrollments_class_status    ON admission.enrollments (class_id, status);
CREATE INDEX IF NOT EXISTS idx_enrollments_pending_hold    ON admission.enrollments (hold_expires_at) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_test_attempts_started  ON admission.test_attempts (started_at, status);
CREATE INDEX IF NOT EXISTS idx_test_attempts_user     ON admission.test_attempts (user_id, test_set_id);
CREATE INDEX IF NOT EXISTS idx_fin_events_enrollment  ON admission.processed_finance_events (enrollment_id);
CREATE INDEX IF NOT EXISTS idx_users_email           ON admission.users (email);
CREATE INDEX IF NOT EXISTS idx_users_role_active      ON admission.users (role, is_active);
CREATE INDEX IF NOT EXISTS idx_refresh_token          ON admission.refresh_tokens (token) WHERE is_revoked = FALSE;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user    ON admission.refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_reset_token            ON admission.password_reset_tokens (token) WHERE is_used = FALSE;
CREATE INDEX IF NOT EXISTS idx_audit_user_created     ON admission.auth_audit_logs (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action           ON admission.auth_audit_logs (action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_classes_schedule       ON admission.classes (room_id, teacher_id, day_pattern, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_classes_status         ON admission.classes (status, course_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_status_hold ON admission.enrollments (class_id, status, hold_expires_at);
CREATE INDEX IF NOT EXISTS idx_enrollments_student    ON admission.enrollments (student_id);
CREATE INDEX IF NOT EXISTS idx_test_q_set_skill       ON admission.test_questions (test_set_id, skill_type, order_index);
CREATE INDEX IF NOT EXISTS idx_submissions_user       ON admission.test_submissions (user_id, submitted_at DESC);


-- ============================================================================
-- SCHEMA 2: STUDENT
-- SERVICE: student-service (Python FastAPI, Port 8082)
-- CHỨC NĂNG: Hồ sơ học viên, Điểm danh, Yêu cầu học vụ (bảo lưu/chuyển/bỏ)
-- BẢNG: 3 bảng + sequence + triggers + indexes + seed data
-- ============================================================================

-- Sequence sinh mã học viên tự động khi INSERT không truyền student_code (tương thích skeleton main.py)
CREATE SEQUENCE IF NOT EXISTS student.student_code_seq START WITH 100 INCREMENT BY 1;

-- ---- 2.1. Bảng Hồ sơ Học viên (Students) ----
CREATE TABLE IF NOT EXISTS student.students (
    id              SERIAL PRIMARY KEY,
    -- user_id: Logical FK tới admission.users(id) — liên kết tài khoản đăng nhập với hồ sơ học viên
    user_id         INTEGER,
    student_code    VARCHAR(30) UNIQUE NOT NULL
                        DEFAULT ('HV-' || EXTRACT(YEAR FROM CURRENT_DATE)::INT || '-' || LPAD(nextval('student.student_code_seq')::TEXT, 4, '0')),
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    phone           VARCHAR(50),
    date_of_birth   DATE,
    address         TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'STUDYING' CHECK (status IN ('STUDYING', 'RESERVED', 'DROPPED', 'GRADUATED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  student.students IS 'Hồ sơ học viên; được tạo tự động khi enrollment APPROVED (gọi từ Admission Service)';
COMMENT ON COLUMN student.students.user_id IS 'Logical FK → admission.users(id); KHÔNG có Physical FK chéo schema';
COMMENT ON COLUMN student.students.student_code IS 'Mã học viên duy nhất, format: HV-YYYY-NNNN (vd HV-2026-0001), sinh tự động';

-- ---- 2.2. Bảng Điểm danh (Attendance) ----
CREATE TABLE IF NOT EXISTS student.attendance (
    id              SERIAL PRIMARY KEY,
    -- class_id: Logical FK tới admission.classes(id)
    class_id        INTEGER NOT NULL,
    student_id      INTEGER NOT NULL REFERENCES student.students(id) ON DELETE CASCADE,
    session_date    DATE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PRESENT' CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (class_id, student_id, session_date)
);
COMMENT ON TABLE  student.attendance IS 'Điểm danh; INV-STU-01: mỗi học viên chỉ có 1 bản ghi/buổi/lớp (UNIQUE 3 cột)';
COMMENT ON COLUMN student.attendance.class_id IS 'Logical FK → admission.classes(id); KHÔNG có Physical FK chéo schema';

-- ---- 2.3. Bảng Yêu cầu Học vụ (Status Requests: Bảo lưu / Chuyển / Bỏ học) ----
CREATE TABLE IF NOT EXISTS student.status_requests (
    id              SERIAL PRIMARY KEY,
    student_id      INTEGER NOT NULL REFERENCES student.students(id) ON DELETE CASCADE,
    request_type    VARCHAR(20) NOT NULL CHECK (request_type IN ('RESERVE', 'TRANSFER', 'DROP')),
    reason          TEXT NOT NULL,
    -- class_id: Logical FK tới admission.classes(id) — lớp đang theo học
    class_id        INTEGER,
    -- target_class_id: Logical FK tới admission.classes(id) — lớp muốn chuyển (chỉ dùng khi TRANSFER)
    target_class_id INTEGER,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by     INTEGER,
    reviewed_note   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     TIMESTAMPTZ
);
COMMENT ON TABLE  student.status_requests IS 'Yêu cầu học vụ: bảo lưu (RESERVE), chuyển lớp (TRANSFER), bỏ học (DROP)';
COMMENT ON COLUMN student.status_requests.class_id IS 'Logical FK → admission.classes(id)';
COMMENT ON COLUMN student.status_requests.reviewed_by IS 'Logical FK → admission.users(id) — nhân viên duyệt';

-- ---- Gắn trigger set_updated_at cho Schema STUDENT ----
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['students', 'attendance', 'status_requests'] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON student.%I', t, t);
        EXECUTE format('CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON student.%I FOR EACH ROW EXECUTE FUNCTION public.set_updated_at()', t, t);
    END LOOP;
END $$;

-- ---- Indexes cho Schema STUDENT ----
CREATE INDEX IF NOT EXISTS idx_students_user_id      ON student.students (user_id);
CREATE INDEX IF NOT EXISTS idx_students_code         ON student.students (student_code);
CREATE INDEX IF NOT EXISTS idx_students_email        ON student.students (email);
CREATE INDEX IF NOT EXISTS idx_students_status       ON student.students (status);
CREATE INDEX IF NOT EXISTS idx_attendance_class      ON student.attendance (class_id, session_date);
CREATE INDEX IF NOT EXISTS idx_attendance_student    ON student.attendance (student_id, session_date DESC);
CREATE INDEX IF NOT EXISTS idx_status_req_student    ON student.status_requests (student_id, status);


-- ============================================================================
-- SCHEMA 3: LEARNING
-- SERVICE: learning-service (Python FastAPI, Port 8083)
-- CHỨC NĂNG: Quản lý học liệu, Nhập điểm đánh giá, Báo cáo kết quả học tập
-- BẢNG: 2 bảng + 2 views + triggers + indexes
-- ============================================================================

-- ---- 3.1. Bảng Học liệu (Materials) ----
CREATE TABLE IF NOT EXISTS learning.materials (
    id                  SERIAL PRIMARY KEY,
    -- class_id: Logical FK tới admission.classes(id)
    class_id            INTEGER NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    material_type       VARCHAR(50) NOT NULL DEFAULT 'DOCUMENT' CHECK (material_type IN ('DOCUMENT', 'VIDEO', 'EXERCISE', 'SLIDE')),
    file_url            TEXT NOT NULL CHECK (btrim(file_url) <> ''),
    original_filename   VARCHAR(255),
    file_size           BIGINT CHECK (file_size >= 0),
    -- uploaded_by: Logical FK tới admission.users(id) role=TEACHER
    uploaded_by         INTEGER,
    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  learning.materials IS 'Học liệu của lớp; file_url trỏ tới object storage (S3/MinIO)';
COMMENT ON COLUMN learning.materials.class_id IS 'Logical FK → admission.classes(id)';
COMMENT ON COLUMN learning.materials.uploaded_by IS 'Logical FK → admission.users(id) role=TEACHER';

-- ---- 3.2. Bảng Điểm Đánh giá (Grades) ----
CREATE TABLE IF NOT EXISTS learning.grades (
    id                SERIAL PRIMARY KEY,
    -- class_id: Logical FK tới admission.classes(id)
    class_id          INTEGER NOT NULL,
    -- student_id: Logical FK tới student.students(id)
    student_id        INTEGER NOT NULL,
    assessment_name   VARCHAR(255) NOT NULL,
    score             NUMERIC(5, 2) NOT NULL CHECK (score >= 0),
    max_score         NUMERIC(5, 2) NOT NULL DEFAULT 100.0 CHECK (max_score > 0),
    weight            NUMERIC(3, 2) DEFAULT 1.0 CHECK (weight IS NULL OR (weight > 0 AND weight <= 1)),
    feedback          TEXT,
    -- graded_by: Logical FK tới admission.users(id) role=TEACHER
    graded_by         INTEGER,
    graded_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (class_id, student_id, assessment_name),
    CONSTRAINT chk_score_lte_max CHECK (score <= max_score)
);
COMMENT ON TABLE  learning.grades IS 'Điểm đánh giá; INV-LRN-01: 0 <= score <= max_score; weight dùng tính điểm trung bình có trọng số (NULL coi là 1.0)';
COMMENT ON COLUMN learning.grades.class_id IS 'Logical FK → admission.classes(id)';
COMMENT ON COLUMN learning.grades.student_id IS 'Logical FK → student.students(id)';
COMMENT ON COLUMN learning.grades.graded_by IS 'Logical FK → admission.users(id) role=TEACHER. INV-SEC-02: teacher chỉ nhập điểm lớp mình dạy';

-- ---- Gắn trigger set_updated_at cho Schema LEARNING ----
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['materials', 'grades'] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON learning.%I', t, t);
        EXECUTE format('CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON learning.%I FOR EACH ROW EXECUTE FUNCTION public.set_updated_at()', t, t);
    END LOOP;
END $$;

-- ---- Indexes cho Schema LEARNING ----
CREATE INDEX IF NOT EXISTS idx_materials_class         ON learning.materials (class_id);
CREATE INDEX IF NOT EXISTS idx_materials_type          ON learning.materials (class_id, material_type);
CREATE INDEX IF NOT EXISTS idx_grades_class_student    ON learning.grades (class_id, student_id);
CREATE INDEX IF NOT EXISTS idx_grades_class_assessment ON learning.grades (class_id, assessment_name);
CREATE INDEX IF NOT EXISTS idx_grades_student          ON learning.grades (student_id);

-- ---- 3.3. View: Tổng kết Điểm Học viên theo Lớp ----
-- Chuẩn hóa từng bài về thang 100% trước khi nhân trọng số COALESCE(weight, 1.0)
CREATE OR REPLACE VIEW learning.v_student_grade_summary AS
SELECT
    class_id,
    student_id,
    COUNT(id)                                                                                           AS total_assessments,
    ROUND(SUM(score * COALESCE(weight, 1.0)) / NULLIF(SUM(COALESCE(weight, 1.0)), 0), 2)               AS weighted_avg_score,
    ROUND(
        SUM((score / max_score * 100.0) * COALESCE(weight, 1.0))
        / NULLIF(SUM(COALESCE(weight, 1.0)), 0)
    , 2)                                                                                                AS average_percentage,
    CASE
        WHEN (
            SUM((score / max_score * 100.0) * COALESCE(weight, 1.0))
            / NULLIF(SUM(COALESCE(weight, 1.0)), 0)
        ) >= 50
        THEN 'Pass'
        ELSE 'Fail'
    END                                                                                                 AS result
FROM learning.grades
GROUP BY class_id, student_id;
COMMENT ON VIEW learning.v_student_grade_summary IS 'Tổng kết điểm có trọng số (chuẩn hóa thang 100%); Pass nếu average_percentage >= 50%';

-- ---- 3.4. View: Tỷ lệ Vượt qua theo Lớp ----
CREATE OR REPLACE VIEW learning.v_class_pass_rate AS
SELECT
    class_id,
    COUNT(DISTINCT student_id)                                              AS total_students,
    COUNT(DISTINCT student_id) FILTER (WHERE result = 'Pass')              AS pass_count,
    COUNT(DISTINCT student_id) FILTER (WHERE result = 'Fail')              AS fail_count,
    ROUND(
        (COUNT(DISTINCT student_id) FILTER (WHERE result = 'Pass'))::NUMERIC
        / NULLIF(COUNT(DISTINCT student_id), 0) * 100
    , 2)                                                                    AS pass_rate_percentage
FROM learning.v_student_grade_summary
GROUP BY class_id;
COMMENT ON VIEW learning.v_class_pass_rate IS 'Tỷ lệ Pass/Fail theo lớp; dùng cho báo cáo kết quả học tập';


-- ============================================================================
-- SCHEMA 4: FINANCE
-- SERVICE: finance-service (Python FastAPI, Port 8084)
-- CHỨC NĂNG: Bảng giá học phí, Hóa đơn, Voucher, Thanh toán VietQR, Ví bảo lưu, Hoàn tiền 3 cấp
-- BẢNG: 9 bảng (8 bảng nghiệp vụ chính + tuition_fees) + 2 BI views + triggers + indexes + seed data
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS finance.invoice_code_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS finance.payment_code_seq START WITH 100 INCREMENT BY 1;

-- ---- 4.0. Bảng Cấu hình Biểu phí Khóa học (Tuition Fees — FIN-01 / contract.md) ----
CREATE TABLE IF NOT EXISTS finance.tuition_fees (
    id              SERIAL PRIMARY KEY,
    -- course_id: Logical FK → admission.courses(id)
    course_id       INTEGER UNIQUE NOT NULL,
    course_name     VARCHAR(255) NOT NULL DEFAULT '',
    amount          DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (amount >= 0),
    base_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (base_amount >= 0),
    currency        VARCHAR(10) NOT NULL DEFAULT 'VND',
    discount_pct    NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (discount_pct >= 0 AND discount_pct <= 100),
    note            TEXT,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE finance.tuition_fees IS 'Biểu phí chuẩn theo khóa học (FIN-01 / contract.md); đồng bộ hoặc tra cứu đối chiếu với admission.courses';

CREATE OR REPLACE FUNCTION finance.sync_tuition_fees_compat()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.amount = 0 AND NEW.base_amount > 0 THEN
        NEW.amount := NEW.base_amount;
    ELSIF NEW.base_amount = 0 AND NEW.amount > 0 THEN
        NEW.base_amount := NEW.amount;
    END IF;
    NEW.note        := COALESCE(NEW.note, NEW.description);
    NEW.description := COALESCE(NEW.description, NEW.note);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tuition_fees_compat ON finance.tuition_fees;
CREATE TRIGGER trg_tuition_fees_compat
BEFORE INSERT OR UPDATE ON finance.tuition_fees
FOR EACH ROW EXECUTE FUNCTION finance.sync_tuition_fees_compat();

-- ---- 4.1. Bảng Chiến dịch Voucher (Vouchers) ----
CREATE TABLE IF NOT EXISTS finance.vouchers (
    id                  SERIAL PRIMARY KEY,
    code                VARCHAR(50) UNIQUE NOT NULL,
    campaign_name       VARCHAR(150) NOT NULL DEFAULT 'Chiến dịch ưu đãi học phí',
    discount_percent    INTEGER NOT NULL DEFAULT 0 CHECK (discount_percent >= 0 AND discount_percent <= 100),
    discount_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    min_order_value     DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (min_order_value >= 0),
    max_discount_amount DECIMAL(12, 2),
    max_uses            INTEGER NOT NULL DEFAULT 100 CHECK (max_uses > 0),
    current_uses        INTEGER NOT NULL DEFAULT 0 CHECK (current_uses >= 0),
    valid_from          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMP NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_voucher_usage_limit CHECK (current_uses <= max_uses),
    CONSTRAINT chk_voucher_dates CHECK (expires_at > valid_from)
);
COMMENT ON TABLE finance.vouchers IS 'Voucher giảm giá; INV-FIN-04: SELECT...FOR UPDATE trước khi tăng current_uses';

-- ---- 4.2. Bảng Hóa đơn Học phí (Invoices) ----
CREATE TABLE IF NOT EXISTS finance.invoices (
    id              SERIAL PRIMARY KEY,
    invoice_code    VARCHAR(50) UNIQUE NOT NULL
                        DEFAULT ('INV-' || EXTRACT(YEAR FROM CURRENT_DATE)::INT || '-' || LPAD(nextval('finance.invoice_code_seq')::TEXT, 4, '0')),
    -- student_id: Logical FK → admission.users(id) theo R-ID-1 (hoặc student.students(id))
    student_id      INTEGER NOT NULL,
    -- course_id: Logical FK → admission.courses(id) (DEFAULT 1 tương thích skeleton main.py)
    course_id       INTEGER NOT NULL DEFAULT 1,
    -- enrollment_id: Logical FK → admission.enrollments(id)
    enrollment_id   INTEGER,
    invoice_type    VARCHAR(30) NOT NULL DEFAULT 'COURSE_PACKAGE'
                        CHECK (invoice_type IN ('COURSE_PACKAGE', 'STANDALONE_RETAKE', 'MIXED')),
    amount          DECIMAL(12, 2) NOT NULL CHECK (amount >= 0),
    voucher_id      INTEGER REFERENCES finance.vouchers(id) ON DELETE SET NULL,
    discount_applied DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (discount_applied >= 0),
    final_amount    DECIMAL(12, 2) NOT NULL CHECK (final_amount >= 0),
    paid_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    remaining_amount DECIMAL(12, 2) GENERATED ALWAYS AS (final_amount - paid_amount) STORED,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'UNPAID', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED', 'REFUNDED')),
    due_date        DATE,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at         TIMESTAMP,
    CONSTRAINT chk_invoice_math        CHECK (final_amount = amount - discount_applied),
    CONSTRAINT chk_invoice_paid_bound  CHECK (paid_amount <= final_amount)
);
COMMENT ON TABLE  finance.invoices IS 'Hóa đơn học phí; INV-FIN-01: không sửa amount/discount/final khi PAID hoặc REFUNDED';
COMMENT ON COLUMN finance.invoices.student_id IS 'Logical FK → admission.users(id) theo R-ID-1';
COMMENT ON COLUMN finance.invoices.course_id IS 'Logical FK → admission.courses(id)';
COMMENT ON COLUMN finance.invoices.enrollment_id IS 'Logical FK → admission.enrollments(id)';
COMMENT ON COLUMN finance.invoices.remaining_amount IS 'GENERATED ALWAYS = final_amount - paid_amount';

-- Trigger tự động tính final_amount = amount - discount_applied khi INSERT không truyền final_amount (tương thích skeleton)
CREATE OR REPLACE FUNCTION finance.normalize_invoice_row()
RETURNS TRIGGER AS $$
BEGIN
    NEW.discount_applied := COALESCE(NEW.discount_applied, 0);
    NEW.final_amount     := COALESCE(NEW.final_amount, NEW.amount - NEW.discount_applied);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_invoices_normalize ON finance.invoices;
CREATE TRIGGER trg_invoices_normalize
BEFORE INSERT OR UPDATE ON finance.invoices
FOR EACH ROW EXECUTE FUNCTION finance.normalize_invoice_row();

-- ---- 4.3. Bảng Chi tiết Khoản mục Hóa đơn (Invoice Items) ----
CREATE TABLE IF NOT EXISTS finance.invoice_items (
    id          SERIAL PRIMARY KEY,
    invoice_id  INTEGER NOT NULL REFERENCES finance.invoices(id) ON DELETE CASCADE,
    -- course_id: Logical FK → admission.courses(id)
    course_id   INTEGER,
    item_type   VARCHAR(30) NOT NULL CHECK (item_type IN ('TUITION_PACKAGE', 'RETAKE_FEE')),
    description VARCHAR(255) NOT NULL,
    quantity    INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price  DECIMAL(12, 2) NOT NULL CHECK (unit_price >= 0),
    line_total  DECIMAL(12, 2) NOT NULL CHECK (line_total >= 0),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_line_total_math CHECK (line_total = quantity * unit_price)
);
COMMENT ON COLUMN finance.invoice_items.course_id IS 'Logical FK → admission.courses(id)';

-- ---- 4.4. Bảng Lịch Đóng Học phí Nhiều Đợt / Đặt cọc (Installments) ----
CREATE TABLE IF NOT EXISTS finance.installments (
    id               SERIAL PRIMARY KEY,
    invoice_id       INTEGER NOT NULL REFERENCES finance.invoices(id) ON DELETE CASCADE,
    installment_no   INTEGER NOT NULL CHECK (installment_no >= 1),
    payment_ref_code VARCHAR(50) UNIQUE NOT NULL,
    amount_due       DECIMAL(12, 2) NOT NULL CHECK (amount_due > 0),
    amount_paid      DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (amount_paid >= 0),
    due_date         DATE NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED')),
    paid_at          TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (invoice_id, installment_no)
);
COMMENT ON TABLE finance.installments IS 'Kỳ thanh toán; INV-FIN-02: tổng amount_due của các kỳ = final_amount của invoice';

-- ---- 4.5. Bảng Giao dịch Thu tiền & Gạch nợ (Payments) ----
CREATE TABLE IF NOT EXISTS finance.payments (
    id                  SERIAL PRIMARY KEY,
    payment_code        VARCHAR(50) UNIQUE NOT NULL
                            DEFAULT ('PAY-' || EXTRACT(YEAR FROM CURRENT_DATE)::INT || '-' || LPAD(nextval('finance.payment_code_seq')::TEXT, 4, '0')),
    invoice_id          INTEGER NOT NULL REFERENCES finance.invoices(id) ON DELETE RESTRICT,
    installment_id      INTEGER REFERENCES finance.installments(id) ON DELETE SET NULL,
    payment_method      VARCHAR(20) NOT NULL
                            CHECK (payment_method IN ('CASH', 'VIETQR', 'WALLET_OFFSET', 'BANK_TRANSFER', 'CARD', 'MOMO')),
    amount              DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    -- bank_transaction_id & transaction_ref: UNIQUE để đảm bảo Idempotency (INV-FIN-02 / INV-FIN-05)
    bank_transaction_id VARCHAR(100) UNIQUE,
    transaction_ref     VARCHAR(100) UNIQUE,                -- Alias tương thích contract.md & skeleton main.py
    qr_reference_code   VARCHAR(50),
    webhook_payload     JSONB,
    -- received_by_staff_id: Logical FK → admission.users(id) role=STAFF
    received_by_staff_id INTEGER,
    status              VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'
                            CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    paid_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  finance.payments IS 'Phiếu thu; INV-FIN-05: bank_transaction_id / transaction_ref UNIQUE = idempotency VietQR';
COMMENT ON COLUMN finance.payments.bank_transaction_id IS 'Mã giao dịch ngân hàng; UNIQUE → chống ghi nhận trùng';

CREATE OR REPLACE FUNCTION finance.sync_payment_refs()
RETURNS TRIGGER AS $$
BEGIN
    NEW.bank_transaction_id := COALESCE(NEW.bank_transaction_id, NEW.transaction_ref);
    NEW.transaction_ref     := COALESCE(NEW.transaction_ref, NEW.bank_transaction_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payments_sync_refs ON finance.payments;
CREATE TRIGGER trg_payments_sync_refs
BEFORE INSERT OR UPDATE ON finance.payments
FOR EACH ROW EXECUTE FUNCTION finance.sync_payment_refs();

-- ---- 4.6. Bảng Ví Bảo lưu Học viên (Student Wallets) ----
CREATE TABLE IF NOT EXISTS finance.student_wallets (
    id              SERIAL PRIMARY KEY,
    -- student_id: Logical FK → admission.users(id) theo R-ID-1; UNIQUE = mỗi học viên 1 ví
    student_id      INTEGER UNIQUE NOT NULL,
    balance         DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    is_withdrawable BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'FROZEN')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  finance.student_wallets IS 'Ví bảo lưu; INV-FIN-06: is_withdrawable luôn FALSE; số dư chỉ giảm qua TUITION_OFFSET';
COMMENT ON COLUMN finance.student_wallets.student_id IS 'Logical FK → admission.users(id) theo R-ID-1; UNIQUE = mỗi học viên 1 ví';

-- ---- 4.7. Bảng Lịch sử Giao dịch Ví (Wallet Transactions) ----
CREATE TABLE IF NOT EXISTS finance.wallet_transactions (
    id                  SERIAL PRIMARY KEY,
    wallet_id           INTEGER NOT NULL REFERENCES finance.student_wallets(id) ON DELETE RESTRICT,
    tx_type             VARCHAR(30) NOT NULL
                            CHECK (tx_type IN ('RESERVATION_CREDIT', 'TUITION_OFFSET', 'ADJUSTMENT')),
    amount              DECIMAL(12, 2) NOT NULL CHECK (amount > 0),
    related_invoice_id  INTEGER REFERENCES finance.invoices(id) ON DELETE SET NULL,
    reason              TEXT NOT NULL,
    -- created_by_staff_id: Logical FK → admission.users(id)
    created_by_staff_id INTEGER,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON COLUMN finance.wallet_transactions.related_invoice_id IS 'Hóa đơn được cấn trừ (khi tx_type = TUITION_OFFSET)';

-- ---- 4.8. Bảng Yêu cầu & Phê duyệt Hoàn tiền 3 cấp (Refund Requests) ----
CREATE TABLE IF NOT EXISTS finance.refund_requests (
    id                          SERIAL PRIMARY KEY,
    refund_code                 VARCHAR(50) UNIQUE NOT NULL,
    invoice_id                  INTEGER NOT NULL REFERENCES finance.invoices(id) ON DELETE RESTRICT,
    -- student_id: Logical FK → admission.users(id) theo R-ID-1
    student_id                  INTEGER NOT NULL,
    -- class_id: Logical FK → admission.classes(id)
    class_id                    INTEGER,
    total_sessions              INTEGER NOT NULL CHECK (total_sessions > 0),
    attended_sessions           INTEGER NOT NULL CHECK (attended_sessions >= 0),
    max_allowed_sessions        INTEGER NOT NULL DEFAULT 3,
    paid_amount_snapshot        DECIMAL(12, 2) NOT NULL,
    refundable_percent          INTEGER NOT NULL CHECK (refundable_percent BETWEEN 0 AND 100),
    deduction_amount            DECIMAL(12, 2) NOT NULL DEFAULT 0,
    requested_refund_amount     DECIMAL(12, 2) NOT NULL CHECK (requested_refund_amount >= 0),
    approved_refund_amount      DECIMAL(12, 2) CHECK (approved_refund_amount >= 0),
    reason                      TEXT NOT NULL,
    bank_name                   VARCHAR(100),
    bank_account_no             VARCHAR(50),
    bank_account_holder         VARCHAR(150),
    transaction_ref             VARCHAR(100),               -- Mã chứng từ giải ngân hoàn tiền (FIN-F08 / API_CATALOG)
    status                      VARCHAR(30) NOT NULL DEFAULT 'PENDING_ACCOUNTANT'
                                    CHECK (status IN (
                                        'PENDING_ACCOUNTANT', 'PENDING_DIRECTOR',
                                        'APPROVED', 'REJECTED', 'COMPLETED'
                                    )),
    -- requested_by_staff_id: Logical FK → admission.users(id) role=STAFF
    requested_by_staff_id       INTEGER NOT NULL,
    requested_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- verified_by_accountant_id: Logical FK → admission.users(id)
    verified_by_accountant_id   INTEGER,
    verified_at                 TIMESTAMP,
    accountant_note             TEXT,
    -- approved_by_director_id: Logical FK → admission.users(id) role=ADMIN
    approved_by_director_id     INTEGER,
    approved_at                 TIMESTAMP,
    director_note               TEXT,
    completed_at                TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_valid_early_withdrawal CHECK (attended_sessions <= max_allowed_sessions)
);
COMMENT ON TABLE  finance.refund_requests IS 'INV-FIN-07: Hoàn tiền 3 cấp PENDING_ACCOUNTANT→PENDING_DIRECTOR→APPROVED→COMPLETED';
COMMENT ON COLUMN finance.refund_requests.student_id IS 'Logical FK → admission.users(id) theo R-ID-1';
COMMENT ON COLUMN finance.refund_requests.class_id IS 'Logical FK → admission.classes(id)';

-- ---- Gắn trigger set_updated_at cho Schema FINANCE ----
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'tuition_fees', 'vouchers', 'invoices', 'invoice_items',
        'installments', 'payments', 'student_wallets',
        'wallet_transactions', 'refund_requests'
    ] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON finance.%I', t, t);
        EXECUTE format('CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON finance.%I FOR EACH ROW EXECUTE FUNCTION public.set_updated_at()', t, t);
    END LOOP;
END $$;

-- ---- Indexes cho Schema FINANCE ----
CREATE INDEX IF NOT EXISTS idx_tuition_fees_course  ON finance.tuition_fees (course_id);
CREATE INDEX IF NOT EXISTS idx_invoices_student     ON finance.invoices (student_id);
CREATE INDEX IF NOT EXISTS idx_invoices_enrollment  ON finance.invoices (enrollment_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status      ON finance.invoices (status);
CREATE INDEX IF NOT EXISTS idx_invoices_code        ON finance.invoices (invoice_code);
CREATE INDEX IF NOT EXISTS idx_installments_invoice ON finance.installments (invoice_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_invoice     ON finance.payments (invoice_id);
CREATE INDEX IF NOT EXISTS idx_payments_bank_txn    ON finance.payments (bank_transaction_id) WHERE bank_transaction_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_wallets_student      ON finance.student_wallets (student_id);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_wallet     ON finance.wallet_transactions (wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_refund_student       ON finance.refund_requests (student_id, status);
CREATE INDEX IF NOT EXISTS idx_refund_invoice       ON finance.refund_requests (invoice_id);

-- ---- 4.9. BI View 1: Doanh thu thực thu & Hoàn phí theo Tháng ----
CREATE OR REPLACE VIEW finance.vw_bi_cash_revenue_monthly AS
WITH cash_in AS (
    SELECT
        DATE_TRUNC('month', paid_at) AS report_month,
        SUM(CASE WHEN payment_method = 'CASH'    THEN amount ELSE 0 END) AS cash_at_counter,
        SUM(CASE WHEN payment_method = 'VIETQR'  THEN amount ELSE 0 END) AS vietqr_transfer,
        SUM(CASE WHEN payment_method IN ('CASH', 'VIETQR') THEN amount ELSE 0 END) AS gross_cash_in
    FROM finance.payments
    WHERE status = 'SUCCESS'
    GROUP BY DATE_TRUNC('month', paid_at)
),
cash_refund AS (
    SELECT
        DATE_TRUNC('month', completed_at) AS report_month,
        COUNT(id)                          AS refund_count,
        SUM(approved_refund_amount)        AS total_refunded
    FROM finance.refund_requests
    WHERE status = 'COMPLETED'
    GROUP BY DATE_TRUNC('month', completed_at)
)
SELECT
    COALESCE(ci.report_month, cr.report_month)       AS report_month,
    COALESCE(ci.cash_at_counter, 0)                  AS cash_at_counter,
    COALESCE(ci.vietqr_transfer, 0)                  AS vietqr_transfer,
    COALESCE(ci.gross_cash_in, 0)                    AS gross_cash_in,
    COALESCE(cr.refund_count, 0)                     AS refund_count,
    COALESCE(cr.total_refunded, 0)                   AS total_refunded,
    (COALESCE(ci.gross_cash_in, 0) - COALESCE(cr.total_refunded, 0)) AS net_cash_revenue,
    CASE
        WHEN COALESCE(ci.gross_cash_in, 0) > 0
        THEN ROUND((COALESCE(cr.total_refunded, 0) / ci.gross_cash_in) * 100, 2)
        ELSE 0
    END                                              AS refund_rate_pct
FROM cash_in ci
FULL OUTER JOIN cash_refund cr ON ci.report_month = cr.report_month
ORDER BY report_month DESC;

-- ---- 4.10. BI View 2: Hiệu quả Chiến dịch Voucher ----
CREATE OR REPLACE VIEW finance.vw_bi_voucher_campaigns AS
SELECT
    v.id                                                        AS voucher_id,
    v.code,
    v.campaign_name,
    v.discount_percent,
    v.discount_amount,
    v.max_uses,
    v.current_uses,
    ROUND((v.current_uses::DECIMAL / NULLIF(v.max_uses, 0)) * 100, 2) AS usage_rate_pct,
    COUNT(i.id)                                                 AS total_invoices,
    COALESCE(SUM(i.discount_applied), 0)                        AS total_discount_given,
    COALESCE(SUM(i.paid_amount), 0)                             AS actual_cash_collected
FROM finance.vouchers v
LEFT JOIN finance.invoices i ON v.id = i.voucher_id AND i.status != 'CANCELLED'
GROUP BY v.id;



-- ============================================================================
-- BƯỚC 2b: TỪ ĐIỂN DỮ LIỆU TRONG DB (COMMENT ON TABLE / COLUMN)
-- Từ điển dữ liệu chuẩn lưu trực tiếp trong PostgreSQL Catalog.
-- Xem bằng psql: \d+ admission.users
-- ============================================================================
CREATE OR REPLACE FUNCTION pg_temp.cmt(tbl TEXT, tbl_desc TEXT, cols JSONB) RETURNS VOID AS $$
DECLARE k TEXT; v TEXT;
BEGIN
    IF tbl_desc IS NOT NULL THEN
        EXECUTE format('COMMENT ON TABLE %s IS %L', tbl, tbl_desc);
    END IF;
    FOR k, v IN SELECT key, value FROM jsonb_each_text(cols) LOOP
        EXECUTE format('COMMENT ON COLUMN %s.%I IS %L', tbl, k, v);
    END LOOP;
END $$ LANGUAGE plpgsql;

-- ---------------------------------- ADMISSION ----------------------------------
SELECT pg_temp.cmt('admission.users', 'Tài khoản đăng nhập tập trung của cả hệ thống (ADMIN, STAFF, TEACHER, STUDENT). Là nguồn định danh duy nhất (IAM).', '{
 "id":"Khóa chính; cũng là user_id dùng trong JWT (claim sub).",
 "email":"Email đăng nhập, duy nhất toàn hệ thống.",
 "password_hash":"Mật khẩu đã băm BCrypt; tuyệt đối không lưu plaintext.",
 "full_name":"Họ tên hiển thị.",
 "role":"Vai trò RBAC: ADMIN | STAFF | TEACHER | STUDENT.",
 "phone":"Số điện thoại liên hệ.",
 "gender":"Giới tính: MALE | FEMALE | OTHER.",
 "date_of_birth":"Ngày sinh.",
 "avatar_url":"Đường dẫn ảnh đại diện.",
 "must_change_password":"TRUE khi tài khoản do ADMIN tạo hộ; bắt buộc đổi mật khẩu ở lần đăng nhập đầu.",
 "is_active":"FALSE = tài khoản bị khóa, không đăng nhập được.",
 "email_verified_at":"Thời điểm xác thực email khi tự đăng ký học viên (v3.6); NULL = chưa xác thực.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất (tự động cập nhật qua trigger set_updated_at)."}');

SELECT pg_temp.cmt('admission.refresh_tokens', 'Phiên đăng nhập: Refresh Token lưu stateful để xoay vòng (rotation) và thu hồi khi đăng xuất.', '{
 "id":"Khóa chính.",
 "user_id":"Chủ sở hữu token (FK users, xóa user thì xóa token).",
 "token":"Giá trị token hoặc hash của token, duy nhất.",
 "token_hash":"Bản băm SHA-256 của token (tương thích TASK-ADM-01/13; đồng bộ tự động 2 chiều với cột token).",
 "expires_at":"Hạn dùng của token.",
 "is_revoked":"TRUE khi đã đăng xuất hoặc đã bị xoay vòng; không được dùng lại.",
 "created_at":"Thời điểm cấp.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.password_reset_tokens', 'Mã đặt lại mật khẩu dùng một lần, gửi qua email, hiệu lực 15 phút.', '{
 "id":"Khóa chính.",
 "user_id":"Tài khoản yêu cầu đặt lại mật khẩu.",
 "token":"Mã đặt lại hoặc hash của mã, duy nhất.",
 "token_hash":"Bản băm SHA-256 của mã đặt lại (tương thích TASK-ADM-01/13; đồng bộ tự động 2 chiều với cột token).",
 "expires_at":"Hạn dùng (thường created_at + 15 phút).",
 "is_used":"TRUE khi đã dùng; không được dùng lại.",
 "created_at":"Thời điểm tạo mã.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.auth_audit_logs', 'Nhật ký kiểm toán bảo mật: đăng nhập, làm mới token, đổi mật khẩu, khóa/mở khóa tài khoản. Phục vụ cảnh báo brute-force.', '{
 "id":"Khóa chính.",
 "user_id":"Tài khoản liên quan; NULL nếu đăng nhập sai email không tồn tại hoặc user đã bị xóa.",
 "email":"Email thực hiện sự kiện (lưu lại cả khi user_id = NULL để chống brute-force theo email, TASK-ADM-12).",
 "action":"Sự kiện: LOGIN_SUCCESS | LOGIN_FAILED | REFRESH_TOKEN | LOGOUT | PASSWORD_CHANGED | ACCOUNT_LOCKED | ACCOUNT_UNLOCKED.",
 "ip_address":"IP nguồn (hỗ trợ IPv6, tối đa 45 ký tự).",
 "user_agent":"User-Agent của client.",
 "details":"Chi tiết bổ sung dạng text/JSON string.",
 "created_at":"Thời điểm xảy ra sự kiện.",
 "updated_at":"Thời điểm cập nhật bản ghi."}');

SELECT pg_temp.cmt('admission.rooms', 'Danh mục phòng học và sức chứa; dùng để kiểm tra xung đột lịch và sĩ số khi mở lớp.', '{
 "id":"Khóa chính.",
 "code":"Mã phòng duy nhất (vd P101).",
 "name":"Tên phòng hiển thị.",
 "capacity":"Sức chứa tối đa (> 0). Ràng buộc nghiệp vụ: classes.max_students <= capacity (INV-FAC-03).",
 "branch_name":"Tên cơ sở/chi nhánh.",
 "is_active":"FALSE = tạm ngưng sử dụng, không xếp lớp mới.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.courses', 'Danh mục khóa học, học phí, thời lượng và lộ trình tiên quyết.', '{
 "id":"Khóa chính; được tham chiếu logic bởi finance.invoices.course_id và finance.tuition_fees.course_id.",
 "code":"Mã khóa học duy nhất (INV-ADM-02).",
 "name":"Tên khóa học.",
 "description":"Mô tả nội dung khóa học.",
 "level":"Trình độ: Beginner | Intermediate | Advanced (không phân biệt hoa thường).",
 "target_output":"Đầu ra cam kết (vd IELTS 5.0 - 5.5).",
 "duration_weeks":"Thời lượng (tuần), > 0.",
 "tuition_amount":"Học phí niêm yết (VND), >= 0; Finance lấy làm amount của hóa đơn.",
 "prerequisite_course_id":"Khóa tiên quyết (tự tham chiếu); NULL nếu không yêu cầu.",
 "is_active":"FALSE = ngừng tuyển sinh (xóa mềm).",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.classes', 'Lớp học cụ thể: khóa, phòng, giáo viên, lịch học, sĩ số và trạng thái.', '{
 "id":"Khóa chính; được tham chiếu logic bởi student.attendance, learning.materials, learning.grades, finance.refund_requests.",
 "course_id":"Khóa học của lớp (RESTRICT khi xóa).",
 "room_id":"Phòng học; NULL nếu chưa xếp phòng.",
 "teacher_id":"Giáo viên phụ trách = users.id với role TEACHER; NULL nếu chưa phân công.",
 "code":"Mã lớp duy nhất (vd CLS-IELTS-01).",
 "name":"Tên lớp.",
 "day_pattern":"Mẫu ngày học: MON_WED_FRI | TUE_THU_SAT | SAT_SUN | DAILY | CUSTOM.",
 "start_time":"Giờ bắt đầu mỗi buổi.",
 "end_time":"Giờ kết thúc mỗi buổi (> start_time).",
 "start_date":"Ngày khai giảng.",
 "end_date":"Ngày kết thúc (> start_date).",
 "min_students":"Sĩ số tối thiểu để mở lớp; thiếu sẽ hủy lớp.",
 "max_students":"Sĩ số tối đa (>= min_students); chặn duyệt khi đủ (INV-ADM-01).",
 "status":"OPEN | IN_PROGRESS | COMPLETED | CANCELLED.",
 "room":"Snapshot mã/tên phòng học (cột tương thích contract.md & skeleton).",
 "teacher_name":"Snapshot họ tên giáo viên phụ trách (cột tương thích contract.md & skeleton).",
 "schedule":"Snapshot chuỗi lịch học hiển thị nhanh (cột tương thích contract.md).",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.enrollments', 'Đơn đăng ký tuyển sinh: giữ chỗ 48h, duyệt tự động qua Finance hoặc duyệt tay, đồng bộ sang Student.', '{
 "id":"Khóa chính; Finance lưu làm enrollment_id (logical FK).",
 "student_id":"Người đăng ký = users.id (role STUDENT). KHÔNG phải student.students.id vì hồ sơ học viên chỉ được tạo sau khi duyệt (quy ước R-ID-1).",
 "class_id":"Lớp đăng ký.",
 "status":"PENDING | APPROVED | REJECTED | EXPIRED | CANCELLED | CANCELLED_REFUND_PENDING. CANCELLED/REJECTED không được chuyển sang APPROVED (INV-ADM-03).",
 "hold_expires_at":"Hạn giữ chỗ (enrolled_at + 48h); chỉ PENDING còn hạn mới chiếm slot (INV-ENR-01).",
 "invoice_code":"Mã hóa đơn do Finance cấp (logical FK sang finance.invoices.invoice_code).",
 "source":"Nguồn: ONLINE | WALK_IN | HOTLINE | FANPAGE | REFERRAL.",
 "approval_method":"Cách duyệt: ONLINE_PAYMENT | MANUAL_CASH | STAFF_OVERRIDE; NULL khi chưa duyệt.",
 "approved_by_user_id":"Nhân viên duyệt tay (users.id); NULL nếu duyệt tự động.",
 "note":"Ghi chú nghiệp vụ.",
 "notes":"Alias tương thích TASK-ADM-44 (đồng bộ tự động 2 chiều với note).",
 "enrolled_at":"Thời điểm nộp đơn.",
 "approved_at":"Thời điểm duyệt.",
 "enrollment_channel":"Kênh nộp đơn: ONLINE | AT_COUNTER (v3.6, phục vụ BI).",
 "hold_extended_by_user_id":"Nhân viên gia hạn giữ chỗ (v3.6).",
 "hold_extension_reason":"Lý do gia hạn giữ chỗ (v3.6).",
 "student_sync_status":"Trạng thái đồng bộ tạo hồ sơ sang Student Service: NOT_STARTED | PENDING | SYNCED | FAILED (v3.6).",
 "is_synced_to_student":"Cờ boolean tương thích TASK-ADM-47 (TRUE khi student_sync_status = SYNCED).",
 "created_at":"Thời điểm tạo bản ghi.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.test_sets', 'Bộ đề khảo thí trình độ đầu vào.', '{
 "id":"Khóa chính.",
 "code":"Mã bộ đề duy nhất (vd PLACEMENT-GEN-01, tương thích TASK-ADM-30 / API_CATALOG).",
 "title":"Tiêu đề bộ đề.",
 "description":"Mô tả chi tiết bộ đề khảo thí.",
 "duration_minutes":"Thời gian làm bài (phút), > 0.",
 "total_max_score":"Tổng điểm tối đa của bộ đề (mặc định 100.0).",
 "is_active":"Chỉ bộ đề active mới được phát cho thí sinh.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.test_questions', 'Câu hỏi trắc nghiệm 4 đáp án thuộc 3 kỹ năng. Đáp án đúng không bao giờ trả về API public.', '{
 "id":"Khóa chính.",
 "test_set_id":"Bộ đề chứa câu hỏi (xóa bộ đề thì xóa câu hỏi).",
 "skill_type":"Kỹ năng: LISTENING | READING | GRAMMAR.",
 "order_index":"Thứ tự hiển thị trong đề.",
 "display_order":"Alias thứ tự hiển thị (tương thích TASK-ADM-30, đồng bộ tự động với order_index).",
 "audio_url":"File nghe (dùng cho LISTENING).",
 "passage_text":"Đoạn văn (dùng cho READING).",
 "question_text":"Nội dung câu hỏi.",
 "option_a":"Đáp án A.",
 "option_b":"Đáp án B.",
 "option_c":"Đáp án C.",
 "option_d":"Đáp án D.",
 "correct_option":"Đáp án đúng A | B | C | D; chỉ server dùng để chấm (INV-TST-01).",
 "score_weight":"Trọng số điểm của câu, > 0.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.test_recommendation_rules', 'Ma trận quy đổi điểm test sang trình độ và khóa học khuyến nghị.', '{
 "id":"Khóa chính.",
 "test_set_id":"Bộ đề áp dụng.",
 "min_score":"Điểm tối thiểu của dải (>= 0).",
 "max_score":"Điểm tối đa của dải (> min_score). Các dải không được chồng lấn (kiểm tra ở tầng ứng dụng, lỗi ADM_RULE_OVERLAP).",
 "assessed_level":"Trình độ đánh giá (vd Beginner).",
 "recommended_course_id":"Khóa học khuyến nghị.",
 "advice_text":"Lời khuyên hiển thị cho thí sinh.",
 "advice_note":"Alias lời khuyên tư vấn (tương thích TASK-ADM-32, đồng bộ tự động với advice_text).",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.test_attempts', 'Lượt làm bài theo giờ server (v3.6): chống nộp quá hạn và chấm lại cùng một lượt.', '{
 "id":"Khóa chính.",
 "user_id":"Thí sinh (users.id).",
 "test_set_id":"Bộ đề đã bốc.",
 "status":"STARTED | SUBMITTED | EXPIRED.",
 "started_at":"Giờ server khi bắt đầu.",
 "expires_at":"Hạn nộp bài = started_at + duration_minutes (> started_at).",
 "submitted_at":"Giờ nộp bài; NULL nếu chưa nộp.",
 "created_at":"Thời điểm tạo lượt thi.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.test_submissions', 'Kết quả bài khảo thí đã chấm, kèm bài làm chi tiết.', '{
 "id":"Khóa chính.",
 "attempt_id":"Lượt thi tương ứng (v3.6); UNIQUE nên mỗi lượt chỉ có một kết quả (INV-TST-02).",
 "user_id":"Thí sinh.",
 "test_set_id":"Bộ đề đã làm.",
 "full_name":"Snapshot họ tên thí sinh tại thời điểm nộp bài (TASK-ADM-31).",
 "email":"Snapshot email thí sinh (TASK-ADM-31).",
 "phone":"Snapshot số điện thoại thí sinh (TASK-ADM-31).",
 "listening_score":"Điểm kỹ năng Nghe (>= 0).",
 "reading_score":"Điểm kỹ năng Đọc (>= 0).",
 "grammar_score":"Điểm kỹ năng Ngữ pháp (>= 0).",
 "total_score":"Tổng điểm do server tính.",
 "assessed_level":"Trình độ suy ra từ ma trận khuyến nghị.",
 "recommended_course_id":"Khóa học được đề xuất; NULL nếu không có luật khớp.",
 "answers_detail":"JSONB: danh sách {question_id, selected_option, is_correct}.",
 "started_at":"Thời điểm bắt đầu làm bài (TASK-ADM-31).",
 "submitted_at":"Thời điểm nộp.",
 "created_at":"Thời điểm tạo bản ghi.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('admission.processed_finance_events', 'Sổ đối soát callback từ Finance (v3.6): đảm bảo idempotency và ghi nhận thanh toán đến muộn.', '{
 "id":"Khóa chính.",
 "enrollment_id":"Đơn đăng ký nhận callback.",
 "finance_event_id":"Mã sự kiện do Finance gửi, duy nhất; callback trùng trả 200 OK và không duyệt lại (INV-ENR-02).",
 "paid_at":"Thời điểm Finance ghi nhận thanh toán.",
 "processed_at":"Thời điểm Admission xử lý.",
 "created_at":"Thời điểm tạo bản ghi.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

-- ----------------------------------- STUDENT -----------------------------------
SELECT pg_temp.cmt('student.students', 'Hồ sơ học vụ của học viên; tạo tự động khi enrollment được APPROVED.', '{
 "id":"Khóa chính = student.students.id; được tham chiếu logic bởi learning.grades.student_id.",
 "user_id":"Logical FK sang admission.users.id (cầu nối giữa hồ sơ học viên và tài khoản). KHÔNG có FK vật lý.",
 "student_code":"Mã học viên duy nhất, dạng HV-YYYY-NNNN, sinh tự động qua sequence nếu không truyền (INV-STU-05).",
 "full_name":"Họ tên học viên.",
 "email":"Email duy nhất (INV-STU-06).",
 "phone":"Số điện thoại.",
 "date_of_birth":"Ngày sinh.",
 "address":"Địa chỉ liên hệ.",
 "status":"STUDYING | RESERVED | DROPPED | GRADUATED (INV-STU-02). RESERVED tối đa 6 tháng (INV-STU-04).",
 "created_at":"Thời điểm tạo hồ sơ.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('student.attendance', 'Điểm danh theo buổi học. Mỗi học viên chỉ có một bản ghi cho mỗi buổi của mỗi lớp (INV-STU-01).', '{
 "id":"Khóa chính.",
 "class_id":"Logical FK sang admission.classes.id.",
 "student_id":"Học viên (FK nội bộ student.students, xóa hồ sơ thì xóa điểm danh).",
 "session_date":"Ngày của buổi học.",
 "status":"PRESENT | ABSENT | LATE | EXCUSED.",
 "notes":"Ghi chú (lý do vắng, đi trễ...).",
 "created_at":"Thời điểm ghi nhận.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('student.status_requests', 'Yêu cầu học vụ của học viên: bảo lưu, chuyển lớp, bỏ học; do STAFF duyệt.', '{
 "id":"Khóa chính.",
 "student_id":"Học viên gửi yêu cầu.",
 "request_type":"RESERVE (bảo lưu) | TRANSFER (chuyển lớp) | DROP (bỏ học).",
 "reason":"Lý do học viên nêu.",
 "class_id":"Logical FK sang admission.classes.id: lớp đang học. Cột mở rộng của bản tổng hợp.",
 "target_class_id":"Logical FK sang admission.classes.id: lớp muốn chuyển đến (chỉ dùng với TRANSFER). Cột mở rộng.",
 "status":"PENDING | APPROVED | REJECTED.",
 "reviewed_by":"Logical FK sang admission.users.id: nhân viên duyệt. Cột mở rộng.",
 "reviewed_note":"Ghi chú của người duyệt. Cột mở rộng.",
 "created_at":"Thời điểm gửi yêu cầu.",
 "updated_at":"Thời điểm cập nhật gần nhất.",
 "reviewed_at":"Thời điểm duyệt. Cột mở rộng."}');

-- ----------------------------------- LEARNING ----------------------------------
SELECT pg_temp.cmt('learning.materials', 'Học liệu (tài liệu, video, bài tập, slide) gắn với từng lớp.', '{
 "id":"Khóa chính.",
 "class_id":"Logical FK sang admission.classes.id.",
 "title":"Tiêu đề học liệu.",
 "description":"Mô tả.",
 "material_type":"DOCUMENT | VIDEO | EXERCISE | SLIDE.",
 "file_url":"Đường dẫn file hoặc link; không được rỗng (INV-LRN-04).",
 "original_filename":"Tên file gốc khi upload.",
 "file_size":"Kích thước file (byte), >= 0.",
 "uploaded_by":"Logical FK sang admission.users.id (giáo viên upload). Cột mở rộng.",
 "uploaded_at":"Thời điểm upload.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('learning.grades', 'Điểm đánh giá của học viên theo từng đầu điểm trong lớp.', '{
 "id":"Khóa chính.",
 "class_id":"Logical FK sang admission.classes.id.",
 "student_id":"Logical FK sang student.students.id (KHÔNG phải users.id).",
 "assessment_name":"Tên đầu điểm (Quiz 1, Midterm...); duy nhất theo (lớp, học viên) (INV-LRN-06).",
 "score":"Điểm đạt được, >= 0 và <= max_score (INV-LRN-01).",
 "max_score":"Thang điểm tối đa, > 0, mặc định 100.",
 "weight":"Trọng số trong (0, 1], dùng tính điểm trung bình có trọng số (NULL được view coi là 1.0).",
 "feedback":"Nhận xét của giáo viên.",
 "graded_by":"Logical FK sang admission.users.id: giáo viên chấm; phải là giáo viên của lớp (INV-LRN-03). Cột mở rộng.",
 "graded_at":"Thời điểm chấm.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

-- ----------------------------------- FINANCE -----------------------------------
SELECT pg_temp.cmt('finance.tuition_fees', 'Biểu phí chuẩn theo khóa học (FIN-01 / contract.md); hỗ trợ tra cứu học phí niêm yết và mức ưu đãi mặc định.', '{
 "id":"Khóa chính.",
 "course_id":"Logical FK sang admission.courses.id (UNIQUE).",
 "course_name":"Tên khóa học hiển thị trên bảng giá.",
 "amount":"Mức học phí niêm yết theo contract.md (VND), >= 0; đồng bộ tự động với base_amount.",
 "base_amount":"Mức học phí gốc (VND), >= 0; đồng bộ tự động với amount.",
 "currency":"Đơn vị tiền tệ (mặc định VND).",
 "discount_pct":"Phần trăm giảm giá mặc định (0-100).",
 "note":"Ghi chú biểu phí (theo contract.md; đồng bộ tự động với description).",
 "description":"Mô tả chính sách học phí của khóa.",
 "is_active":"TRUE = đang áp dụng.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('finance.vouchers', 'Chiến dịch mã giảm giá học phí.', '{
 "id":"Khóa chính.",
 "code":"Mã voucher duy nhất.",
 "campaign_name":"Tên chiến dịch.",
 "discount_percent":"Giảm theo phần trăm (0-100).",
 "discount_amount":"Giảm số tiền cố định (VND).",
 "min_order_value":"Giá trị hóa đơn tối thiểu để áp dụng.",
 "max_discount_amount":"Mức giảm tối đa khi giảm theo phần trăm; NULL = không giới hạn.",
 "max_uses":"Tổng lượt dùng tối đa.",
 "current_uses":"Số lượt đã dùng (<= max_uses). Phải SELECT ... FOR UPDATE trước khi tăng (INV-FIN-04).",
 "valid_from":"Bắt đầu hiệu lực.",
 "expires_at":"Hết hiệu lực (> valid_from).",
 "is_active":"Công tắc bật/tắt chiến dịch.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('finance.invoices', 'Hóa đơn học phí. Sau khi PAID/REFUNDED không được sửa amount, discount_applied, final_amount (INV-FIN-01).', '{
 "id":"Khóa chính.",
 "invoice_code":"Mã hóa đơn duy nhất (sinh tự động nếu không truyền); Admission lưu lại ở enrollments.invoice_code.",
 "student_id":"users.id của người học (quy ước R-ID-1) vì hóa đơn được sinh khi đơn còn PENDING, lúc chưa có hồ sơ student.students.",
 "course_id":"Logical FK sang admission.courses.id.",
 "enrollment_id":"Logical FK sang admission.enrollments.id; NULL với hóa đơn học lại độc lập.",
 "invoice_type":"COURSE_PACKAGE | STANDALONE_RETAKE | MIXED.",
 "amount":"Giá trị trước giảm giá (>= 0).",
 "voucher_id":"Voucher đã áp dụng; NULL nếu không có.",
 "discount_applied":"Số tiền được giảm (>= 0).",
 "final_amount":"Số phải thu = amount - discount_applied (tự động tính qua trigger nếu không truyền).",
 "paid_amount":"Tổng đã thu, 0 <= paid_amount <= final_amount.",
 "remaining_amount":"Cột sinh tự động = final_amount - paid_amount.",
 "status":"PENDING | UNPAID | PARTIAL | PAID | OVERDUE | CANCELLED | REFUNDED.",
 "due_date":"Hạn thanh toán. Cột mở rộng của bản tổng hợp.",
 "notes":"Ghi chú.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất.",
 "paid_at":"Thời điểm thanh toán đủ."}');

SELECT pg_temp.cmt('finance.invoice_items', 'Khoản mục cấu thành hóa đơn (gói học phí, phí học lại).', '{
 "id":"Khóa chính.",
 "invoice_id":"Hóa đơn chứa khoản mục (xóa hóa đơn thì xóa khoản mục).",
 "course_id":"Logical FK sang admission.courses.id.",
 "item_type":"TUITION_PACKAGE | RETAKE_FEE.",
 "description":"Mô tả khoản mục.",
 "quantity":"Số lượng (> 0).",
 "unit_price":"Đơn giá (>= 0).",
 "line_total":"Thành tiền = quantity x unit_price (CHECK).",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('finance.installments', 'Lịch đóng học phí nhiều đợt hoặc đặt cọc. Tổng amount_due các kỳ bằng final_amount hóa đơn (INV-FIN-02).', '{
 "id":"Khóa chính.",
 "invoice_id":"Hóa đơn sở hữu kỳ.",
 "installment_no":"Số thứ tự kỳ (>= 1), duy nhất theo hóa đơn.",
 "payment_ref_code":"Mã tham chiếu thanh toán duy nhất (nội dung chuyển khoản VietQR).",
 "amount_due":"Số phải đóng trong kỳ (> 0).",
 "amount_paid":"Số đã đóng (>= 0).",
 "due_date":"Ngày đến hạn.",
 "status":"PENDING | PARTIAL | PAID | OVERDUE | CANCELLED.",
 "paid_at":"Thời điểm đóng đủ kỳ.",
 "created_at":"Thời điểm tạo.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('finance.payments', 'Phiếu thu: tiền mặt, VietQR hoặc cấn trừ ví. bank_transaction_id / transaction_ref UNIQUE đảm bảo idempotency (INV-FIN-05).', '{
 "id":"Khóa chính.",
 "payment_code":"Mã phiếu thu duy nhất (sinh tự động nếu không truyền).",
 "invoice_id":"Hóa đơn được thanh toán.",
 "installment_id":"Kỳ thanh toán được gạch nợ; NULL nếu thu không theo kỳ.",
 "payment_method":"CASH | VIETQR | WALLET_OFFSET | BANK_TRANSFER | CARD | MOMO.",
 "amount":"Số tiền giao dịch (> 0).",
 "bank_transaction_id":"Mã giao dịch ngân hàng, duy nhất; giao dịch trùng trả 200 OK nhưng không ghi lần hai.",
 "transaction_ref":"Alias mã tham chiếu giao dịch (tương thích contract.md & skeleton; đồng bộ 2 chiều với bank_transaction_id).",
 "qr_reference_code":"Mã tham chiếu trong nội dung QR.",
 "webhook_payload":"JSONB: payload gốc từ ngân hàng để đối soát.",
 "received_by_staff_id":"Logical FK sang admission.users.id: nhân viên thu tiền mặt.",
 "status":"PENDING | SUCCESS | FAILED.",
 "paid_at":"Thời điểm thanh toán.",
 "created_at":"Thời điểm tạo bản ghi.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('finance.student_wallets', 'Ví bảo lưu học phí, mỗi học viên một ví. Không cho rút tiền (INV-FIN-06).', '{
 "id":"Khóa chính.",
 "student_id":"users.id của học viên (R-ID-1), duy nhất = mỗi người một ví.",
 "balance":"Số dư (>= 0).",
 "is_withdrawable":"Luôn FALSE; số dư chỉ giảm bằng TUITION_OFFSET.",
 "status":"ACTIVE | FROZEN.",
 "created_at":"Thời điểm mở ví.",
 "updated_at":"Thời điểm cập nhật số dư."}');

SELECT pg_temp.cmt('finance.wallet_transactions', 'Sổ cái ví bảo lưu: cộng tiền bảo lưu, cấn trừ học phí, điều chỉnh.', '{
 "id":"Khóa chính.",
 "wallet_id":"Ví liên quan.",
 "tx_type":"RESERVATION_CREDIT | TUITION_OFFSET | ADJUSTMENT.",
 "amount":"Số tiền (> 0); chiều cộng/trừ do tx_type quyết định.",
 "related_invoice_id":"Hóa đơn được cấn trừ (khi TUITION_OFFSET).",
 "reason":"Lý do giao dịch (bắt buộc để truy vết).",
 "created_by_staff_id":"Logical FK sang admission.users.id: nhân viên thực hiện.",
 "created_at":"Thời điểm ghi nhận.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');

SELECT pg_temp.cmt('finance.refund_requests', 'Yêu cầu hoàn học phí, duyệt 3 cấp: tư vấn tạo, kế toán xác minh, giám đốc duyệt, kế toán giải ngân (INV-FIN-07/08).', '{
 "id":"Khóa chính.",
 "refund_code":"Mã yêu cầu duy nhất.",
 "invoice_id":"Hóa đơn cần hoàn.",
 "student_id":"users.id của học viên (R-ID-1).",
 "class_id":"Logical FK sang admission.classes.id.",
 "total_sessions":"Tổng số buổi của khóa (> 0).",
 "attended_sessions":"Số buổi đã học; phải <= max_allowed_sessions.",
 "max_allowed_sessions":"Ngưỡng buổi tối đa được xét hoàn (mặc định 3).",
 "paid_amount_snapshot":"Ảnh chụp số đã thanh toán tại thời điểm yêu cầu.",
 "refundable_percent":"Tỷ lệ hoàn (0-100).",
 "deduction_amount":"Số tiền khấu trừ.",
 "requested_refund_amount":"Số tiền đề nghị hoàn (>= 0).",
 "approved_refund_amount":"Số tiền được duyệt (>= 0).",
 "reason":"Lý do hoàn.",
 "bank_name":"Ngân hàng nhận.",
 "bank_account_no":"Số tài khoản nhận.",
 "bank_account_holder":"Chủ tài khoản.",
 "transaction_ref":"Mã chứng từ chuyển khoản giải ngân hoàn tiền (FIN-F08 / API_CATALOG).",
 "status":"PENDING_ACCOUNTANT | PENDING_DIRECTOR | APPROVED | REJECTED | COMPLETED.",
 "requested_by_staff_id":"Logical FK sang admission.users.id: nhân viên tạo (cấp 1).",
 "requested_at":"Thời điểm tạo.",
 "verified_by_accountant_id":"Logical FK sang admission.users.id: kế toán xác minh (cấp 2).",
 "verified_at":"Thời điểm xác minh.",
 "accountant_note":"Ghi chú kế toán.",
 "approved_by_director_id":"Logical FK sang admission.users.id (ADMIN): giám đốc duyệt (cấp 3).",
 "approved_at":"Thời điểm duyệt.",
 "director_note":"Ghi chú giám đốc.",
 "completed_at":"Thời điểm giải ngân xong.",
 "updated_at":"Thời điểm cập nhật gần nhất."}');


-- ============================================================================
-- BƯỚC 3: SEED DATA – DỮ LIỆU MẪU CHO DEMO (tài khoản theo README.md)
-- ============================================================================
-- Quy ước ID (R-ID-1): trong schema admission & finance, `student_id` = admission.users.id
-- (tài khoản đăng nhập). Trong schema student & learning, `student_id` = student.students.id.
-- Cầu nối: student.students.user_id = admission.users.id.
-- Seed KHÔNG hard-code ID: mọi tham chiếu đều tra cứu theo code/email để chạy đúng ở mọi thứ tự sequence.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---- SEED: admission.users (mật khẩu BCrypt, khớp README: admin123/staff123/teacher123/student123) ----
INSERT INTO admission.users (email, password_hash, full_name, role, phone, gender, is_active, email_verified_at)
VALUES
    ('admin@english.edu.vn',    crypt('admin123',   gen_salt('bf', 10)), 'Quản Trị Viên',        'ADMIN',   '0901234567', 'MALE',   TRUE, CURRENT_TIMESTAMP),
    ('staff@english.edu.vn',    crypt('staff123',   gen_salt('bf', 10)), 'Nguyễn Thị Học Vụ',    'STAFF',   '0902345678', 'FEMALE', TRUE, CURRENT_TIMESTAMP),
    ('teacher@english.edu.vn',  crypt('teacher123', gen_salt('bf', 10)), 'Thầy John Smith',      'TEACHER', '0903456789', 'MALE',   TRUE, CURRENT_TIMESTAMP),
    ('teacher2@english.edu.vn', crypt('teacher123', gen_salt('bf', 10)), 'Cô Sarah Connor',      'TEACHER', '0903456790', 'FEMALE', TRUE, CURRENT_TIMESTAMP),
    ('student@english.edu.vn',  crypt('student123', gen_salt('bf', 10)), 'Trần Văn Nam',         'STUDENT', '0904567890', 'MALE',   TRUE, CURRENT_TIMESTAMP),
    ('mai.le@gmail.com',        crypt('student123', gen_salt('bf', 10)), 'Lê Thị Mai',           'STUDENT', '0912334455', 'FEMALE', TRUE, CURRENT_TIMESTAMP),
    ('bao.pham@gmail.com',      crypt('student123', gen_salt('bf', 10)), 'Phạm Quốc Bảo',        'STUDENT', '0987654321', 'MALE',   TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- ---- SEED: admission.rooms ----
INSERT INTO admission.rooms (code, name, capacity, branch_name)
VALUES
    ('P101',     'Phòng 101 - Cơ sở 1', 25, 'Cơ sở 1 - Cầu Giấy'),
    ('P201',     'Phòng 201 - Cơ sở 1', 20, 'Cơ sở 1 - Cầu Giấy'),
    ('P302',     'Phòng 302 - Cơ sở 1', 18, 'Cơ sở 1 - Cầu Giấy'),
    ('P102-CS2', 'Phòng 102 - Cơ sở 2', 30, 'Cơ sở 2 - Đống Đa')
ON CONFLICT (code) DO NOTHING;

-- ---- SEED: admission.courses ----
INSERT INTO admission.courses (code, name, description, level, target_output, duration_weeks, tuition_amount)
VALUES
    ('ENG-COMM-BAS',    'Tiếng Anh Giao Tiếp Phản Xạ', 'Rèn luyện phát âm chuẩn IPA và tự tin giao tiếp hàng ngày.',        'Beginner',     'A2 - B1 CEFR',    10, 3800000),
    ('ENG-IELTS-FOUND', 'IELTS Foundation 4.5 - 5.5',  'Xây dựng nền tảng ngữ pháp, từ vựng và 4 kỹ năng cơ bản.',          'Intermediate', 'IELTS 5.0 - 5.5', 12, 6500000),
    ('ENG-IELTS-INT',   'IELTS Intensive 6.5+',        'Luyện đề chuyên sâu và chiến thuật làm bài thi thật.',              'Advanced',     'IELTS 6.5 - 7.5', 14, 8500000),
    ('ENG-TOEIC-600',   'TOEIC Cấp Tốc 600+',          'Ôn thi TOEIC nghe đọc cấp tốc chuẩn format ETS.',                   'Intermediate', 'TOEIC 600 - 700',  8, 4200000)
ON CONFLICT (code) DO NOTHING;

-- Lộ trình tiên quyết: IELTS Intensive yêu cầu hoàn thành IELTS Foundation
UPDATE admission.courses
SET prerequisite_course_id = (SELECT id FROM admission.courses WHERE code = 'ENG-IELTS-FOUND')
WHERE code = 'ENG-IELTS-INT' AND prerequisite_course_id IS NULL;

-- ---- SEED: admission.classes (tra cứu theo code, không hard-code ID) ----
INSERT INTO admission.classes (course_id, room_id, teacher_id, code, name, day_pattern, start_time, end_time, start_date, end_date, min_students, max_students, status, room, teacher_name, schedule)
SELECT c.id, r.id, u.id, v.code, v.name, v.day_pattern, v.start_time::time, v.end_time::time, v.start_date::date, v.end_date::date, v.min_students, v.max_students, 'OPEN',
       r.code || ' (' || r.name || ')', u.full_name, v.day_pattern || ' ' || v.start_time || '-' || v.end_time
FROM (VALUES
    ('CLS-IELTS-01', 'IELTS Foundation K28', 'ENG-IELTS-FOUND', 'P201',     'teacher@english.edu.vn',  'MON_WED_FRI', '18:00', '19:30', '2026-11-01', '2027-01-25',  8, 20),
    ('CLS-IELTS-02', 'IELTS Intensive K15',  'ENG-IELTS-INT',   'P302',     'teacher2@english.edu.vn', 'TUE_THU_SAT', '19:30', '21:00', '2026-11-05', '2027-02-15',  8, 18),
    ('CLS-TOEIC-01', 'TOEIC 600+ Tối',       'ENG-TOEIC-600',   'P102-CS2', 'teacher@english.edu.vn',  'MON_WED_FRI', '19:30', '21:00', '2026-11-10', '2027-01-10', 10, 25)
) AS v(code, name, course_code, room_code, teacher_email, day_pattern, start_time, end_time, start_date, end_date, min_students, max_students)
JOIN admission.courses c ON c.code = v.course_code
JOIN admission.rooms   r ON r.code = v.room_code
JOIN admission.users   u ON u.email = v.teacher_email
ON CONFLICT (code) DO NOTHING;

-- ---- SEED: bộ đề khảo thí đầu vào ----
INSERT INTO admission.test_sets (code, title, description, duration_minutes, total_max_score, is_active)
SELECT 'PLACEMENT-GEN-01', 'Bài Khảo thí Trình độ Đầu vào Tổng quát', 'Đánh giá nhanh 3 kỹ năng Nghe - Đọc - Ngữ pháp để xếp lớp phù hợp.', 45, 100.0, TRUE
WHERE NOT EXISTS (SELECT 1 FROM admission.test_sets WHERE title = 'Bài Khảo thí Trình độ Đầu vào Tổng quát');

INSERT INTO admission.test_questions
    (test_set_id, skill_type, order_index, audio_url, passage_text, question_text, option_a, option_b, option_c, option_d, correct_option, score_weight)
SELECT ts.id, q.skill_type, q.order_index, q.audio_url, q.passage_text, q.question_text, q.a, q.b, q.c, q.d, q.correct, 1.0
FROM admission.test_sets ts
CROSS JOIN (VALUES
    ('LISTENING', 1, 'https://example.com/audio/placement-1.mp3', NULL::text,
     'What time does the speaker say the class starts?', '5:00', '6:00', '7:00', '8:00', 'B'),
    ('READING',   2, NULL::text, 'English is spoken worldwide. Learning it opens doors to global careers and education.',
     'According to the passage, what is the main benefit of learning English?',
     'Better food', 'More holidays', 'Access to global opportunities', 'Cheaper travel', 'C'),
    ('GRAMMAR',   3, NULL::text, NULL::text,
     'She ______ to the English center every Monday.', 'go', 'goes', 'went', 'going', 'B'),
    ('GRAMMAR',   4, NULL::text, NULL::text,
     'If I ______ enough money, I would take the IELTS Intensive course.', 'have', 'had', 'will have', 'having', 'B')
) AS q(skill_type, order_index, audio_url, passage_text, question_text, a, b, c, d, correct)
WHERE ts.title = 'Bài Khảo thí Trình độ Đầu vào Tổng quát'
  AND NOT EXISTS (SELECT 1 FROM admission.test_questions x WHERE x.test_set_id = ts.id);

INSERT INTO admission.test_recommendation_rules (test_set_id, min_score, max_score, assessed_level, recommended_course_id, advice_text)
SELECT ts.id, r.min_score, r.max_score, r.level, c.id, r.advice
FROM admission.test_sets ts
CROSS JOIN (VALUES
    (0.0,  49.9,  'Beginner',     'ENG-COMM-BAS',    'Bạn nên bắt đầu với khóa Tiếng Anh Giao Tiếp Phản Xạ để lấy lại căn bản phát âm và ngữ pháp.'),
    (50.0, 74.9,  'Intermediate', 'ENG-IELTS-FOUND', 'Trình độ hiện tại phù hợp với khóa IELTS Foundation 4.5 - 5.5.'),
    (75.0, 100.0, 'Advanced',     'ENG-IELTS-INT',   'Nền tảng rất vững! Hãy đăng ký IELTS Intensive 6.5+ để bứt phá band điểm.')
) AS r(min_score, max_score, level, course_code, advice)
JOIN admission.courses c ON c.code = r.course_code
WHERE ts.title = 'Bài Khảo thí Trình độ Đầu vào Tổng quát'
  AND NOT EXISTS (SELECT 1 FROM admission.test_recommendation_rules x WHERE x.test_set_id = ts.id);

-- ---- SEED: admission.enrollments ----
-- Nam & Mai: đã APPROVED (đã thanh toán). Bảo: PENDING đang giữ chỗ 48h cho lớp IELTS Intensive.
INSERT INTO admission.enrollments
    (student_id, class_id, status, hold_expires_at, invoice_code, source, approval_method, approved_by_user_id, enrolled_at, approved_at, enrollment_channel, student_sync_status)
SELECT u.id, c.id, v.status,
       CASE WHEN v.status = 'PENDING' THEN CURRENT_TIMESTAMP + INTERVAL '48 hours' END,
       v.invoice_code, v.source, v.approval_method,
       CASE WHEN v.approval_method = 'MANUAL_CASH' THEN (SELECT id FROM admission.users WHERE email = 'staff@english.edu.vn') END,
       CURRENT_TIMESTAMP - INTERVAL '3 days',
       CASE WHEN v.status = 'APPROVED' THEN CURRENT_TIMESTAMP - INTERVAL '2 days' END,
       v.channel, v.sync_status
FROM (VALUES
    ('student@english.edu.vn', 'CLS-IELTS-01', 'APPROVED', 'INV-2026-001', 'ONLINE',  'ONLINE_PAYMENT', 'ONLINE',     'SYNCED'),
    ('mai.le@gmail.com',       'CLS-IELTS-01', 'APPROVED', 'INV-2026-002', 'WALK_IN', 'MANUAL_CASH',    'AT_COUNTER', 'SYNCED'),
    ('bao.pham@gmail.com',     'CLS-IELTS-02', 'PENDING',  'INV-2026-003', 'ONLINE',  NULL,             'ONLINE',     'NOT_STARTED')
) AS v(email, class_code, status, invoice_code, source, approval_method, channel, sync_status)
JOIN admission.users   u ON u.email = v.email
JOIN admission.classes c ON c.code  = v.class_code
ON CONFLICT (student_id, class_id) DO NOTHING;


-- ---- SEED: student.students (hồ sơ tạo khi enrollment APPROVED; Bảo là học viên cũ) ----
INSERT INTO student.students (user_id, student_code, full_name, email, phone, date_of_birth, address, status)
SELECT u.id, v.code, u.full_name, u.email, u.phone, v.dob::date, v.addr, 'STUDYING'
FROM (VALUES
    ('student@english.edu.vn', 'HV-2026-0001', '2004-05-15', '123 Cầu Giấy, Hà Nội'),
    ('mai.le@gmail.com',       'HV-2026-0002', '2003-08-20', '45 Đống Đa, Hà Nội'),
    ('bao.pham@gmail.com',     'HV-2026-0003', '2002-11-10', '88 Hai Bà Trưng, Hà Nội')
) AS v(email, code, dob, addr)
JOIN admission.users u ON u.email = v.email
ON CONFLICT (email) DO NOTHING;

-- ---- SEED: student.attendance (lớp CLS-IELTS-01, thứ 2 và thứ 4 đầu khóa) ----
INSERT INTO student.attendance (class_id, student_id, session_date, status, notes)
SELECT c.id, s.id, v.session_date::date, v.status, v.notes
FROM (VALUES
    ('CLS-IELTS-01', 'HV-2026-0001', '2026-11-02', 'PRESENT', NULL),
    ('CLS-IELTS-01', 'HV-2026-0002', '2026-11-02', 'LATE',    'Đến trễ 10 phút'),
    ('CLS-IELTS-01', 'HV-2026-0001', '2026-11-04', 'PRESENT', NULL),
    ('CLS-IELTS-01', 'HV-2026-0002', '2026-11-04', 'ABSENT',  'Nghỉ không phép')
) AS v(class_code, student_code, session_date, status, notes)
JOIN admission.classes c ON c.code = v.class_code
JOIN student.students  s ON s.student_code = v.student_code
ON CONFLICT (class_id, student_id, session_date) DO NOTHING;

-- ---- SEED: student.status_requests (1 yêu cầu chuyển lớp đang chờ duyệt) ----
INSERT INTO student.status_requests (student_id, request_type, reason, class_id, target_class_id, status)
SELECT s.id, 'TRANSFER', 'Trùng lịch làm thêm vào tối thứ 2/4/6, xin chuyển sang lớp thứ 3/5/7.',
       (SELECT id FROM admission.classes WHERE code = 'CLS-IELTS-01'),
       (SELECT id FROM admission.classes WHERE code = 'CLS-IELTS-02'),
       'PENDING'
FROM student.students s
WHERE s.student_code = 'HV-2026-0002'
  AND NOT EXISTS (SELECT 1 FROM student.status_requests x WHERE x.student_id = s.id AND x.request_type = 'TRANSFER');


-- ---- SEED: learning.materials ----
INSERT INTO learning.materials (class_id, title, description, material_type, file_url, original_filename, file_size, uploaded_by)
SELECT c.id, v.title, v.descr, v.mtype, v.url, v.fname, v.fsize,
       (SELECT id FROM admission.users WHERE email = 'teacher@english.edu.vn')
FROM (VALUES
    ('CLS-IELTS-01', 'Giáo trình IELTS Foundation Unit 1-4',         'Slide và bài tập từ vựng chủ đề Daily Routine',    'DOCUMENT', 'https://example.com/materials/ielts-u1-4.pdf',      'ielts-u1-4.pdf',      2457600::bigint),
    ('CLS-IELTS-01', 'Audio luyện nghe Cambridge Practice Test 1',   'File nghe định dạng MP3 cho bài tập về nhà',       'DOCUMENT', 'https://example.com/materials/listening-test1.mp3', 'listening-test1.mp3', 8912896::bigint),
    ('CLS-IELTS-01', 'Bài tập về nhà tuần 1',                        'Bộ bài tập ngữ pháp thì hiện tại',                 'EXERCISE', 'https://example.com/materials/homework-w1.pdf',     'homework-w1.pdf',      512000::bigint)
) AS v(class_code, title, descr, mtype, url, fname, fsize)
JOIN admission.classes c ON c.code = v.class_code
WHERE NOT EXISTS (SELECT 1 FROM learning.materials m WHERE m.class_id = c.id AND m.title = v.title);

-- ---- SEED: learning.grades (student_id = student.students.id) ----
INSERT INTO learning.grades (class_id, student_id, assessment_name, score, max_score, weight, feedback, graded_by)
SELECT c.id, s.id, v.assessment, v.score, v.max_score, v.weight, v.feedback,
       (SELECT id FROM admission.users WHERE email = 'teacher@english.edu.vn')
FROM (VALUES
    ('CLS-IELTS-01', 'HV-2026-0001', 'Quiz 1 (Grammar)',                  9.0::numeric,  10.0::numeric, 0.2::numeric, 'Nắm chắc thì hiện tại.'),
    ('CLS-IELTS-01', 'HV-2026-0001', 'Midterm Test (Reading & Listening)', 82.5::numeric, 100.0::numeric, 0.8::numeric, 'Kỹ năng nghe tốt, cần chú ý phần Matching Headings.'),
    ('CLS-IELTS-01', 'HV-2026-0002', 'Midterm Test (Reading & Listening)', 78.0::numeric, 100.0::numeric, 0.8::numeric, 'Cần cải thiện từ vựng học thuật.')
) AS v(class_code, student_code, assessment, score, max_score, weight, feedback)
JOIN admission.classes c ON c.code = v.class_code
JOIN student.students  s ON s.student_code = v.student_code
ON CONFLICT (class_id, student_id, assessment_name) DO NOTHING;


-- ---- SEED: finance.tuition_fees (đồng bộ từ admission.courses) ----
INSERT INTO finance.tuition_fees (course_id, course_name, amount, base_amount, currency, discount_pct, note, description, is_active)
SELECT c.id, c.name, c.tuition_amount, c.tuition_amount, 'VND', 0,
       'Học phí niêm yết trọn khóa ' || c.name || ' (' || c.duration_weeks || ' tuần)',
       'Học phí niêm yết trọn khóa ' || c.name || ' (' || c.duration_weeks || ' tuần)', TRUE
FROM admission.courses c
ON CONFLICT (course_id) DO NOTHING;

-- ---- SEED: finance.vouchers ----
INSERT INTO finance.vouchers (code, campaign_name, discount_percent, discount_amount, min_order_value, max_discount_amount, max_uses, current_uses, expires_at)
VALUES
    ('KHAI_GIANG_2026', 'Ưu đãi Khai giảng tháng 11/2026',     10,      0, 3000000, 1000000,  50, 1, '2026-11-30 23:59:59'),
    ('GIOI_THIEU',      'Giảm 500K khi giới thiệu bạn bè',      0, 500000, 4000000,    NULL, 100, 0, '2026-12-31 23:59:59')
ON CONFLICT (code) DO NOTHING;

-- ---- SEED: finance.invoices (student_id = admission.users.id theo R-ID-1) ----
INSERT INTO finance.invoices
    (invoice_code, student_id, course_id, enrollment_id, invoice_type, amount, voucher_id, discount_applied, final_amount, paid_amount, status, due_date, paid_at)
SELECT v.invoice_code, u.id, c.id, e.id, 'COURSE_PACKAGE', v.amount,
       (SELECT id FROM finance.vouchers WHERE code = v.voucher_code),
       v.discount, v.amount - v.discount, v.paid, v.status, v.due_date::date,
       CASE WHEN v.status = 'PAID' THEN CURRENT_TIMESTAMP - INTERVAL '2 days' END
FROM (VALUES
    ('INV-2026-001', 'student@english.edu.vn', 'ENG-IELTS-FOUND', 'CLS-IELTS-01', 6500000::numeric,       NULL::text,        0::numeric, 6500000::numeric, 'PAID',    '2026-11-05'),
    ('INV-2026-002', 'mai.le@gmail.com',       'ENG-IELTS-FOUND', 'CLS-IELTS-01', 6500000::numeric,       NULL::text,        0::numeric, 6500000::numeric, 'PAID',    '2026-11-05'),
    ('INV-2026-003', 'bao.pham@gmail.com',     'ENG-IELTS-INT',   'CLS-IELTS-02', 8500000::numeric, 'KHAI_GIANG_2026',  850000::numeric,       0::numeric, 'PENDING', '2026-11-10')
) AS v(invoice_code, email, course_code, class_code, amount, voucher_code, discount, paid, status, due_date)
JOIN admission.users u ON u.email = v.email
JOIN admission.courses c ON c.code = v.course_code
JOIN admission.classes cl ON cl.code = v.class_code
JOIN admission.enrollments e ON e.student_id = u.id AND e.class_id = cl.id
ON CONFLICT (invoice_code) DO NOTHING;

INSERT INTO finance.invoice_items (invoice_id, course_id, item_type, description, quantity, unit_price, line_total)
SELECT i.id, i.course_id, 'TUITION_PACKAGE', 'Học phí khóa học (gói trọn khóa)', 1, i.amount, i.amount
FROM finance.invoices i
WHERE NOT EXISTS (SELECT 1 FROM finance.invoice_items x WHERE x.invoice_id = i.id);

-- Lịch đóng: hóa đơn đã trả = 1 kỳ PAID; hóa đơn INV-2026-003 chia 2 đợt (tổng = final_amount, INV-FIN-02)
INSERT INTO finance.installments (invoice_id, installment_no, payment_ref_code, amount_due, amount_paid, due_date, status, paid_at)
SELECT i.id, 1, i.invoice_code || '-K1', i.final_amount, i.final_amount, i.due_date, 'PAID', i.paid_at
FROM finance.invoices i WHERE i.status = 'PAID'
ON CONFLICT (invoice_id, installment_no) DO NOTHING;

INSERT INTO finance.installments (invoice_id, installment_no, payment_ref_code, amount_due, amount_paid, due_date, status)
SELECT i.id, k.no, i.invoice_code || '-K' || k.no, i.final_amount / 2, 0, i.due_date + (k.no - 1) * 30, 'PENDING'
FROM finance.invoices i CROSS JOIN (VALUES (1), (2)) AS k(no)
WHERE i.invoice_code = 'INV-2026-003'
ON CONFLICT (invoice_id, installment_no) DO NOTHING;

-- ---- SEED: finance.payments ----
INSERT INTO finance.payments (payment_code, invoice_id, installment_id, payment_method, amount, bank_transaction_id, qr_reference_code, received_by_staff_id, status)
SELECT 'PAY-2026-0001', i.id, ins.id, 'VIETQR', i.final_amount, 'FT261015998811', i.invoice_code, NULL, 'SUCCESS'
FROM finance.invoices i JOIN finance.installments ins ON ins.invoice_id = i.id AND ins.installment_no = 1
WHERE i.invoice_code = 'INV-2026-001'
ON CONFLICT (payment_code) DO NOTHING;

INSERT INTO finance.payments (payment_code, invoice_id, installment_id, payment_method, amount, received_by_staff_id, status)
SELECT 'PAY-2026-0002', i.id, ins.id, 'CASH', i.final_amount, (SELECT id FROM admission.users WHERE email = 'staff@english.edu.vn'), 'SUCCESS'
FROM finance.invoices i JOIN finance.installments ins ON ins.invoice_id = i.id AND ins.installment_no = 1
WHERE i.invoice_code = 'INV-2026-002'
ON CONFLICT (payment_code) DO NOTHING;

-- ---- SEED: finance.student_wallets (mỗi học viên 1 ví, số dư 0; student_id = users.id) ----
INSERT INTO finance.student_wallets (student_id, balance)
SELECT u.id, 0 FROM admission.users u WHERE u.role = 'STUDENT'
ON CONFLICT (student_id) DO NOTHING;


-- ============================================================================
-- BƯỚC 4: GRANT PERMISSIONS (khuyến nghị cho môi trường production)
-- ============================================================================
-- Mỗi service dùng 1 DB user riêng, chỉ có quyền trên schema của mình:
-- CREATE USER admission_svc WITH PASSWORD '...';  CREATE USER student_svc  WITH PASSWORD '...';
-- CREATE USER learning_svc  WITH PASSWORD '...';  CREATE USER finance_svc  WITH PASSWORD '...';
-- GRANT USAGE ON SCHEMA admission TO admission_svc;
-- GRANT ALL ON ALL TABLES IN SCHEMA admission TO admission_svc;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA admission TO admission_svc;
-- (tương tự cho student/learning/finance). Tuyệt đối KHÔNG GRANT chéo schema.

-- ============================================================================
-- END OF init-db.sql
-- ============================================================================
