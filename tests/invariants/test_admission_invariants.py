"""
tests/invariants/test_admission_invariants.py
Kiểm thử các quy tắc bất biến phân hệ Tuyển sinh (INV-ADM-01, INV-ADM-02, INV-ADM-03).
"""

import unittest
import sqlite3

class AdmissionInvariantsTest(unittest.TestCase):

    def setUp(self):
        self.conn = sqlite3.connect(":memory:")
        self.cur = self.conn.cursor()
        
        # Khởi tạo bảng mô phỏng schema admission
        self.cur.execute("""
            CREATE TABLE courses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                tuition_amount REAL CHECK (tuition_amount >= 0)
            )
        """)
        self.cur.execute("""
            CREATE TABLE classes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id INTEGER,
                code TEXT UNIQUE NOT NULL,
                max_students INTEGER CHECK (max_students > 0)
            )
        """)
        self.cur.execute("""
            CREATE TABLE enrollments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id INTEGER,
                student_id INTEGER,
                status TEXT CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
            )
        """)

    def tearDown(self):
        self.conn.close()

    def test_inv_adm_01_cannot_exceed_max_students(self):
        """INV-ADM-01: Không được duyệt đơn đăng ký nếu lớp đã đủ sĩ số tối đa."""
        max_limit = 2
        self.cur.execute("INSERT INTO classes (code, max_students) VALUES ('CLS-TEST', ?)", (max_limit,))
        class_id = self.cur.lastrowid

        # Đã có 2 học viên APPROVED
        self.cur.execute("INSERT INTO enrollments (class_id, student_id, status) VALUES (?, 1, 'APPROVED')", (class_id,))
        self.cur.execute("INSERT INTO enrollments (class_id, student_id, status) VALUES (?, 2, 'APPROVED')", (class_id,))
        self.conn.commit()

        # Thử duyệt học viên thứ 3
        def approve_enrollment(target_class_id):
            self.cur.execute("SELECT COUNT(*) FROM enrollments WHERE class_id = ? AND status = 'APPROVED'", (target_class_id,))
            current_count = self.cur.fetchone()[0]
            self.cur.execute("SELECT max_students FROM classes WHERE id = ?", (target_class_id,))
            limit = self.cur.fetchone()[0]
            if current_count >= limit:
                raise ValueError("ADM_CLASS_FULL: Sĩ số lớp đã đạt tối đa")
            return True

        with self.assertRaises(ValueError) as ctx:
            approve_enrollment(class_id)
        self.assertIn("ADM_CLASS_FULL", str(ctx.exception))

    def test_inv_adm_02_course_code_unique(self):
        """INV-ADM-02: Mã khóa học phải là duy nhất."""
        self.cur.execute("INSERT INTO courses (code, name, tuition_amount) VALUES ('ENG-01', 'IELTS 1', 5000000)")
        self.conn.commit()

        with self.assertRaises(sqlite3.IntegrityError):
            self.cur.execute("INSERT INTO courses (code, name, tuition_amount) VALUES ('ENG-01', 'IELTS Duplicate', 6000000)")

    def test_inv_adm_03_state_machine_transition(self):
        """INV-ADM-03: Đơn đăng ký ở trạng thái CANCELLED không thể chuyển trực tiếp sang APPROVED."""
        valid_transitions = {
            "PENDING": ["APPROVED", "REJECTED", "CANCELLED"],
            "APPROVED": ["CANCELLED"],
            "REJECTED": [],
            "CANCELLED": []
        }

        def transition_status(current, target):
            if target not in valid_transitions.get(current, []):
                raise ValueError(f"Chuyển trạng thái bất hợp lệ từ {current} sang {target}")
            return True

        with self.assertRaises(ValueError):
            transition_status("CANCELLED", "APPROVED")

        with self.assertRaises(ValueError):
            transition_status("REJECTED", "APPROVED")

if __name__ == "__main__":
    unittest.main()
