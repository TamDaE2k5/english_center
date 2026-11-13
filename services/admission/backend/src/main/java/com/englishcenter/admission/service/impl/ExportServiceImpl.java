package com.englishcenter.admission.service.impl;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.entity.TestSet;
import com.englishcenter.admission.entity.TestSubmission;
import com.englishcenter.admission.repository.ClassRepository;
import com.englishcenter.admission.repository.EnrollmentRepository;
import com.englishcenter.admission.repository.TestSetRepository;
import com.englishcenter.admission.repository.TestSubmissionRepository;
import com.englishcenter.admission.service.ExportService;
import com.englishcenter.admission.util.CsvExportHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Triển khai dịch vụ xuất danh sách học viên lớp học và điểm khảo thí ra CSV UTF-8 (TASK-ADM-50).
 */
@Service
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TestSetRepository testSetRepository;
    private final TestSubmissionRepository testSubmissionRepository;

    public ExportServiceImpl(ClassRepository classRepository,
                             EnrollmentRepository enrollmentRepository,
                             TestSetRepository testSetRepository,
                             TestSubmissionRepository testSubmissionRepository) {
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.testSetRepository = testSetRepository;
        this.testSubmissionRepository = testSubmissionRepository;
    }

    /**
     * Xuất danh sách của lớp theo ID tài khoản Admission. Lớp không tồn tại trả ADM_CLASS_NOT_FOUND.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportClassRoster(Integer classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_CLASS_NOT_FOUND, "Không tìm thấy lớp học ID: " + classId));

        List<Enrollment> enrollments = enrollmentRepository.findByClassIdOrderByCreatedAtAsc(classId);

        List<String> headers = List.of(
                "STT",
                "Mã đơn",
                "ID tài khoản Admission",
                "Họ và tên",
                "Email",
                "Số điện thoại",
                "Trạng thái đơn",
                "Kênh đăng ký",
                "Hạn giữ chỗ",
                "Thời gian đăng ký"
        );

        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (Enrollment e : enrollments) {
            String admissionUserId = Objects.toString(e.getStudentId(), "");
            String fullName = e.getStudent() != null ? e.getStudent().getFullName() : "N/A";
            String email = e.getStudent() != null ? e.getStudent().getEmail() : "N/A";
            String phone = e.getStudent() != null ? e.getStudent().getPhone() : "N/A";
            String holdExpiry = e.getHoldExpiresAt() != null ? e.getHoldExpiresAt().format(DATE_TIME_FORMATTER) : "N/A";
            String enrolledAt = e.getEnrolledAt() != null ? e.getEnrolledAt().format(DATE_TIME_FORMATTER) : "N/A";

            List<String> row = List.of(
                    String.valueOf(stt++),
                    String.format("ENR-%d", e.getId()),
                    admissionUserId,
                    fullName,
                    email,
                    phone != null ? phone : "",
                    e.getStatus(),
                    e.getEnrollmentChannel() != null ? e.getEnrollmentChannel() : "ONLINE",
                    holdExpiry,
                    enrolledAt
            );
            rows.add(row);
        }

        return CsvExportHelper.generateCsvBytes(headers, rows);
    }

    /**
     * Xuất bảng điểm kết quả khảo thí theo bộ đề ra CSV UTF-8.
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportTestResults(Integer testSetId) {
        TestSet testSet = testSetRepository.findById(testSetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_TEST_NOT_FOUND, "Không tìm thấy bộ đề khảo thí ID: " + testSetId));

        List<TestSubmission> submissions = testSubmissionRepository.findByTestSetIdOrderBySubmittedAtDesc(testSetId);

        List<String> headers = List.of(
                "STT",
                "Mã bài nộp",
                "Mã thí sinh",
                "Họ và tên",
                "Email",
                "Số điện thoại",
                "Điểm Nghe",
                "Điểm Đọc",
                "Điểm Ngữ pháp",
                "Tổng điểm",
                "Xếp loại",
                "Khóa học đề xuất",
                "Thời gian nộp bài"
        );

        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (TestSubmission s : submissions) {
            String candidateCode = String.format("USR-%d", s.getUserId());
            String fullName = s.getFullName() != null ? s.getFullName() : (s.getUser() != null ? s.getUser().getFullName() : "N/A");
            String email = s.getEmail() != null ? s.getEmail() : (s.getUser() != null ? s.getUser().getEmail() : "N/A");
            String phone = s.getPhone() != null ? s.getPhone() : (s.getUser() != null ? s.getUser().getPhone() : "");
            String listening = s.getListeningScore() != null ? s.getListeningScore().toString() : "0.0";
            String reading = s.getReadingScore() != null ? s.getReadingScore().toString() : "0.0";
            String grammar = s.getGrammarScore() != null ? s.getGrammarScore().toString() : "0.0";
            String total = s.getTotalScore() != null ? s.getTotalScore().toString() : "0.0";
            String level = s.getAssessedLevel() != null ? s.getAssessedLevel() : "N/A";
            String recommended = s.getRecommendedCourse() != null ? s.getRecommendedCourse().getName() : "Chưa có";
            String submittedAt = s.getSubmittedAt() != null ? s.getSubmittedAt().format(DATE_TIME_FORMATTER) : "N/A";

            List<String> row = List.of(
                    String.valueOf(stt++),
                    String.format("SUB-%d", s.getId()),
                    candidateCode,
                    fullName,
                    email,
                    phone != null ? phone : "",
                    listening,
                    reading,
                    grammar,
                    total,
                    level,
                    recommended,
                    submittedAt
            );
            rows.add(row);
        }

        return CsvExportHelper.generateCsvBytes(headers, rows);
    }
}
