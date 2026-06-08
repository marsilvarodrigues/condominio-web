package com.pmrodrigues.gateway.dto;

/**
 * Result of a successful Asaas charge issuance — all identifiers and payment tokens needed
 * to persist and communicate the charge to the resident.
 *
 * @param asaasId         Asaas payment ID (e.g. {@code pay_000123456789})
 * @param asaasCustomerId Asaas customer ID (e.g. {@code cus_000123456789})
 * @param boletoUrl       URL to download the boleto PDF (may be null)
 * @param boletoCodBarras boleto barcode / nosso número (may be null)
 * @param pixQrCodeBase64 Base64-encoded PNG QR Code image (null if no Pix payload)
 * @param pixCopiaCola    Pix copia-e-cola EMV string (may be null)
 */
public record AsaasEmissaoResult(
        String asaasId,
        String asaasCustomerId,
        String boletoUrl,
        String boletoCodBarras,
        String pixQrCodeBase64,
        String pixCopiaCola
) {}
