package com.englishcenter.admission.service;
import com.englishcenter.admission.common.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** SMTP không hoạt động không được thành công giả hoặc lộ thông tin xác thực trong exception. */
class AccountMailServiceTest {
    @Test void disabledMailRejectsWithoutSending() {
        JavaMailSender sender = mock(JavaMailSender.class);
        AccountMailService service = new AccountMailService(sender, false, "qa@example.test", "http://localhost/account");
        BusinessException e = assertThrows(BusinessException.class, () -> service.send("user@example.test", "verify", "sensitive-token"));
        assertEquals(ErrorCode.AUTH_EMAIL_UNAVAILABLE, e.getErrorCode()); verifyNoInteractions(sender);
    }
    @Test void smtpFailureIsSanitized() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("SMTP password and sensitive-token")).when(sender).send(any(SimpleMailMessage.class));
        AccountMailService service = new AccountMailService(sender, true, "qa@example.test", "http://localhost/account");
        BusinessException e = assertThrows(BusinessException.class, () -> service.send("user@example.test", "reset", "sensitive-token"));
        assertFalse(e.getMessage().contains("sensitive-token")); assertNull(e.getCause());
    }
}
