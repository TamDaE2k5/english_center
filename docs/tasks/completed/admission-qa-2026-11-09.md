# Nghiệm thu QA Admission — 09/11/2026

Baseline `feature/admission` tại `84af6e9`, fetch origin và đã đồng bộ. Không commit/push/merge, không thay đổi main hoặc công việc ở dự án cha.

Chi tiết tái hiện, nguyên nhân, bản sửa và tồn đọng: [admission-bug-report.md](../../admission-bug-report.md).

## File thay đổi

- `services/admission/backend/src/main/java/com/englishcenter/admission/config/SecurityConfig.java`: nối filter xác thực nội bộ vào security chain.
- `services/admission/backend/src/main/java/com/englishcenter/admission/security/InternalSecretFilter.java`: chặn webhook/internal thiếu hoặc sai secret.
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/EnrollmentServiceImpl.java`: từ chối duyệt trạng thái kết thúc EXPIRED.
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/PlacementTestServiceImpl.java`: bắt buộc attempt, từ chối câu trả lời trùng.
- `services/admission/backend/src/main/java/com/englishcenter/admission/service/impl/AuthServiceImpl.java`: không ghi reset token vào log.
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionRegressionTest.java`: 4 test hồi quy service.
- `services/admission/backend/src/test/java/com/englishcenter/admission/security/WebhookSecurityTest.java`: 3 test HTTP qua Spring Security thật.
- `services/admission/backend/src/test/java/com/englishcenter/admission/service/AuthServiceTest.java`: thêm 1 test bảo vệ log; không đổi assertion cũ.
- Hai tài liệu báo cáo QA và nghiệm thu này.

## Kiểm thử

- Java: 92/92 pass, không skip; JDK 25 compiler release 21, Maven 3.9.9, Byte Buddy experimental flag. Cần xác nhận lại bằng JDK 21 trên CI.
- Python: 15/15 invariant pass.
- `docker compose config --quiet` và `git diff --check`: pass.
- Docker runtime và Spring Boot chạy DB thật: bị chặn bởi Linux engine không kết nối được và PostgreSQL localhost từ chối kết nối. Chưa nghiệm thu E2E hoặc concurrency PostgreSQL.
