package com.englishcenter.admission.security;

import com.englishcenter.admission.common.exception.BusinessException;
import com.englishcenter.admission.common.exception.ErrorCode;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dịch vụ nạp thông tin người dùng từ cơ sở dữ liệu để phục vụ xác thực Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Nạp người dùng thông qua email (được dùng làm username trong hệ thống).
     *
     * @param email Địa chỉ email của người dùng
     * @return Đối tượng UserDetails (UserPrincipal)
     * @throws UsernameNotFoundException Nếu không tìm thấy người dùng với email tương ứng
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        return UserPrincipal.create(user);
    }

    /**
     * Nạp thông tin người dùng thông qua ID khóa chính (phục vụ JWT Filter khi giải mã claim sub).
     *
     * @param id ID của người dùng
     * @return Đối tượng UserDetails (UserPrincipal)
     * @throws BusinessException Nếu không tìm thấy người dùng theo ID
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADM_USER_NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));

        return UserPrincipal.create(user);
    }
}
