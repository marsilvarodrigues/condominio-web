package com.pmrodrigues.commons.dto;

/**
 * Filter parameters for querying {@link com.pmrodrigues.commons.model.Banco} entries. All fields
 * are optional; absent values are ignored.
 */
public record BancoFilterDTO(String codigo, String nome) {}
