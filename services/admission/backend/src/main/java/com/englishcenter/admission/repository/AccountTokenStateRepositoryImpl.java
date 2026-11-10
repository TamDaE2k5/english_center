package com.englishcenter.admission.repository;
import com.englishcenter.admission.entity.PasswordResetToken;
import jakarta.persistence.*;
/** Fragment JPA refresh token trong transaction tài khoản. */
public class AccountTokenStateRepositoryImpl implements AccountTokenStateRepository {
    @PersistenceContext private EntityManager em;
    public void refreshForUse(PasswordResetToken token) { em.refresh(token, LockModeType.PESSIMISTIC_WRITE); }
}
