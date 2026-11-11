package com.englishcenter.admission.service;
import org.junit.jupiter.api.Test;
import com.englishcenter.admission.helper.StudentServiceSyncHelper;
import com.englishcenter.admission.client.StudentServiceClient;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.repository.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
/** TĂ¡i hiá»‡n helper cÅ© gá»­i há»“ sÆ¡ khi Ä‘Æ¡n chÆ°a APPROVED. */
class StudentSyncGuardTest {
 @Test void pendingMustNeverContactStudent() {
  var client=mock(StudentServiceClient.class);var enrollments=mock(EnrollmentRepository.class);var users=mock(UserRepository.class);
  var e=new Enrollment();e.setId(1);e.setStudentId(2);e.setStatus("PENDING");
  var u=new User();u.setId(2);when(users.findById(2)).thenReturn(Optional.of(u));
  var outbox=mock(StudentSyncOutbox.class);new StudentServiceSyncHelper(outbox,enrollments,users).syncStudentProfile(e);
  verifyNoInteractions(outbox,enrollments);
  verifyNoInteractions(client);
 }
}
