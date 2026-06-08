package com.pmrodrigues.gateway.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input for a single Asaas charge issuance: all data needed to create or reuse an Asaas customer
 * and emit a BOLETO_PIX charge.
 *
 * @param pessoaId         local Pessoa primary key (used to look up or create the Asaas customer)
 * @param nome             resident full name
 * @param cpf              CPF — digits only
 * @param email            resident email address
 * @param valor            charge amount in BRL
 * @param vencimento       due date
 * @param descricao        charge description displayed to the payer
 * @param multaPercent     one-time fine percentage applied when overdue
 * @param jurosMoraPercent monthly interest rate applied after the due date
 */
public record AsaasEmissaoRequest(
        Long pessoaId,
        String nome,
        String cpf,
        String email,
        BigDecimal valor,
        LocalDate vencimento,
        String descricao,
        BigDecimal multaPercent,
        BigDecimal jurosMoraPercent
) {}
