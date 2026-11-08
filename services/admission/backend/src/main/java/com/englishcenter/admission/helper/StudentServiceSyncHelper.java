package com.englishcenter.admission.helper;

import com.englishcenter.admission.client.StudentServiceClient;
import com.englishcenter.admission.client.dto.StudentProfileData;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.EnrollmentRepository;
import com.englishcenter.admission.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Helper hỗ trợ đồng bộ hồ sơ học viên sang Student Service bất đồng bộ hoặc retry (TASK-ADM-47, FR-ENR-06).
 * Đảm bảo tính nhất quán cuối cùng (Eventual Consistency): Nếu Student Service tạm thời bận,
 * không làm rollback giao dịch duyệt đơn mà lưu cờ để thử lại sau.
 */
@Component
public class StudentServiceSyncHelper {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceSyncHelper.class);

    private final StudentServiceClient studentServiceClient;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public StudentServiceSyncHelper(StudentServiceClient studentServiceClient,
                                   EnrollmentRepository enrollmentRepository,
                                   UserRepository userRepository) {
        this.studentServiceClient = studentServiceClient;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Đồng bộ hồ sơ học viên khi đơn tuyển sinh được duyệt.
     *
     * @param enrollment Đơn đăng ký vừa được duyệt
     */
    public void syncStudentProfile(Enrollment enrollment) {
        if (enrollment == null || enrollment.getStudentId() == null) {
            return;
        }

        User user = userRepository.findById(enrollment.getStudentId()).orElse(null);
        if (user == null) {
            log.warn("Không tìm thấy thông tin user ID={} để đồng bộ sang Student Service", enrollment.getStudentId());
            return;
        }

        try {
            StudentProfileData data = studentServiceClient.syncStudentProfile(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getDateOfBirth()
            );

            if (data != null) {
                enrollment.setIsSyncedToStudent(true);
                enrollment.setStudentSyncStatus("SYNCED");
                enrollmentRepository.save(enrollment);
                log.info("Đã đồng bộ hồ sơ học viên thành công sang Student Service cho đơn ID={}", enrollment.getId());
            }
        } catch (Exception e) {
            log.warn("Đồng bộ hồ sơ sang Student Service thất bại (sẽ được thử lại sau): enrollment_id={}, lý do={}",
                    enrollment.getId(), e.getMessage());
            enrollment.setIsSyncedToStudent(false);
            enrollment.setStudentSyncStatus("FAILED");
            enrollmentRepository.save(enrollment);
        }
    }
}
