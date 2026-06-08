package com.pmrodrigues.gateway.dto;

/**
 * Asaas webhook notification payload.
 *
 * <p>Asaas sends a POST to the configured webhook URL whenever a payment event occurs. This record
 * captures the fields the application needs to process payment confirmations.
 *
 * @param event event type (e.g. {@code PAYMENT_RECEIVED}, {@code PAYMENT_CONFIRMED})
 * @param payment embedded payment summary
 */
public record AsaasWebhookPayload(String event, AsaasWebhookPayment payment) {

  /**
   * Summary of the payment referenced by the webhook event.
   *
   * @param id Asaas payment ID
   * @param status current Asaas payment status
   * @param value payment amount as a string
   * @param paymentDate date the payment was received (ISO date)
   */
  public record AsaasWebhookPayment(String id, String status, String value, String paymentDate) {}
}
