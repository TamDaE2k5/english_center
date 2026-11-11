package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

/** Outbox bền vững; chỉ thao tác schema Admission, không giữ transaction trong lúc gọi mạng. */
@Repository
public class StudentSyncOutbox {
 private final JdbcTemplate jdbc;
 public StudentSyncOutbox(JdbcTemplate jdbc){this.jdbc=jdbc;}
 /** Snapshot theo user_id, khóa idempotency tự nhiên theo hợp đồng; tham gia transaction duyệt. */
 @Transactional public void enqueue(Enrollment e,User u){
  jdbc.update("INSERT INTO admission.student_sync_outbox(user_id,enrollment_id,full_name,email,phone,date_of_birth,state) VALUES (?,?,?,?,?,?,CASE WHEN EXISTS(SELECT 1 FROM admission.enrollments WHERE student_id=? AND is_synced_to_student=TRUE) THEN 'REVIEW' ELSE 'PENDING' END) ON CONFLICT (user_id) DO UPDATE SET state='PENDING',enrollment_id=EXCLUDED.enrollment_id,last_error=NULL,next_attempt_at=CURRENT_TIMESTAMP WHERE student_sync_outbox.state='CANCELLED' AND student_sync_outbox.attempts=0",u.getId(),e.getId(),u.getFullName(),u.getEmail(),u.getPhone(),u.getDateOfBirth(),u.getId());
 }
 /** Hồ sơ đã có ACK bền vững dùng được cho đơn mới cùng user, không tạo hồ sơ lần hai. */
 public boolean confirmed(Integer userId){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM admission.student_sync_outbox WHERE user_id=? AND state='SUCCEEDED')",Boolean.class,userId));}
 /** Payload được cố định từ lúc duyệt, không đổi dữ liệu giữa các lần retry. */
 public record Job(int userId,int enrollmentId,String fullName,String email,String phone,LocalDate birth,int attempts,String token){}
 /** Thu hồi lease: không gửi lại kết quả mơ hồ nếu chưa xác nhận idempotency từ Student. */
 @Transactional public void recover(boolean verified){
  jdbc.update("UPDATE admission.student_sync_outbox SET state=CASE WHEN attempts>=5 THEN 'EXHAUSTED' WHEN ? THEN 'RETRY' ELSE 'REVIEW' END,last_error='LEASE_EXPIRED_UNCERTAIN',claim_token=NULL,lease_until=NULL,next_attempt_at=CURRENT_TIMESTAMP+INTERVAL '30 seconds',updated_at=CURRENT_TIMESTAMP WHERE state='IN_FLIGHT' AND lease_until<CURRENT_TIMESTAMP",verified);
 }
 /** SKIP LOCKED cùng token fencing chỉ cho một worker sở hữu mỗi lần gửi. */
 @Transactional public Optional<Job> claim(Integer userId){
  jdbc.update("UPDATE admission.student_sync_outbox o SET state='CANCELLED',last_error='NO_ELIGIBLE_ENROLLMENT' WHERE state IN ('PENDING','RETRY') AND NOT EXISTS (SELECT 1 FROM admission.enrollments e JOIN admission.classes c ON c.id=e.class_id WHERE e.student_id=o.user_id AND e.status='APPROVED' AND c.status<>'CANCELLED')");
  String token=UUID.randomUUID().toString();
  return jdbc.query("UPDATE admission.student_sync_outbox SET state='IN_FLIGHT',attempts=attempts+1,claim_token=?,lease_until=CURRENT_TIMESTAMP+INTERVAL '2 minutes',updated_at=CURRENT_TIMESTAMP WHERE user_id=(SELECT user_id FROM admission.student_sync_outbox WHERE state IN ('PENDING','RETRY') AND attempts<5 AND next_attempt_at<=CURRENT_TIMESTAMP AND (?::integer IS NULL OR user_id=?) ORDER BY next_attempt_at,user_id FOR UPDATE SKIP LOCKED LIMIT 1) RETURNING *",
    (r,n)->new Job(r.getInt("user_id"),r.getInt("enrollment_id"),r.getString("full_name"),r.getString("email"),r.getString("phone"),r.getObject("date_of_birth",LocalDate.class),r.getInt("attempts"),token),token,userId,userId).stream().findFirst();
 }
 /** Kết thúc đúng lease đang sở hữu, không để worker cũ ghi đè kết quả mới. */
 @Transactional public void finish(Job j,String state,Integer profile,String error){
  jdbc.update("UPDATE admission.student_sync_outbox SET state=?,student_profile_id=?,last_error=?,next_attempt_at=CURRENT_TIMESTAMP+(? * INTERVAL '1 second'),claim_token=NULL,lease_until=NULL,updated_at=CURRENT_TIMESTAMP WHERE user_id=? AND state='IN_FLIGHT' AND claim_token=? AND lease_until>CURRENT_TIMESTAMP",
   state,profile,error,30*(1 << (j.attempts()-1)),j.userId(),j.token());
 }
 /** Cờ hiển thị cập nhật riêng sau journal; không đảo thứ tự khóa enrollment/outbox của transaction duyệt. */
 @Transactional public void reconcile(){
  jdbc.update("UPDATE admission.enrollments e SET is_synced_to_student=(o.state='SUCCEEDED'),student_sync_status=CASE WHEN o.state='SUCCEEDED' THEN 'SYNCED' WHEN o.state IN ('REVIEW','EXHAUSTED','CANCELLED') THEN 'FAILED' ELSE 'PENDING' END FROM admission.student_sync_outbox o WHERE e.student_id=o.user_id AND e.status='APPROVED' AND (e.student_sync_status IS DISTINCT FROM CASE WHEN o.state='SUCCEEDED' THEN 'SYNCED' WHEN o.state IN ('REVIEW','EXHAUSTED','CANCELLED') THEN 'FAILED' ELSE 'PENDING' END OR e.is_synced_to_student IS DISTINCT FROM (o.state='SUCCEEDED'))");
 }
}
