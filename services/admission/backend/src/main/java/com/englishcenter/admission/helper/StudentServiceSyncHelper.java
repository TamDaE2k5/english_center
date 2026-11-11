package com.englishcenter.admission.helper;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/** Giữ cổng helper hiện có, thay HTTP trong transaction bằng outbox nguyên tử. */
@Component
public class StudentServiceSyncHelper {
 private final StudentSyncOutbox outbox; private final EnrollmentRepository enrollments; private final UserRepository users;
 public StudentServiceSyncHelper(StudentSyncOutbox outbox,EnrollmentRepository enrollments,UserRepository users){this.outbox=outbox;this.enrollments=enrollments;this.users=users;}
 /** Chỉ APPROVED được enqueue và bắt buộc nằm trong transaction duyệt; lỗi ghi outbox phải rollback duyệt. */
 @Transactional(propagation=Propagation.MANDATORY)
 public void syncStudentProfile(Enrollment e){
  if(e==null||!"APPROVED".equals(e.getStatus()))return;
  var user=users.findById(e.getStudentId()).orElseThrow(()->new IllegalStateException("Missing approved user"));
  enrollments.flush();outbox.enqueue(e,user);
  boolean confirmed=outbox.confirmed(user.getId());
  e.setIsSyncedToStudent(confirmed);e.setStudentSyncStatus(confirmed?"SYNCED":"PENDING");enrollments.save(e);
 }
}
