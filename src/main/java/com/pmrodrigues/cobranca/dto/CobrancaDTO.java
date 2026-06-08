package com.pmrodrigues.cobranca.dto;

import com.pmrodrigues.cobranca.model.StatusCobranca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full representation of a {@code Cobranca} returned to API clients.
 *
 * @param id                charge primary key
 * @param apartamentoId     apartment FK
 * @param apartamentoNumero apartment unit number
 * @param blocoNome         building block identifier (may be null)
 * @param moradorId         resident FK (may be null)
 * @param moradorNome       resident display name (enriched by service; not mapped by MapStruct)
 * @param moradorEmail      resident email (enriched by service; not mapped by MapStruct)
 * @param valor             charge amount in BRL
 * @param vencimento        due date
 * @param status            current lifecycle status
 * @param boletoUrl         URL to the boleto PDF on Asaas (may be null)
 * @param boletoCodBarras   barcode / nosso número (may be null)
 * @param pixQrCodeBase64   Base64-encoded PNG of the Pix QR Code (may be null)
 * @param pixCopiaCola      Pix copia-e-cola EMV payload (may be null)
 * @param emailEnviado      whether the billing email was dispatched
 * @param emailEnviadoEm    timestamp of last email send (may be null)
 * @param pagoEm            payment confirmation timestamp (may be null)
 * @param createdAt         charge creation timestamp
 */
public record CobrancaDTO(
        Long id,
        Long apartamentoId,
        String apartamentoNumero,
        String blocoNome,
        Long moradorId,
        String moradorNome,
        String moradorEmail,
        BigDecimal valor,
        LocalDate vencimento,
        StatusCobranca status,
        String boletoUrl,
        String boletoCodBarras,
        String pixQrCodeBase64,
        String pixCopiaCola,
        boolean emailEnviado,
        LocalDateTime emailEnviadoEm,
        LocalDateTime pagoEm,
        LocalDateTime createdAt
) {}
