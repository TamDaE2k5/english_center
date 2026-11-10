package com.englishcenter.admission.repository;
import com.englishcenter.admission.entity.User;
/** Nạp lại trạng thái tài khoản sau khi đợi khóa. */
public interface AccountUserStateRepository { void refreshForAccountUse(User user); }
