package com.pmrodrigues.cobranca.dto;

import com.pmrodrigues.cobranca.model.StatusCobranca;
import java.time.LocalDate;

/**
 * Filter criteria for listing charges. All fields are optional — absent values are ignored.
 *
 * @param apartamentoId restrict to charges for a specific apartment
 * @param status restrict to a specific lifecycle status
 * @param vencimentoDe lower bound of the due-date range (inclusive)
 * @param vencimentoAte upper bound of the due-date range (inclusive)
 * @param emailEnviado filter by whether the billing email was sent
 */
public record CobrancaFilterDTO(
    Long apartamentoId,
    StatusCobranca status,
    LocalDate vencimentoDe,
    LocalDate vencimentoAte,
    Boolean emailEnviado) {}
