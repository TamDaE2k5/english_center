package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.*;
import com.englishcenter.admission.dto.test.*;
import com.englishcenter.admission.entity.User;
import com.englishcenter.admission.repository.TestAttemptRepository;
import com.englishcenter.admission.repository.UserRepository;
import com.englishcenter.admission.security.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Biên xác thực của API khảo thí công khai; lõi chấm điểm vẫn độc lập với HTTP. */
@Service
public class PublicPlacementTestService {
    private final PlacementTestService engine;
    private final UserService users;
    private final TestAttemptRepository attempts;
    private final PlacementAttemptToken tokens;
    private final UserRepository userRepository;

    /** Nhận lõi thi, kho tài khoản/lượt thi và bộ token để thực thi chính sách sở hữu. */
    public PublicPlacementTestService(PlacementTestService engine, UserService users,
                                     TestAttemptRepository attempts, PlacementAttemptToken tokens, UserRepository userRepository) {
        this.engine = engine; this.users = users; this.attempts = attempts; this.tokens = tokens;
        this.userRepository = userRepository;
    }

    /** Người đăng nhập chỉ thi cho chính mình; khách tạo tài khoản mới, không liên kết email có sẵn. */
    @Transactional
    public TakeTestResponse startTest(Integer setId, StartTestRequest request) {
        Integer owner = SecurityUtils.getCurrentUserId();
        if (request != null && request.getUserId() != null && !request.getUserId().equals(owner)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "Không được tạo lượt thi cho tài khoản khác");
        }
        if (owner == null) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_UNVERIFIED);
        }
        requireVerified(owner);
        TakeTestResponse response = engine.startTest(setId, new StartTestRequest(owner, null, null, null));
        response.setAttemptToken(tokens.issue(response.getAttemptId(), response.getExpiresAt()));
        return response;
    }

    /** Cho chính chủ đã xác minh tải lại lượt thi theo ID, giữ nguyên deadline. */
    @Transactional(readOnly = true)
    public TakeTestResponse resumeTest(Integer attemptId) {
        Integer owner = SecurityUtils.getCurrentUserId();
        if (owner == null) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        requireVerified(owner);
        return engine.resumeTest(attemptId, owner);
    }

    /** Nộp bài chỉ khi có principal chính chủ hoặc token được cấp riêng cho đúng lượt thi. */
    @Transactional(noRollbackFor = TestAttemptExpiredException.class)
    public TestResultResponse submitAndGrade(SubmitTestRequest request) {
        if (request.getAttemptId() == null) {
            throw new BusinessException(ErrorCode.ADM_VALIDATION_ERROR, "Bắt buộc cung cấp attempt_id");
        }
        Integer owner = SecurityUtils.getCurrentUserId();
        Integer attemptOwner = attempts.findUserIdById(request.getAttemptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));
        if (!attemptOwner.equals(owner) && !tokens.permits(request.getAttemptToken(), request.getAttemptId())) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "Bạn không có quyền nộp lượt thi này");
        }
        requireVerified(attemptOwner);
        return engine.submitAndGrade(request);
    }

    /** Lượt thi chính thức chỉ gắn tài khoản STUDENT đã xác minh và thiết lập mật khẩu. */
    private void requireVerified(Integer id) {
        User user = userRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FORBIDDEN));
        if (!Boolean.TRUE.equals(user.getIsActive())) throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        if (!"STUDENT".equals(user.getRole())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if (user.getEmailVerifiedAt() == null || Boolean.TRUE.equals(user.getMustChangePassword()))
            throw new BusinessException(ErrorCode.AUTH_EMAIL_UNVERIFIED);
    }
}
