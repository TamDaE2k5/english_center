# Tiếp tục QA Admission sau gián đoạn — 09/11/2026

- `feature/admission`, HEAD `84af6e9`. Kiểm tra/lưu snapshot 64 file thay đổi hiện có trước khi tiếp tục; không reset/xóa/commit/push/merge.
- Hoàn tất xác nhận audit LOGIN_FAILED commit và migration CHECK audit của Admission, gồm CREATE_USER và các action email/bảo mật; migration chạy lặp hai lần trên DB QA.
- Sửa fixture `AdmissionPostgresTest`: IP riêng cho từng test, không tắt brute-force hoặc xóa audit. Thêm `repeatedFailedLoginsStillTriggerBruteForceProtection` xác nhận hành vi bảo vệ thật sau 5 lần sai.
- Java JDK 21: **125/125 pass**, gồm **14 PostgreSQL integration tests**, không skip. Invariants **15/15**; Node frontend **5/5**; Chrome 7 trang + guest role boundary pass, có API fixtures.
- Đã kiểm tra các luồng SMTP/tài khoản, hash token, replay/hết hạn, đổi mật khẩu, khóa tài khoản và teacher lookup. SMTP loopback thật, không gửi email ra ngoài. Hai cổng liên service vẫn mock; không chạy E2E toàn hệ thống.
- Cập nhật báo cáo lỗi, hướng dẫn test, cấu hình SMTP/migration và đối chiếu 30 FR; tách chức năng đã kiểm chứng khỏi khoảng trống BI/hủy lớp/retry/cash approval.
- Thay đổi trong phiên tiếp tục: `services/admission/backend/src/test/java/com/englishcenter/admission/service/AdmissionPostgresTest.java` và các tài liệu Admission/QA. Code sửa từ phiên trước được giữ nguyên và kiểm chứng lại; không sửa service khác/shared/main.

Đánh giá: cốt lõi nằm trong regression đã đạt; riêng Admission còn hoàn thiện một phần theo BA/BI. Chi tiết: [báo cáo QA](../../admission-bug-report.md), [tiến độ Admission](../../../services/admission/docs/admission-quality-progress.md).
