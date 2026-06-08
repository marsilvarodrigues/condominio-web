package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.email.Template;
import io.micrometer.core.annotation.Timed;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.StringWriter;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service that composes and sends transactional HTML emails.
 *
 * <p>Templates are Velocity {@code .vm} files loaded from the classpath under {@code
 * templates/email/}. Each template is paired with a {@link Template} implementation whose component
 * names match the {@code $variableName} placeholders in the file.
 *
 * <p>The single public entry point is {@link #sendEmail(String, Template)}; {@link
 * #sendHtml(String, String, String)} is available for callers that already hold a pre-rendered
 * body.
 */
@Slf4j
@Service
public class MailService {

  private final JavaMailSender mailSender;
  private final VelocityEngine velocityEngine;

  /**
   * Constructs a {@code MailService} with the given mail sender and a classpath-based Velocity
   * engine.
   *
   * @param mailSender Spring mail sender (auto-configured from {@code spring.mail.*})
   */
  public MailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
    this.velocityEngine = buildVelocityEngine();
  }

  /**
   * Renders the given template with Velocity and sends the result as an HTML email.
   *
   * @param to recipient email address
   * @param template self-describing email template (path, subject, model variables)
   * @throws RuntimeException if Velocity rendering or mail sending fails
   */
  @Timed(value = "mail.service.sendEmail", description = "Send email from Velocity template")
  public void sendEmail(String to, Template template) {
    log.info("Sending email to: {} template: {}", to, template.templatePath());
    try {
      var context = new VelocityContext(template.model());
      var writer = new StringWriter();
      velocityEngine.mergeTemplate(template.templatePath(), "UTF-8", context, writer);
      sendHtml(to, template.subject(), writer.toString());
    } catch (RuntimeException e) {
      log.error(
          "Failed to render or send email template={} to={}: {}",
          template.templatePath(),
          to,
          e.getMessage());
      throw e;
    }
  }

  /**
   * Sends a pre-rendered HTML email body directly, bypassing template rendering.
   *
   * @param to recipient email address
   * @param subject email subject line
   * @param htmlBody fully rendered HTML body
   * @throws RuntimeException wrapping {@link MessagingException} if the mail cannot be sent
   */
  @Timed(value = "mail.service.sendHtml", description = "Send pre-rendered HTML email")
  public void sendHtml(String to, String subject, String htmlBody) {
    log.info("Sending HTML email to: {} subject: {}", to, subject);
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);
      mailSender.send(message);
      log.info("HTML email sent successfully to: {}", to);
    } catch (MessagingException e) {
      log.error("Failed to send HTML email to: {} - {}", to, e.getMessage());
      throw new RuntimeException("Failed to send HTML email to: " + to, e);
    }
  }

  private static VelocityEngine buildVelocityEngine() {
    var props = new Properties();
    props.setProperty("resource.loaders", "classpath");
    props.setProperty(
        "resource.loader.classpath.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    var engine = new VelocityEngine();
    engine.init(props);
    return engine;
  }
}
