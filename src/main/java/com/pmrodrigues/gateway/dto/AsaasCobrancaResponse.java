package com.pmrodrigues.gateway.dto;

/**
 * Subset of the Asaas {@code Payment} object returned by {@code POST /payments}
 * and {@code GET /payments/{id}}.
 *
 * @param id           Asaas payment ID (e.g. {@code pay_000123456789})
 * @param status       Asaas payment status string
 * @param bankSlipUrl  URL to download the boleto PDF
 * @param invoiceUrl   URL to the Asaas invoice page
 * @param nossoNumero  boleto barcode / nosso número
 * @param pix          Pix-specific payment details (may be null when billing type is boleto only)
 */
public record AsaasCobrancaResponse(
        String id,
        String status,
        String bankSlipUrl,
        String invoiceUrl,
        String nossoNumero,
        AsaasPixResponse pix
) {}
