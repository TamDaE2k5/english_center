package com.englishcenter.admission.repository;
import com.englishcenter.admission.entity.PasswordResetToken;
/** Nạp lại token sau khi chờ khóa user để chống replay đồng thời. */
public interface AccountTokenStateRepository { void refreshForUse(PasswordResetToken token); }
