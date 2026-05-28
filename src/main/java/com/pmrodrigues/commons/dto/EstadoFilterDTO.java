package com.pmrodrigues.commons.dto;

/**
 * Filter criteria for querying estados by name and/or UF abbreviation.
 */
public record EstadoFilterDTO(String nome, String uf) {}
