package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.email.Template;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock MimeMessage mimeMessage;

    MailService mailService;

    /** Minimal stub template that points to a real classpath template. */
    private static final Template ACTIVATION_TEMPLATE = new Template() {
        @Override public String templatePath() { return "templates/email/activation.vm"; }
        @Override public String subject()      { return "Test Subject"; }
        @Override public Map<String, Object> model() {
            return Map.of(
                    "userName", "Test User",
                    "password", "tempPass",
                    "activationUrl", "http://localhost/activate?token=abc"
            );
        }
    };

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender);
    }

    // ── sendEmail ─────────────────────────────────────────────────────────

    @Test
    void sendEmail_callsMailSendOnce() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendEmail("user@test.com", ACTIVATION_TEMPLATE);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_createsMimeMessageFromSender() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendEmail("user@test.com", ACTIVATION_TEMPLATE);

        verify(mailSender).createMimeMessage();
    }

    @Test
    void sendEmail_whenMessagingExceptionThrown_throwsRuntimeException() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("SMTP error"))
                .when(mimeMessage).setSubject(anyString(), anyString()); // NOSONAR — Mockito stub

        assertThatThrownBy(() -> mailService.sendEmail("user@test.com", ACTIVATION_TEMPLATE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send HTML email to:")
                .cause()
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void sendEmail_doesNotSendWhenMessageCreationFails() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("SMTP error"))
                .when(mimeMessage).setSubject(anyString(), anyString()); // NOSONAR — Mockito stub

        try {
            mailService.sendEmail("user@test.com", ACTIVATION_TEMPLATE);
        } catch (RuntimeException ignored) {}

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ── sendHtml ──────────────────────────────────────────────────────────

    @Test
    void sendHtml_callsMailSendOnce() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendHtml("user@test.com", "Subject", "<p>Body</p>");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendHtml_whenMessagingExceptionThrown_throwsRuntimeException() throws MessagingException {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("SMTP error"))
                .when(mimeMessage).setSubject(anyString(), anyString()); // NOSONAR — Mockito stub

        assertThatThrownBy(() -> mailService.sendHtml("user@test.com", "Subject", "<p>Body</p>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send HTML email to:")
                .cause()
                .isInstanceOf(MessagingException.class);
    }
}
