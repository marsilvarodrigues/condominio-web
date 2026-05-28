package com.pmrodrigues.commons.service;

import io.micrometer.core.annotation.Timed;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Service that composes and sends HTML transactional emails via {@link JavaMailSender}.
 * Email bodies are loaded from classpath templates under {@code templates/email/}.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String activationTemplate;

    /**
     * @param mailSender  Spring mail sender
     * @param frontendUrl React application base URL used to build the activation link
     */
    public MailService(JavaMailSender mailSender, @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.activationTemplate = loadTemplate("templates/email/activation.html");
    }

    /**
     * Sends an HTML account-activation email containing the user's name, a temporary password, and an activation link.
     *
     * @param to              recipient email address
     * @param userName        the user's display name shown in the email greeting
     * @param rawPassword     the user's temporary plain-text password to display in the email
     * @param activationToken token appended to the activation URL
     * @throws RuntimeException wrapping {@link MessagingException} if the mail cannot be sent
     */
    @Timed(value = "mail.service.sendActivationEmail", description = "Send activation email")
    public void sendActivationEmail(String to, String userName, String rawPassword, String activationToken) {
        log.info("Sending activation email to: {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Bem-vindo ao CondoGest — Ative sua conta em até 24 horas");

            String activationUrl = frontendUrl + "/auth/activate?token=" + activationToken;
            String body = activationTemplate
                    .replace("{{userName}}", userName)
                    .replace("{{password}}", rawPassword)
                    .replace("{{activationUrl}}", activationUrl);

            helper.setText(body, true);
            mailSender.send(message);
            log.info("Activation email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send activation email to: {} - {}", to, e.getMessage());
            throw new RuntimeException("Failed to send activation email to: " + to, e);
        }
    }

    private static String loadTemplate(String path) {
        try {
            var resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load email template: " + path, e);
        }
    }
}
