package com.pmrodrigues.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Full representation of a {@code CoeficienteRateio} record. */
public record CoeficienteRateioDTO(
    Long id,
    Long grupoDespesaId,
    Long apartamentoId,
    String apartamentoNumero,
    BigDecimal coeficiente,
    BigDecimal areaM2,
    BigDecimal consumoM3,
    LocalDate vigencia) {}
