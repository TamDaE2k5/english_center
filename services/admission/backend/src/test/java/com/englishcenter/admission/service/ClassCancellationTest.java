package com.englishcenter.admission.service;
import com.englishcenter.admission.entity.*;
import com.englishcenter.admission.repository.*;
import com.englishcenter.admission.service.impl.ClassServiceImpl;
import com.englishcenter.admission.engine.ConflictEngine;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
/** Tái hiện cascade và điều kiện hủy trước khi sửa. */
class ClassCancellationTest {
 @Test void cancellationCascadesToEnrollments() {
  ClassRepository classes=mock(ClassRepository.class); EnrollmentRepository enrollments=mock(EnrollmentRepository.class);
  ClassEntity c=new ClassEntity(); c.setId(1);c.setStatus("OPEN");c.setStartDate(LocalDate.now().plusDays(2));c.setMinStudents(5);c.setMaxStudents(10);
  when(classes.findById(1)).thenReturn(Optional.of(c));when(classes.findByIdWithLock(1)).thenReturn(Optional.of(c));when(classes.save(any())).thenAnswer(i->i.getArgument(0));
  Enrollment pending=new Enrollment();pending.setId(2);pending.setStatus("PENDING");
  Enrollment approved=new Enrollment();approved.setId(3);approved.setStatus("APPROVED");
  when(enrollments.findByClassIdOrderByCreatedAtAsc(1)).thenReturn(List.of(pending,approved));
  var service=new ClassServiceImpl(classes,mock(CourseRepository.class),mock(RoomRepository.class),mock(UserRepository.class),enrollments,mock(ConflictEngine.class));
  service.updateClassStatus(1,"CANCELLED");
  assertEquals("CANCELLED",pending.getStatus());assertEquals("CANCELLED_REFUND_PENDING",approved.getStatus());
 }
}
