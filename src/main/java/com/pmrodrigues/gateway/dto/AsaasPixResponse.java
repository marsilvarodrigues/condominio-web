package com.pmrodrigues.gateway.dto;

/**
 * Pix payment details returned by Asaas inside a charge response.
 *
 * @param encodedImage   Base64-encoded PNG QR Code image (returned by Asaas directly)
 * @param payload        Pix copia-e-cola EMV string
 * @param expirationDate ISO date/time when the Pix payload expires
 */
public record AsaasPixResponse(String encodedImage, String payload, String expirationDate) {}
