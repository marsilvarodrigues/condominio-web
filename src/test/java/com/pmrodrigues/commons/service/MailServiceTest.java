package com.pmrodrigues.commons.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock MimeMessage mimeMessage;

    MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender, "http://localhost:5173");
    }

    // ── sendActivationEmail ───────────────────────────────────────────────

    @Test
    void sendActivationEmail_callsMailSendOnce() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendActivationEmail("user@test.com", "Maria Santos", "tempPass123", "activation-uuid");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendActivationEmail_createsMimeMessageFromSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendActivationEmail("user@test.com", "Maria Santos", "tempPass123", "activation-uuid");

        verify(mailSender).createMimeMessage();
    }

    @Test
    void sendActivationEmail_whenMessagingExceptionThrown_throwsRuntimeException() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("SMTP error"))
                .when(mimeMessage).setSubject(anyString(), anyString()); // NOSONAR — Mockito stub

        assertThatThrownBy(() ->
                mailService.sendActivationEmail("user@test.com", "Test User", "tempPass", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send activation email")
                .cause()
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void sendActivationEmail_doesNotSendWhenMessageCreationFails() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("SMTP error"))
                .when(mimeMessage).setSubject(anyString(), anyString()); // NOSONAR — Mockito stub

        try {
            mailService.sendActivationEmail("user@test.com", "Test User", "tempPass", "token");
        } catch (RuntimeException ignored) {}

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}