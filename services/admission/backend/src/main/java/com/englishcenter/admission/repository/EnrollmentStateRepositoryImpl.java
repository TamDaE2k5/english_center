package com.englishcenter.admission.repository;

import com.englishcenter.admission.entity.Enrollment;
import jakarta.persistence.*;

/** Thao tác JPA refresh có khóa dòng để bảo vệ trạng thái khi nhiều transaction cạnh tranh. */
public class EnrollmentStateRepositoryImpl implements EnrollmentStateRepository {
    @PersistenceContext private EntityManager entityManager;

    /** Nạp lại đơn từ DB sau khi khóa lớp; không thay đổi trường nghiệp vụ trong Repository. */
    @Override public void refreshForMutation(Enrollment enrollment) {
        entityManager.refresh(enrollment, LockModeType.PESSIMISTIC_WRITE);
    }
}
