package com.englishcenter.admission.controller;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
     * Phân quyền: Học vụ và Quản trị viên theo ma trận BA.
     *
     * @param rawClassId ID lớp học từ URL
     * @param format  Định dạng xuất (mặc định csv)
     * @return File CSV đính kèm tải về
     */
    @GetMapping("/classes/{classId}/roster")
    public ResponseEntity<byte[]> exportClassRoster(
            @PathVariable("classId") String rawClassId,
            @RequestParam(name = "format", defaultValue = "csv") String format) {
        int classId = requirePositiveId(rawClassId);
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
     * @param rawTestSetId ID bộ đề khảo thí từ URL
     * @param format    Định dạng xuất (mặc định csv)
     * @return File CSV bảng điểm đính kèm
     */
    @GetMapping("/tests/{testSetId}/results")
    public ResponseEntity<byte[]> exportTestResults(
            @PathVariable("testSetId") String rawTestSetId,
            @RequestParam(name = "format", defaultValue = "csv") String format) {
        int testSetId = requirePositiveId(rawTestSetId);
        byte[] csvData = exportService.exportTestResults(testSetId);

        String filename = String.format("test_set_%d_results.csv", testSetId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /** Đọc ID từ URL và trả lỗi ADM_VALIDATION_ERROR khi giá trị sai kiểu hoặc không dương. */
    private static int requirePositiveId(String rawId) {
        try {
            int id = Integer.parseInt(rawId);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // Chuỗi sai kiểu hoặc vượt miền Integer dùng cùng lỗi validation.
        }
        throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "ID phải là số nguyên dương");
    }
}
