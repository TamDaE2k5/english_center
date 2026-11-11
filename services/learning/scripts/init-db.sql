CREATE SCHEMA IF NOT EXISTS learning;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS learning.materials (
    id SERIAL PRIMARY KEY,
    class_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    material_type VARCHAR(50) NOT NULL DEFAULT 'DOCUMENT'
        CHECK (material_type IN ('DOCUMENT', 'VIDEO', 'EXERCISE', 'SLIDE')),
    file_url TEXT NOT NULL
        CHECK (btrim(file_url) <> ''),
    original_filename VARCHAR(255),
    file_size BIGINT CHECK (file_size >= 0),
    uploaded_by INTEGER,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_materials_class ON learning.materials USING btree (class_id);
CREATE INDEX IF NOT EXISTS idx_materials_type ON learning.materials USING btree (class_id, material_type);

DROP TRIGGER IF EXISTS trg_materials_updated_at ON learning.materials;
CREATE TRIGGER trg_materials_updated_at
BEFORE UPDATE ON learning.materials
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS learning.grades (
    id SERIAL PRIMARY KEY,
    class_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    assessment_name VARCHAR(255) NOT NULL,
    score NUMERIC(5,2) NOT NULL CHECK (score >= 0),
    max_score NUMERIC(5,2) NOT NULL DEFAULT 100.0 CHECK (max_score > 0),
    weight NUMERIC(3,2) DEFAULT 1.0 CHECK (weight IS NULL OR (weight > 0 AND weight <= 1)),
    feedback TEXT,
    graded_by INTEGER,
    graded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_score_lte_max CHECK (score <= max_score),
    CONSTRAINT grades_class_id_student_id_assessment_name_key UNIQUE (class_id, student_id, assessment_name)
);

CREATE INDEX IF NOT EXISTS idx_grades_class_assessment ON learning.grades USING btree (class_id, assessment_name);
CREATE INDEX IF NOT EXISTS idx_grades_class_student ON learning.grades USING btree (class_id, student_id);
CREATE INDEX IF NOT EXISTS idx_grades_student ON learning.grades USING btree (student_id);

DROP TRIGGER IF EXISTS trg_grades_updated_at ON learning.grades;
CREATE TRIGGER trg_grades_updated_at
BEFORE UPDATE ON learning.grades
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE VIEW learning.v_student_grade_summary AS
SELECT class_id,
    student_id,
    count(id) AS total_assessments,
    round(sum(score * COALESCE(weight, 1.0)) / NULLIF(sum(COALESCE(weight, 1.0)), 0::numeric), 2) AS weighted_avg_score,
    round(sum(score / max_score * 100.0 * COALESCE(weight, 1.0)) / NULLIF(sum(COALESCE(weight, 1.0)), 0::numeric), 2) AS average_percentage,
    CASE
        WHEN (sum(score / max_score * 100.0 * COALESCE(weight, 1.0)) / NULLIF(sum(COALESCE(weight, 1.0)), 0::numeric)) >= 50::numeric THEN 'Pass'::text
        ELSE 'Fail'::text
    END AS result
FROM learning.grades
GROUP BY class_id, student_id;

CREATE OR REPLACE VIEW learning.v_class_pass_rate AS
SELECT class_id,
    count(DISTINCT student_id) AS total_students,
    count(DISTINCT student_id) FILTER (WHERE result = 'Pass'::text) AS pass_count,
    count(DISTINCT student_id) FILTER (WHERE result = 'Fail'::text) AS fail_count,
    round(count(DISTINCT student_id) FILTER (WHERE result = 'Pass'::text)::numeric / NULLIF(count(DISTINCT student_id), 0)::numeric * 100::numeric, 2) AS pass_rate_percentage
FROM learning.v_student_grade_summary
GROUP BY class_id;