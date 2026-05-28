package com.pmrodrigues.condominio.dto;

/**
 * Filter criteria for querying condominium complexes by name or CNPJ.
 */
public record CondominioFilterDTO(String nome, String cnpj) {}
