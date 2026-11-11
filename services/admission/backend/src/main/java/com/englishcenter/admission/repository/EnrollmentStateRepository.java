package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.Enrollment;

/** Đọc lại trạng thái sau khi chờ khóa lớp để không dùng entity cũ trong persistence context. */
public interface EnrollmentStateRepository {
    /** Khóa và refresh đơn đã đọc, chỉ gọi trong transaction sau khi khóa lớp tương ứng. */
    void refreshForMutation(Enrollment enrollment);
}
