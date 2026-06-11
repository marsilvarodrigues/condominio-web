package com.pmrodrigues.morador.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only projection of a single occupancy history record. */
public record HistoricoOcupacaoDTO(
    Long id,
    Long apartamentoId,
    Long pessoaId,
    String nomeMorador,
    String emailMorador,
    String cpfMorador,
    LocalDate dataEntrada,
    LocalDate dataSaida,
    LocalDateTime criadoEm) {}
