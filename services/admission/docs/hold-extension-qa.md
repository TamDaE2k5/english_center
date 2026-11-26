# H06 — Gia hạn giữ chỗ còn hiệu lực — 11/11/2026

Trạng thái: **PARTIAL về chính sách tổng thể**. Bản sửa thực thi FR-ENR-05 hiện có: chỉ gia hạn PENDING còn hiệu lực, không lợi dụng job chạy trễ để khôi phục đơn. Không tự quy định số lần tối đa hoặc cơ chế override sau hết hạn.

## Căn cứ và những điểm chưa thống nhất

| Nguồn | Quy tắc được tìm thấy | Cách xử lý lượt này |
|---|---|---|
| BA/BI FR-ENR-05, dòng mô tả FR | STAFF/ADMIN, PENDING **còn hiệu lực**, lưu người/lý do/hạn mới; không EXPIRED/APPROVED | Chặn deadline <= now, NULL hoặc trạng thái khác PENDING ở service, sau khi có khóa |
| TASK-ADM-44 | Có reason; notes ghi người/thời điểm; extended_hours 1–72; công thức cũ max(deadline,now)+hours | Giữ validation 1–72 đã có. Với đơn còn hạn, công thức là deadline+hours. Không dùng nhánh NOW() để phục hồi đơn quá hạn |
| BA/BI ngoại lệ vận hành | Nêu ví dụ 24h/48h | Không tự đổi sang chỉ cho 24/48 hoặc thay thời lượng; task/API hiện cho 1–72, UI 24/48/72. Nhóm phải thống nhất phạm vi cho bản chốt |
| API catalog #47 / code | POST extend-hold, extended_hours; STAFF/ADMIN | Giữ endpoint/method. BA có ví dụ PUT/extra_hours khác catalog; không tự tạo alias mới trong nhóm lỗi này |
| Schema | Deadline nullable; hold_extended_by_user_id/reason là metadata lần cuối; notes | NULL không chứng minh còn hạn nên từ chối gia hạn; không đổi cách count NULL legacy ở các nghiệp vụ khác. Thêm audit append-only cho các lần gia hạn/từ chối |
| Số lần gia hạn và khôi phục sau hạn | Không có giới hạn lần rõ ràng; FR và công thức task cũ mâu thuẫn ở trường hợp quá hạn | Không thêm cap. Yêu cầu mới với snapshot mới vẫn được kiểm tra như bình thường; override khôi phục đơn **chưa có hợp đồng/chưa triển khai** |

Audit H06 gốc: `C:/Users/Admin/admission-work/qa-admission-final-audit-20261110/admission-v1-final-audit.md`. Baseline lượt này: HEAD `cbf5be7c2e956397cffe4e962a12c2acce197953` + **17 file chưa commit của H01**. Snapshot/hash trước sửa ở `C:/Users/Admin/admission-work/qa-admission-hold-20261111/before/` và `before-manifest.json`; không reset/stash/clean/discard.

## Lỗi và sửa nguyên nhân

| Phát hiện | Mức độ/vị trí | Tái hiện trước sửa | Thay đổi |
|---|---|---|---|
| ADM-QA-29 / H06 | Medium, EnrollmentServiceImpl.extendHold | PENDING deadline đã qua nhưng job chưa chạy: 200, hạn được đặt lại từ now; NULL cũng được tự tạo hạn mới | Kiểm tra deadline **sau class lock và refresh/row lock**; deadline <= now/NULL trả ADM_HOLD_EXPIRED, không đổi hạn/status. Job áp dụng <= để thống nhất mốc hết hạn |
| ADM-QA-30 | Medium, service/API/frontend | Gửi lại cùng yêu cầu cộng thêm giờ lần nữa; hai nhân viên cùng snapshot có thể cộng hai lần ngoài ý muốn | Compare-and-set trên deadline gốc dưới row lock, API yêu cầu expected_hold_expires_at. Snapshot cũ trả409 ADM_HOLD_CONFLICT; không cộng thêm giờ |
| ADM-QA-31 | Medium, service | Lời gọi service thiếu actor vẫn cập nhật với người gia hạn NULL; PENDING legacy ở lớp CANCELLED vẫn được kéo dài | Guard STAFF/ADMIN và actor tại service ngoài guard HTTP; lớp CANCELLED bị chặn. Audit nghiệp vụ từ chối commit sau rollback |
| ADM-QA-32 | Medium, enrollments.html | Nút gia hạn vẫn hiện ở PENDING đã hết thời gian/NULL deadline; form có thể báo thành công khi API trả null | Ẩn/guard nút theo quyền, trạng thái và deadline đã đọc; gửi snapshot từ modal, giữ nguyên qua retry; chặn submit đang chờ; null/false không báo thành công |

**Bằng chứng đỏ:** 5/5 PostgreSQL tests mới fail trên logic trước sửa (4 trường hợp HTTP200 thay vì400/409 và thiếu BusinessException khi không có actor). Node: 2 tests mới fail, 10 tests cũ vẫn pass. Log `red.log`, `frontend-red.log`. Các nhánh concurrency bổ sung không được tuyên bố đã chạy đỏ trên baseline.

## Hợp đồng cập nhật kỹ thuật

`POST /api/admission/enrollments/{id}/extend-hold`, JWT STAFF/ADMIN:

```json
{
  "extended_hours": 24,
  "reason": "Chờ chuyển khoản",
  "expected_hold_expires_at": "2026-11-12T10:30:00.123456"
}
```

Client lấy **đúng** hold_expires_at từ GET enrollment/danh sách. Trường mới là precondition kỹ thuật, không cho client đặt hạn mới hoặc bỏ qua thời gian server. Backend chỉ cộng extended_hours vào deadline thật đã khóa.

- Body cũ thiếu precondition:400, yêu cầu client tải lại và cập nhật payload; không silently áp dụng chế độ cộng lặp cũ. Đây là thay đổi bắt buộc khi nâng phiên bản API/client Admission cùng bản sửa.
- Cùng snapshot gửi lại sau double click/timeout: lần đã thay đổi hạn khiến lần sau409. Đây là idempotence về **hiệu ứng cập nhật**, không phải replay200/ACK rằng thao tác trước chắc chắn thành công. Người dùng tải lại để xem trạng thái/hạn hiện tại trước khi chủ ý gửi mới.
- Hai nhân viên dùng cùng snapshot: một lần thành công, một409. Snapshot mới cho phép yêu cầu chủ ý tiếp theo nếu trạng thái/deadline/sĩ số/quyền vẫn hợp lệ; chưa có quy tắc số lần tối đa để áp đặt.
- Modal giữ deadline tại lúc mở, không tự thay bằng deadline mới trong quá trình gửi lại. Nếu đơn đã đổi trạng thái/qua hạn, backend từ chối dù giao diện đang cũ.

## Quy tắc state và audit

| Tình trạng tại thời điểm kiểm tra dưới khóa | Kết quả |
|---|---|
| PENDING, deadline > now, snapshot khớp, không vượt occupied max | Cộng giờ vào deadline, giữ slot đã có; actor/reason/notes + audit cùng commit |
| PENDING, deadline <= now, job chưa chạy |400 ADM_HOLD_EXPIRED; giữ deadline/status, không hồi sinh; job xử lý EXPIRED riêng |
| PENDING, deadline NULL |400 ADM_HOLD_EXPIRED; cần đối soát/chốt policy dữ liệu legacy |
| EXPIRED/CANCELLED/APPROVED/REJECTED/CANCELLED_REFUND_PENDING | Từ chối, không đổi deadline/state |
| Lớp CANCELLED còn PENDING legacy | Từ chối ADM_CLASS_NOT_OPEN |
| Deadline DB đã khác snapshot |409 ADM_HOLD_CONFLICT; không tăng lần nữa |
| APPROVED + active held == max | Có thể gia hạn **slot của chính đơn** còn hạn, không thêm slot |
| Occupied đã > max do dữ liệu ngoài protocol | Từ chối ADM_CLASS_FULL, không tự sửa dữ liệu lịch sử |

TransactionTemplate REQUIRES_NEW bao toàn bộ class lock -> row refresh/lock -> validation -> cập nhật -> audit thành công. `countOccupiedSlots` dùng cùng mốc now với kiểm tra hạn. BusinessException được audit ở transaction độc lập **sau** rollback/nhả connection. Không nuốt lỗi audit hoặc trả thành công nếu ghi DB lỗi.

Giữ thứ tự class->enrollment như callback H01/duyệt/hủy/cascade. Expiration bulk UPDATE lấy row lock PostgreSQL; ở READ COMMITTED, sau khi chờ writer gia hạn commit, PostgreSQL recheck predicate deadline mới. Hai hướng đã có test dùng barrier và row lock thật. Gia hạn được chấp nhận tại điểm kiểm tra deadline dưới khóa; nếu deadline cũ qua trong lúc transaction đang hoàn tất, job không được ghi EXPIRED đè deadline mới đã commit. Không khẳng định mọi mức isolation, direct SQL bỏ protocol hoặc throughput nhiều instance đã được benchmark.

Audit mới `admission.hold_extension_audit`: actor, enrollment, outcome EXTENDED/REJECTED, mã lý do, giờ yêu cầu, timestamp; thành công có hạn cũ/mới. Reason nghiệp vụ và tên/ID/thời điểm nối vào notes, metadata lần cuối được giữ theo schema. Không log JWT/secret/full body hoặc reason nhập từ người dùng. Không FK audit để ghi cả actor/enrollment không tồn tại và giữ lịch sử. HTTP security/Jackson/Bean Validation từ chối trước service không được tuyên bố có audit nghiệp vụ mới.

## Migration và kiểm thử

Áp dụng `backend/src/main/resources/db/admission-hold-extension-audit.sql` sau các migration Admission hiện có, gồm H01. Chỉ CREATE TABLE IF NOT EXISTS trong schema Admission; không sửa/xóa dữ liệu cũ. `ddl-auto=none`: phải áp dụng trước chạy bản code mới.

Suite cuối với assertion quyền service siết chặt đạt **200/200 Java tests với JDK21.0.13, gồm87 PostgreSQL16.15 tests, không failure/error/skip** (`java-final-guard.log`, BUILD SUCCESS11:26:54). Lần `java-final.log` trước đó cũng200/200 pass. Pass **15/15 invariants,12/12 Node** và Chrome7 trang/BI role/CASH/hold precondition với API fixtures. Migration chạy lại COMMIT, không xóa dữ liệu; `git diff --check` pass. Log trong `C:/Users/Admin/admission-work/qa-admission-hold-20261111/`.

**15 PG tests mới** gồm 5 ca đỏ, role/validation/terminal, gia hạn chủ ý lần sau/audit, capacity tính own slot, rollback audit, concurrent snapshot, hai hướng expiry-vs-extension với barrier, equality boundary, race đăng ký slot cuối và races approval/cancel/cascade/payment. Các test H01, H02/H03, giữ chỗ/cash/retry/BI cũ chạy lại trong cùng suite. Unit test cũ kỳ vọng ADM_CLASS_FULL cho đơn quá hạn đổi thành ADM_HOLD_EXPIRED theo FR-ENR-05; giữ assertion không save và thêm assertion không xin slot. Đây là đổi kỳ vọng theo policy guard, không che lỗi bằng nới assertion.

Lịch sử kiểm tra: `java-first.log` lỗi biên dịch do overload generic assertEquals trong test mới, đã sửa kiểu Integer tường minh. `java-verified.log` 199 tests có2 failure ở fixture/expectation: unit thiếu class mock sau khi service luôn lấy khóa trước state guard; test race mới kỳ vọng giữ deadline cũ dù cascade hiện có cố ý đặt deadline về thời điểm hủy. Đã bổ sung fixture và kiểm tra đúng deadline hủy trong khoảng transaction, giữ assertion status/slot/audit; không sửa code cascade hoặc nới guard nghiệp vụ để làm xanh. `java-final.log` sau đó200/200 pass. Không dùng các lần fail làm bằng chứng thành công.

Scheduler cron tắt trong integration fixture để kiểm soát thời gian; test gọi service expiry thật/SQL và transaction PostgreSQL thật, chưa kiểm chứng cron dispatcher 5 phút ngoài vận hành. Finance/Student clients mock; SMTP loopback thật; Chrome dùng API fixtures; không E2E toàn hệ thống.

## Cần người phụ trách và Tâm chốt

1. Có cho khôi phục PENDING đã quá hạn hoặc đơn EXPIRED qua một luồng override riêng không? Nếu có, cần quy tắc xin lại slot, chu kỳ invoice/cash/receipt theo H01, quyền và audit; hiện bị chặn theo FR.
2. Thống nhất 24/48 giờ ở phần BA với 1–72 giờ ở task/API/UI; số lần và tổng thời gian tối đa nếu muốn giới hạn. Không được coi việc hiện cho yêu cầu chủ ý với snapshot mới là chính sách gia hạn vô hạn đã được duyệt.
3. Deadline NULL legacy và thời gian/múi giờ liên service; xác nhận vận hành cron/triển khai migration/client payload. Không tự tạo deadline thay dữ liệu thiếu.

Không sửa Finance/Student/Learning, không chạy E2E, không commit/push/merge. Không thay kết luận tổng nghiệm thu V1.
