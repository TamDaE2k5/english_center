# Nghiệm thu chuẩn bị Admission V1 — 09/11/2026

- Nhánh `feature/admission`, baseline `84af6e9`; giữ nguyên thay đổi chưa commit và 8 test hồi quy cũ (đã so hash snapshot).
- Sửa ADM-QA-06: khóa lớp và refresh trạng thái khi tranh chấp; đăng ký/gia hạn không chiếm quá slot.
- Sửa ADM-QA-08: deadline server chặn duyệt quá hạn; webhook muộn ghi EXPIRED + event đối soát.
- Sửa ADM-QA-07: lớp service xác thực API công khai, principal chính chủ hoặc token đúng lượt, không gắn khách vào email/tài khoản có sẵn; cập nhật DTO và frontend Admission.
- Sửa thêm ADM-QA-09 được DB thật phát hiện: Hibernate bind JSONB cho `TestSubmission.answersDetail`.
- Bổ sung test `EnrollmentV1Test`, `PlacementPublicApiTest`, `PublicPlacementServiceTest`, `AdmissionPostgresTest`. Fixture của `EnrollmentServiceTest` đổi sang query khóa mới, giữ assertion cũ.
- JDK 21: **110 tests pass, 0 skip**; PostgreSQL 16.15 thật có 3 test tích hợp; invariants **15 pass**; JavaScript syntax và diff whitespace pass.
- Kết luận **NOT READY FOR V1** do còn thiếu E2E với Gateway/Finance/Student thật, xác minh email và luồng khôi phục tài khoản khách. Không coi các cổng liên service mock là E2E.
- Chỉ sửa Admission và tài liệu/test liên quan. Không sửa main, không commit/push/merge.

Chi tiết bằng chứng và điều kiện nghiệm thu: [admission-bug-report.md](../../admission-bug-report.md). Lệnh chạy lại: [testing-v1.md](../../../services/admission/docs/testing-v1.md).
