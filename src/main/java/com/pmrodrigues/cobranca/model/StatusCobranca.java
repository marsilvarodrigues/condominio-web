package com.pmrodrigues.cobranca.model;

/**
 * Lifecycle states of a {@link Cobranca} entity.
 *
 * <ul>
 *   <li>{@code PENDENTE} — charge created locally, not yet sent to Asaas
 *   <li>{@code ENVIADA} — charge sent to Asaas and boleto/Pix generated
 *   <li>{@code VISUALIZADA} — payer opened the boleto link
 *   <li>{@code PAGA} — payment confirmed via Asaas webhook
 *   <li>{@code VENCIDA} — due date passed without payment
 *   <li>{@code CANCELADA} — manually cancelled by an admin
 * </ul>
 */
public enum StatusCobranca {
  PENDENTE,
  ENVIADA,
  VISUALIZADA,
  PAGA,
  VENCIDA,
  CANCELADA
}
