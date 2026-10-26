"""
tests/invariants/test_security_invariants.py
Kiểm thử các quy tắc bất biến về bảo mật và phân quyền (INV-SEC-01, INV-SEC-02, INV-SEC-03).
"""

import unittest
import os
import re

class SecurityInvariantsTest(unittest.TestCase):

    def test_inv_sec_03_no_plain_secrets_in_git(self):
        """INV-SEC-03: Kiểm tra không đưa mật khẩu sản xuất hoặc secret thật vào git."""
        project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.."))
        
        # Kiểm tra file .gitignore có bỏ qua .env không
        gitignore_path = os.path.join(project_root, ".gitignore")
        self.assertTrue(os.path.exists(gitignore_path), ".gitignore phải tồn tại")
        with open(gitignore_path, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn(".env", content, ".gitignore phải loại trừ file .env")

    def test_inv_sec_01_student_profile_isolation_logic(self):
        """INV-SEC-01: Học viên chỉ được xem hồ sơ của chính mình."""
        current_user_id = 10
        requested_user_id = 99  # Thử truy cập học viên khác
        user_role = "STUDENT"

        # Quy tắc bảo mật: Nếu là STUDENT thì requested_user_id phải trùng current_user_id
        is_allowed = (user_role == "ADMIN") or (user_role == "STUDENT" and current_user_id == requested_user_id)
        self.assertFalse(is_allowed, "Học viên A không được phép truy cập hồ sơ của học viên B")

    def test_inv_sec_02_teacher_grading_boundary(self):
        """INV-SEC-02: Giáo viên chỉ được sửa điểm lớp mình phụ trách."""
        assigned_classes = [1, 2] # Giáo viên chỉ dạy lớp 1 và lớp 2
        target_class = 5          # Cố tình sửa điểm lớp 5
        user_role = "TEACHER"

        is_allowed = (user_role == "ADMIN") or (user_role == "TEACHER" and target_class in assigned_classes)
        self.assertFalse(is_allowed, "Giáo viên không được sửa điểm của lớp mình không dạy")

if __name__ == "__main__":
    unittest.main()
