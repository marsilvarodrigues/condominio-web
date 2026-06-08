package com.pmrodrigues.commons.email;

import java.util.Map;

/**
 * Email template for the account-activation message sent when a new user is created.
 *
 * <p>Template file: {@code templates/email/activation.vm} Variable names match the record
 * components directly.
 *
 * @param userName display name shown in the email greeting
 * @param password temporary plain-text password
 * @param activationUrl full activation URL (base URL + path + token)
 */
public record ActivationEmailTemplate(String userName, String password, String activationUrl)
    implements Template {

  @Override
  public String templatePath() {
    return "templates/email/activation.vm";
  }

  @Override
  public String subject() {
    return "Bem-vindo ao CondoGest — Ative sua conta em até 24 horas";
  }

  @Override
  public Map<String, Object> model() {
    return Map.of(
        "userName", userName,
        "password", password,
        "activationUrl", activationUrl);
  }
}
