"""
tests/invariants/test_student_invariants.py
Kiểm thử các quy tắc bất biến phân hệ Học viên (INV-STU-01, INV-STU-02).
"""

import unittest
import sqlite3

class StudentInvariantsTest(unittest.TestCase):

    def setUp(self):
        self.conn = sqlite3.connect(":memory:")
        self.cur = self.conn.cursor()
        
        self.cur.execute("""
            CREATE TABLE students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                status TEXT CHECK (status IN ('STUDYING', 'RESERVED', 'DROPPED', 'GRADUATED'))
            )
        """)
        self.cur.execute("""
            CREATE TABLE attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                session_date TEXT NOT NULL,
                status TEXT CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
                UNIQUE(class_id, student_id, session_date)
            )
        """)

    def tearDown(self):
        self.conn.close()

    def test_inv_stu_01_single_attendance_per_session(self):
        """INV-STU-01: Mỗi học viên chỉ có tối đa 1 điểm danh trong 1 buổi học."""
        self.cur.execute("""
            INSERT INTO attendance (class_id, student_id, session_date, status)
            VALUES (1, 100, '2026-11-01', 'PRESENT')
        """)
        self.conn.commit()

        # Thử điểm danh lần 2 cho cùng học viên trong cùng ngày của lớp
        with self.assertRaises(sqlite3.IntegrityError):
            self.cur.execute("""
                INSERT INTO attendance (class_id, student_id, session_date, status)
                VALUES (1, 100, '2026-11-01', 'LATE')
            """)

    def test_inv_stu_02_valid_student_status(self):
        """INV-STU-02: Trạng thái học vụ của học viên phải nằm trong danh mục cho phép."""
        # Trạng thái hợp lệ
        self.cur.execute("INSERT INTO students (full_name, email, status) VALUES ('Nam Tran', 'nam@test.com', 'STUDYING')")
        self.conn.commit()

        # Trạng thái không hợp lệ
        with self.assertRaises(sqlite3.IntegrityError):
            self.cur.execute("INSERT INTO students (full_name, email, status) VALUES ('Invalid Student', 'inv@test.com', 'EXPELLED_UNKNOWN')")

if __name__ == "__main__":
    unittest.main()
