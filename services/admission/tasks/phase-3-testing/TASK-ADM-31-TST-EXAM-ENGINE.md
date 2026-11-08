# TASK-ADM-31: Động cơ Khảo thí Trực tuyến Countdown, Tự động Chấm & Bóc tách Kỹ năng

- **Mã nhiệm vụ**: `TASK-ADM-31`
- **Phân kỳ**: `Phase 3: Khảo thí Đầu vào Chuyên sâu (TST)`
- **Độ ưu tiên**: `P0 (Bắt buộc)`
- **Yêu cầu chức năng liên quan**: `FR-TST-03`, `FR-TST-04`, `BI-FUN-01`
- **Quy tắc bất biến bảo vệ**: `INV-SYS-01`

---

## 1. Mục tiêu Nghiệp vụ & Kỹ thuật
Xây dựng Động cơ thi trực tuyến hoàn chỉnh phục vụ đánh giá năng lực ứng viên:
1. **Bắt đầu Lượt thi (Server-side Countdown Tracking - INV-TST-02)**:
   - Học viên gọi `POST /api/admission/tests/public/{id}/start` kèm thông tin tài khoản `user_id` (hoặc email).
   - Hệ thống khởi tạo 1 bản ghi trong bảng `admission.test_attempts`:
     - `status = 'STARTED'`
     - `started_at = NOW()`
     - `expires_at = started_at + duration_minutes`
   - Trả về mã lượt thi `attempt_id` và danh sách câu hỏi trắc nghiệm (ẩn trường `correct_option` để chống lộ đáp án).
2. **Nộp bài & Chấm điểm Tự động (Auto-Grading & Single Submission - INV-TST-02)**:
   - Nhận `attempt_id` và danh sách câu trả lời: `[{"question_id": 1, "selected": "B"}, ...]`.
   - Kiểm tra lượt thi:
     - Phải đang ở trạng thái `STARTED`. Nếu đã `SUBMITTED` $\rightarrow$ Ném lỗi `ADM_TEST_ALREADY_SUBMITTED`.
     - Kiểm tra thời gian: Nếu `NOW() > expires_at` $\rightarrow$ Cập nhật `status = 'EXPIRED'`, ném lỗi `ADM_ATTEMPT_EXPIRED`.
   - Chấm điểm server-side theo trọng số `score_weight` cho từng kỹ năng (`LISTENING`, `READING`, `GRAMMAR`).
   - Lưu kết quả vào `admission.test_submissions` với `attempt_id` (ràng buộc `UNIQUE` chống chấm lại cùng 1 lượt).
   - Cập nhật `test_attempts.status = 'SUBMITTED'`, `submitted_at = NOW()`.

---

## 2. Phạm vi File Tác động

### File tạo mới hoặc chỉnh sửa:
- `services/admission/backend/src/main/java/com/englishcenter/admission/controller/PlacementTestController.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/PlacementTestService.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/PlacementTestServiceImpl.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/repository/TestSubmissionRepository.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/entity/TestSubmission.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/dto/test/TakeTestResponse.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/dto/test/SubmitTestRequest.java`
- `services/admission/backend/src/main/java/com/englishcenter/admission/dto/test/TestResultResponse.java`

---

## 3. Đặc tả API Hợp đồng

### 3.1. Lấy Đề thi Bắt đầu Làm bài
- **URL**: `GET /api/admission/tests/public/{setId}/start`
- **Response**: Trả về `test_set` và danh sách câu hỏi (không chứa trường `correct_option`).

### 3.2. Nộp Bài thi & Nhận Kết quả Tức thì
- **URL**: `POST /api/admission/tests/public/submit`
- **Request Body**:
  ```json
  {
    "test_set_id": 1,
    "email": "hocvien.moi@gmail.com",
    "full_name": "Nguyen Van B",
    "phone": "0912345678",
    "answers": [
      { "question_id": 101, "selected_option": "B" },
      { "question_id": 102, "selected_option": "A" }
    ]
  }
  ```
- **Response Thành công (HTTP 200 OK)**:
  ```json
  {
    "success": true,
    "data": {
      "submission_id": 501,
      "total_score": 75.0,
      "listening_score": 25.0,
      "reading_score": 30.0,
      "grammar_score": 20.0,
      "recommended_course": {
        "course_id": 2,
        "course_code": "IELTS-5.5",
        "title": "IELTS Pre-Intermediate Mục Tiêu 5.5"
      }
    },
    "message": "Nộp bài và chấm điểm thành công"
  }
  ```

---

## 4. Kế hoạch Triển khai (Step-by-step)
1. Tạo JPA Entity `TestSubmission` lưu bản ghi kết quả bài làm.
2. Trong `PlacementTestServiceImpl`:
   - Viết hàm `getTestForExam(Long testSetId)`: Lọc câu hỏi và chuyển sang `PublicQuestionDTO` (giấu đáp án đúng).
   - Viết hàm `submitAndGrade(SubmitTestRequest request)`:
     - Tự động sinh/lấy tài khoản học viên từ email (gọi `UserService.getOrCreateStudentUser`).
     - Tải danh sách câu hỏi và đáp án gốc từ DB.
     - Lặp qua từng câu trả lời của thí sinh, so sánh với `correct_option`.
     - Phân loại cộng điểm theo kỹ năng tương ứng.
     - Lưu bài làm vào `test_submissions`.
     - Gọi `RecommendationService` để lấy khóa học phù hợp (TASK-ADM-32).
3. Viết comment tiếng Việt đầy đủ.

---

## 5. Tiêu chí Nghiệm thu (Acceptance Criteria)
- [ ] API lấy đề thi tuyệt đối không để lộ đáp án đúng trong response JSON.
- [ ] Trắc nghiệm chấm điểm tự động chính xác từng kỹ năng, tổng điểm bằng tổng của 3 kỹ năng cộng lại.
- [ ] Thí sinh nộp bài thi thành công tự động được tạo tài khoản trong `admission.users` nếu là thí sinh mới.
