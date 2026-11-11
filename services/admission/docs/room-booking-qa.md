# Sức chứa phòng và concurrent booking — Admission, 10/11/2026

## Phạm vi và căn cứ

Xử lý H02/H03 của báo cáo `C:/Users/Admin/admission-work/qa-admission-final-audit-20261110/admission-v1-final-audit.md`. BA FR-FAC-03 yêu cầu class.max_students <= room.capacity; FR-ACA-03/mục3.3 định nghĩa xung đột bằng khoảng ngày giao nhau, day_pattern có ngày chung và start1 < end2, end1 > start2. Giữ nguyên công thức/mã lỗi đó; không thêm khoảng đệm giờ, giới hạn số lớp hoặc đổi quy tắc giáo viên.

Đầu lượt: feature/admission,103 file chưa commit (48 modified,55 untracked). Bản sao đầy đủ các file đầu lượt ở `C:/Users/Admin/admission-work/qa-admission-room-booking-20261110`, status-before.txt và manifest-before.json. Không reset/clean/discard, không xóa dữ liệu cũ, không commit/push/merge. Không sửa Finance/Student/Learning/shared hoặc các nhóm lỗi callback/session/BI khác.

## Lỗi được xác nhận trước sửa

- **H02/ADM-QA-23 — High:** RoomServiceImpl.updateRoom cho giảm phòng30 xuống10 dù có lớp max25. Test PostgreSQL bookingCannotShrinkRoomBelowAssignedClass mong BusinessException nhưng không có lỗi, cập nhật sai đã commit.
- **H03/ADM-QA-24 — High:** checkConflict -> save không có cơ chế serialize chung. Hai create cùng phòng, hai create khác phòng cùng teacher, hai update lớp tới cùng ca đều có thể vượt qua validation trước khi transaction kia commit. Test giữ transaction đầu sau validation, transaction thứ hai cũ đã hoàn tất trong lúc đầu chưa commit.
- **Race capacity cùng nhóm:** resize cạnh tranh create hoặc update max_students cũng không được phối hợp. Test trước sửa chứng minh writer thứ hai lọt qua trước commit; invariant có thể mất dù mỗi request kiểm tra hợp lệ ở thời điểm đọc.
- **Nhánh PATCH status phòng:** entity Room có thể ghi lại capacity cũ vì Hibernate update cả entity. Đã đưa writer này vào cùng cơ chế và bổ sung regression. Fixture ban đầu callRealMethod trên Spring Data findById abstract nên thất bại trước barrier; không lấy lỗi fixture đó làm bằng chứng runtime của code cũ. Fixture cuối đọc EntityManager thật trong transaction rồi giữ barrier; assertion capacity vẫn giữ nguyên.

Bằng chứng: red.log **5 tests/5 failures/0 errors** trên code cũ; red-extra.log gồm1 failure race update thực và1 failure fixture PATCH nói trên. Không sửa/nới assertion của các test cũ.

## Sửa nguyên nhân gốc

1. RoomRepository.lockScheduling dùng PostgreSQL **transaction advisory lock** `(8081,1)`, bắt buộc có transaction hiện hữu. SELECT1 FROM pg_advisory_xact_lock để chỉ trả scalar Integer; không tạo bảng/hàm/extension. Khóa tự nhả khi commit/rollback.
2. Gọi đầu createClass/updateClass/updateRoom/updateRoomStatus **trước mọi đọc phòng/lịch và trước khóa class row**. Do vậy validation và save dùng chung transaction được serialize qua database, không chỉ qua synchronized trong một JVM.
3. Sau khi giữ khóa, updateRoom lấy MAX(max_students) trong các lớp gán phòng. Giảm dưới mức đó bị ADM_ROOM_CAPACITY_EXCEEDED; bằng mức đó được chấp nhận. Không tự loại lớp CANCELLED/COMPLETED khỏi invariant capacity vì FR không định nghĩa ngoại lệ này; không tự sửa dữ liệu lịch sử.
4. PATCH status cũng lấy khóa trước khi đọc Room để không ghi lại capacity cũ sau resize/create. Lưu/check các field khác giữ nguyên.
5. Không đổi ConflictEngine hoặc thuật toán presets. Giờ kề nhau end1==start2, mẫu ngày không giao nhau, khoảng ngày rời nhau vẫn được nhận. Không thay luồng slot/enrollment/cancel/cash/retry/BI.

## Bảo đảm và giới hạn kỹ thuật

- Bảo đảm cho các writer API/service Admission hiện có trên PostgreSQL với transaction mặc định **READ COMMITTED** của môi trường dự án: query sau khi lấy khóa thấy commit mới của writer trước. Không dùng snapshot REPEATABLE READ đã mở sẵn bao ngoài các writer này; nếu đổi isolation/entry point phải kiểm chứng lại. BI read-only không lấy khóa và không gọi writer.
- Một khóa chung cho ghi lịch/sức chứa toàn Admission: đơn giản, tránh các chuỗi khóa room/teacher/class ngược nhau và làm được cross-instance cùng DB. Đánh đổi là các thao tác ghi lịch khác phòng cũng chờ nhau; không tuyên bố throughput cao hoặc stress test nhiều máy. Đã kiểm tra nhiều connection/transaction đồng thời trong test; chưa khởi động nhiều JVM.
- Không có migration mới. Không thay constraint/schema phân hệ khác. Không có trigger chống DML trực tiếp: SQL/manual writer bỏ qua cơ chế này hoặc phiên bản Admission cũ không lấy cùng khóa không nằm trong bảo đảm. Tất cả writer mới phải lấy cùng khóa; triển khai đồng bộ các instance Admission.
- Những thao tác status lớp không đổi schedule/capacity chỉ khóa class row như cũ; không mở lại CANCELLED/COMPLETED, chỉ giữ hoặc giải phóng lịch. Capacity guard kiểm tra tất cả lớp gán phòng nên không bị race do status kết thúc. Chưa sửa lớp/capacity/lịch cũ đã sai trước khi có guard; không tự xóa fixture hoặc dữ liệu người dùng.
- `CUSTOM` trong schema/enum cũ chưa có tập ngày cụ thể như4 preset BA. Không tự diễn giải CUSTOM thành các ngày mới trong lượt H02/H03. Các ca được chứng minh về giao ngày là preset có định nghĩa; lịch CUSTOM/chuỗi tùy ý cần nhóm chốt biểu diễn/validation trước khi tuyên bố bảo đảm đầy đủ cho lịch tùy chỉnh. Đây không phải lỗi do khóa concurrent vừa thêm, không âm thầm thay policy cũ.

## Test mới và kết quả

10 PostgreSQL integration tests bổ sung vào AdmissionPostgresTest:

| Test | Điều kiện được kiểm tra |
|---|---|
| bookingCannotShrinkRoomBelowAssignedClass | Phòng30/lớp25: giảm10 bị từ chối và rollback; giảm25 được nhận |
| bookingConcurrentRoomCreatesSerialize |2 create cùng phòng/ca: đúng1 OK,1 ADM_ROOM_CONFLICT, DB có1 lớp |
| bookingConcurrentTeacherCreatesSerialize |2 phòng khác nhau/cùng teacher/ca:1 OK,1 ADM_TEACHER_CONFLICT |
| bookingRoomResizeRacingCreatePreservesCapacity | Create max25 cạnh tranh resize10: resize chờ rồi bị từ chối |
| bookingConcurrentClassUpdatesSerialize |2 lớp riêng chuyển cùng phòng/ca:1 update OK,1 conflict |
| bookingRoomResizeRacingClassUpdatePreservesCapacity | Tăng max10->25 cạnh tranh resize30->10: không commit cặp giá trị sai |
| bookingRoomStatusCannotRestoreStaleCapacity | PATCH status đọc trước, resize/create sau: không ghi lại capacity10 dưới lớp25 |
| bookingAdjacentTimesRemainAllowed |2 request concurrent8–9 và9–10 vẫn đều OK |
| bookingDisjointDaysAndDateRangesRemainAllowed | MWF vs TThSat, khoảng ngày rời nhau được nhận |
| bookingRollbackReleasesSchedulingLock | Lỗi validation rollback nhả khóa, transaction connection khác vẫn tạo lớp hợp lệ |

Kết quả cuối **171/171 Java tests pass,0 failures/errors/skipped**, JDK21.0.13/Maven3.9.9; trong đó **58 PostgreSQL16 integration tests** (48 cũ+10 mới) trên database admission_qa_v1/cổng55432. Bộ cũ kiểm tra giữ chỗ/cancel/approve/cash/Student retry/BI/email/SMTP/audit vẫn pass. **15/15 invariants,10/10 Node frontend regressions và Chrome7 trang+BI ADMIN/STAFF/guest pass**. Browser dùng API fixtures, không phải E2E toàn hệ thống. git diff --check pass.

Log tại thư mục QA nêu trên: red.log, red-extra.log, fixture-diagnostic.log, java-first.log (1 fixture failure), **verified.log (171 pass)**, invariants.log, frontend.log, browser.log. Không gửi mail thật, không chạy Finance/Student thật. Test giữ latches trong thời gian ngắn, futures có timeout và luôn release/shutdown trong finally; không dùng sleep để giả lập assertion.

## Trạng thái hạng mục

H02 và H03 được sửa, đã có bằng chứng trước/sau trên PostgreSQL cho phạm vi writer và preset BA nêu trên. Giới hạn còn lại: dữ liệu legacy cần đối soát riêng; CUSTOM cần định nghĩa nghiệp vụ; direct SQL/phiên bản cũ/isolation khác cần cơ chế tương thích. Các lỗi audit khác giữ nguyên, không đổi kết luận Admission V1 tổng thể thành feature-complete trong lượt này.
