package com.pmrodrigues.condominio.dto;

/**
 * Filter criteria for querying apartments by bloco or unit number.
 */
public record ApartamentoFilterDTO(Long blocoId, String numero) {}
