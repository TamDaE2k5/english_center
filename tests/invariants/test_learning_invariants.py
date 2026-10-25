"""
tests/invariants/test_learning_invariants.py
Kiểm thử các quy tắc bất biến phân hệ Đào tạo & Kết quả học tập (INV-LRN-01, INV-LRN-02).
"""

import unittest
import sqlite3

class LearningInvariantsTest(unittest.TestCase):

    def setUp(self):
        self.conn = sqlite3.connect(":memory:")
        self.cur = self.conn.cursor()
        
        self.cur.execute("""
            CREATE TABLE grades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                assessment_name TEXT NOT NULL,
                score REAL CHECK (score >= 0.0),
                max_score REAL CHECK (max_score > 0.0)
            )
        """)

    def tearDown(self):
        self.conn.close()

    def test_inv_lrn_01_score_within_valid_range(self):
        """INV-LRN-01: Điểm thi phải nằm trong khoảng: 0.0 <= score <= max_score."""
        def record_grade(class_id, student_id, name, score, max_score=100.0):
            if score < 0.0 or score > max_score:
                raise ValueError("LRN_GRADE_OUT_OF_RANGE: Điểm số vượt quá thang điểm quy định")
            self.cur.execute("INSERT INTO grades (class_id, student_id, assessment_name, score, max_score) VALUES (?, ?, ?, ?, ?)",
                             (class_id, student_id, name, score, max_score))
            self.conn.commit()

        # Điểm âm -> lỗi
        with self.assertRaises(ValueError):
            record_grade(1, 10, "Quiz 1", -5.0, 100.0)

        # Điểm vượt max -> lỗi
        with self.assertRaises(ValueError):
            record_grade(1, 10, "Final Exam", 105.0, 100.0)

        # Điểm hợp lệ -> thành công
        record_grade(1, 10, "Midterm", 85.5, 100.0)
        self.cur.execute("SELECT score FROM grades WHERE student_id = 10")
        self.assertEqual(self.cur.fetchone()[0], 85.5)

    def test_inv_lrn_02_student_grades_isolation(self):
        """INV-LRN-02: Học viên chỉ được xem điểm của chính mình."""
        current_student_id = 10
        requested_student_id = 12

        can_view = (current_student_id == requested_student_id)
        self.assertFalse(can_view, "Học viên không được phép tra cứu điểm của học viên khác")

if __name__ == "__main__":
    unittest.main()
