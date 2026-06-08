package com.pmrodrigues.commons.email;

import java.util.Map;

/**
 * Contract for HTML email templates rendered via Apache Velocity.
 *
 * <p>Each implementation is a self-contained value object (typically a Java {@code record}) whose
 * component names match the variable names used in the corresponding {@code .vm} template file. The
 * {@link #model()} method exposes those components as a {@code Map<String, Object>} that Velocity
 * uses to populate {@code $variableName} placeholders in the template.
 *
 * <p>Implementations live in the module that owns the email type:
 *
 * <ul>
 *   <li>{@code commons.email} for cross-cutting mails (e.g. account activation)
 *   <li>{@code cobranca.email} for billing-specific mails
 * </ul>
 */
public interface Template {

  /**
   * Classpath-relative path to the Velocity {@code .vm} template file, e.g. {@code
   * "templates/email/activation.vm"}.
   *
   * @return classpath path to the template
   */
  String templatePath();

  /**
   * Email subject line sent to the recipient.
   *
   * @return subject string (plain text, no HTML)
   */
  String subject();

  /**
   * Model variables passed to the Velocity engine for rendering. Keys must match the {@code
   * $variableName} placeholders in the template file exactly.
   *
   * @return model map; values must not be null (use empty string for absent optional data)
   */
  Map<String, Object> model();
}
