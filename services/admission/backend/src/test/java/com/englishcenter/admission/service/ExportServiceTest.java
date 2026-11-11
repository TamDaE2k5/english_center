package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.entity.ClassEntity;
import com.englishcenter.admission.entity.Enrollment;
import com.englishcenter.admission.entity.TestSet;
import com.englishcenter.admission.entity.TestSubmission;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.ClassRepository;
import com.englishcenter.admission.repository.EnrollmentRepository;
import com.englishcenter.admission.repository.TestSetRepository;
import com.englishcenter.admission.repository.TestSubmissionRepository;
import com.englishcenter.admission.service.impl.ExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị cho ExportService (TASK-ADM-50).
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private TestSetRepository testSetRepository;

    @Mock
    private TestSubmissionRepository testSubmissionRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    private ClassEntity mockClass;
    private User mockStudent;
    private Enrollment mockEnrollment;
    private TestSet mockTestSet;
    private TestSubmission mockSubmission;

    @BeforeEach
    void setUp() {
        mockClass = new ClassEntity();
        mockClass.setId(10);
        mockClass.setCode("CLS-IELTS-01");
        mockClass.setName("Lớp IELTS Khóa 1");
        mockClass.setTeacherId(20);

        mockStudent = new User();
        mockStudent.setId(101);
        mockStudent.setFullName("Lê Thị Hoa");
        mockStudent.setEmail("hoa.le@test.com");
        mockStudent.setPhone("0912345678");

        mockEnrollment = new Enrollment();
        mockEnrollment.setId(1);
        mockEnrollment.setClassId(10);
        mockEnrollment.setStudentId(101);
        mockEnrollment.setStudent(mockStudent);
        mockEnrollment.setStatus("APPROVED");
        mockEnrollment.setEnrolledAt(LocalDateTime.now());

        mockTestSet = new TestSet();
        mockTestSet.setId(5);
        mockTestSet.setCode("PLACEMENT-A1");
        mockTestSet.setTitle("Bộ đề Khảo thí A1");

        mockSubmission = new TestSubmission();
        mockSubmission.setId(100);
        mockSubmission.setTestSetId(5);
        mockSubmission.setUserId(101);
        mockSubmission.setUser(mockStudent);
        mockSubmission.setFullName("Lê Thị Hoa");
        mockSubmission.setEmail("hoa.le@test.com");
        mockSubmission.setListeningScore(new BigDecimal("30.0"));
        mockSubmission.setReadingScore(new BigDecimal("40.0"));
        mockSubmission.setGrammarScore(new BigDecimal("20.0"));
        mockSubmission.setTotalScore(new BigDecimal("90.0"));
        mockSubmission.setAssessedLevel("ADVANCED");
        mockSubmission.setSubmittedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Xuất danh sách học viên lớp học thành công ra CSV kèm BOM UTF-8")
    void testExportClassRoster_Success() {
        when(classRepository.findById(10)).thenReturn(Optional.of(mockClass));
        when(enrollmentRepository.findByClassIdOrderByCreatedAtAsc(10)).thenReturn(List.of(mockEnrollment));

        byte[] csvBytes = exportService.exportClassRoster(10);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);

        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.startsWith("\uFEFF")); // UTF-8 BOM
        assertTrue(csvContent.contains("Mã đơn,Mã học viên,Họ và tên,Email"));
        assertTrue(csvContent.contains("Lê Thị Hoa"));
        assertTrue(csvContent.contains("hoa.le@test.com"));
        assertTrue(csvContent.contains("APPROVED"));
    }

    @Test
    @DisplayName("Xuất danh sách lớp học thất bại khi lớp không tồn tại")
    void testExportClassRoster_ClassNotFound() {
        when(classRepository.findById(999)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> exportService.exportClassRoster(999));
        assertEquals(ErrorCode.ADM_CLASS_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Xuất bảng điểm khảo thí theo bộ đề thành công ra CSV")
    void testExportTestResults_Success() {
        when(testSetRepository.findById(5)).thenReturn(Optional.of(mockTestSet));
        when(testSubmissionRepository.findByTestSetIdOrderBySubmittedAtDesc(5)).thenReturn(List.of(mockSubmission));

        byte[] csvBytes = exportService.exportTestResults(5);

        assertNotNull(csvBytes);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvContent.startsWith("\uFEFF"));
        assertTrue(csvContent.contains("Điểm Nghe,Điểm Đọc,Điểm Ngữ pháp,Tổng điểm"));
        assertTrue(csvContent.contains("Lê Thị Hoa"));
        assertTrue(csvContent.contains("90.0"));
        assertTrue(csvContent.contains("ADVANCED"));
    }
}
