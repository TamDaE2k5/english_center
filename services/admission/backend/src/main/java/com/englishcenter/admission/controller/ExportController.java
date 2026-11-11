package com.englishcenter.admission.controller;

import com.englishcenter.admission.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý tải xuống các tệp báo cáo danh sách lớp học và điểm thi khảo thí dạng CSV/Excel (TASK-ADM-50).
 */
@RestController
@RequestMapping("/api/admission/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Xuất danh sách học viên của lớp học ra file CSV UTF-8 kèm BOM (FR-OPS-01).
     * Phân quyền: Học vụ, Quản trị viên, hoặc Giáo viên phụ trách lớp.
     *
     * @param classId ID lớp học
     * @param format  Định dạng xuất (mặc định csv)
     * @return File CSV đính kèm tải về
     */
    @GetMapping("/classes/{classId}/roster")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<byte[]> exportClassRoster(
            @PathVariable("classId") Integer classId,
            @RequestParam(name = "format", defaultValue = "csv") String format) {
        byte[] csvData = exportService.exportClassRoster(classId);

        String filename = String.format("class_%d_roster.csv", classId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Xuất bảng điểm kết quả khảo thí đầu vào của bộ đề ra file CSV UTF-8 (FR-OPS-02).
     * Phân quyền: Học vụ, Quản trị viên.
     *
     * @param testSetId ID bộ đề khảo thí
     * @param format    Định dạng xuất (mặc định csv)
     * @return File CSV bảng điểm đính kèm
     */
    @GetMapping("/tests/{testSetId}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<byte[]> exportTestResults(
            @PathVariable("testSetId") Integer testSetId,
            @RequestParam(name = "format", defaultValue = "csv") String format) {
        byte[] csvData = exportService.exportTestResults(testSetId);

        String filename = String.format("test_set_%d_results.csv", testSetId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
