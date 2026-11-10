package com.englishcenter.admission.service;

import com.englishcenter.admission.common.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Gửi link tài khoản qua SMTP môi trường; không ghi body/link/token vào log. */
@Service
public class AccountMailService {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;
    private final String accountUrl;
    /** Nhận sender và URL cấu hình tin cậy, không dùng Host header của client. */
    public AccountMailService(JavaMailSender sender, @Value("${admission.mail.enabled:false}") boolean enabled,
            @Value("${admission.mail.from}") String from, @Value("${admission.mail.account-url}") String accountUrl) {
        this.sender = sender; this.enabled = enabled; this.from = from; this.accountUrl = accountUrl;
    }
    /** SMTP tắt thì cả email tồn tại/không tồn tại nhận cùng lỗi cấu hình. */
    public void requireAvailable() { if (!enabled) throw new BusinessException(ErrorCode.AUTH_EMAIL_UNAVAILABLE); }
    /** Token trong fragment tránh access log/referrer; SMTP failure không được log nội dung exception. */
    public void send(String recipient, String action, String rawToken) {
        requireAvailable();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(recipient); message.setSubject("English Center - Huong dan tai khoan");
        message.setText("Lien ket co hieu luc 15 phut, chi dung mot lan:\n" + accountUrl + "#" + action + "=" + rawToken
                + "\nNeu ban khong yeu cau, vui long bo qua email nay.");
        try { sender.send(message); }
        catch (MailException ex) { throw new BusinessException(ErrorCode.AUTH_EMAIL_UNAVAILABLE); }
    }
}
