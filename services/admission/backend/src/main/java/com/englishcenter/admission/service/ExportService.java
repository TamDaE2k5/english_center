package com.englishcenter.admission.service;

/**
 * Giao diện dịch vụ xuất dữ liệu báo cáo danh sách lớp và kết quả khảo thí ra CSV/Excel (TASK-ADM-50, FR-OPS-01, FR-OPS-02).
 */
public interface ExportService {

    /**
     * Xuất danh sách học viên của lớp học ra mảng byte CSV chuẩn UTF-8 kèm BOM.
     * Chỉ Học vụ và Quản trị viên được xuất. ID tài khoản lấy từ khóa Admission của đơn.
     *
     * @param classId ID lớp học
     * @return Mảng byte định dạng CSV UTF-8
     */
    byte[] exportClassRoster(Integer classId);

    /**
     * Xuất bảng điểm kết quả bài thi khảo thí theo bộ đề ra mảng byte CSV chuẩn UTF-8 kèm BOM.
     *
     * @param testSetId ID bộ đề khảo thí
     * @return Mảng byte định dạng CSV UTF-8
     */
    byte[] exportTestResults(Integer testSetId);
}
