package com.pmrodrigues.condominio.dto;

/**
 * Lightweight DTO summarising an {@code Apartamento} for use in nested representations (e.g. inside
 * a {@code ProprietarioDTO}).
 */
public record ApartamentoResumoDTO(Long id, String numero, String blocoNome, Long condominioId) {}
