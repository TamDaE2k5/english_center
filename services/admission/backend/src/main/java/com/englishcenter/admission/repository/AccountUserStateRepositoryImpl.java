package com.englishcenter.admission.repository;
import com.englishcenter.admission.entity.User;
import jakarta.persistence.*;
/** Fragment refresh tài khoản trong transaction để bảo vệ resend/verify đồng thời. */
public class AccountUserStateRepositoryImpl implements AccountUserStateRepository {
    @PersistenceContext private EntityManager em;
    public void refreshForAccountUse(User user) { em.refresh(user, LockModeType.PESSIMISTIC_WRITE); }
}
