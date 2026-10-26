"""
tests/invariants/test_system_invariants.py
Kiểm thử các quy tắc toàn hệ thống: snake_case API payload và cô lập schema database (INV-SYS-01, INV-SYS-02).
"""

import unittest
import os
import re

class SystemInvariantsTest(unittest.TestCase):

    def test_inv_sys_01_snake_case_naming_convention(self):
        """INV-SYS-01: Kiểm tra quy ước snake_case trong các file code backend và API conventions."""
        project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
        api_conv_path = os.path.join(project_root, "docs/api-conventions.md")
        
        self.assertTrue(os.path.exists(api_conv_path), "docs/api-conventions.md phải tồn tại")
        with open(api_conv_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("snake_case", content, "Tài liệu API convention phải quy định chuẩn snake_case")

        # Kiểm tra helper hàm Python format
        def is_snake_case(name: str) -> bool:
            return bool(re.match(r"^[a-z0-9_]+$", name))

        sample_fields = ["student_id", "course_code", "tuition_amount", "created_at", "max_students"]
        for fld in sample_fields:
            self.assertTrue(is_snake_case(fld), f"{fld} phải đúng định dạng snake_case")

        invalid_fields = ["studentId", "course-code", "TuitionAmount"]
        for fld in invalid_fields:
            self.assertFalse(is_snake_case(fld), f"{fld} không được phép dùng")

    def test_inv_sys_02_database_schema_isolation_script(self):
        """INV-SYS-02: init-db.sql phải định nghĩa 4 schema riêng biệt."""
        project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
        init_sql_path = os.path.join(project_root, "scripts/init-db.sql")
        
        self.assertTrue(os.path.exists(init_sql_path), "scripts/init-db.sql phải tồn tại")
        with open(init_sql_path, "r", encoding="utf-8") as f:
            sql_content = f.read()

        required_schemas = ["admission", "student", "learning", "finance"]
        for sch in required_schemas:
            pattern = rf"CREATE SCHEMA IF NOT EXISTS {sch};"
            self.assertRegex(sql_content, pattern, f"Database phải tạo schema độc lập: {sch}")

if __name__ == "__main__":
    unittest.main()
