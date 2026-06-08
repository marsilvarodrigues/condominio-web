package com.pmrodrigues.commons.dto;

/** Read-only representation of a {@link com.pmrodrigues.commons.model.Banco}. */
public record BancoDTO(Long id, String codigo, String nome, String ispb) {}
