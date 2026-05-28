package com.pmrodrigues.commons.dto;

/**
 * Data transfer object representing a Brazilian postal address.
 */
public record EnderecoDTO(String logradouro, String cep, String cidade, EstadoDTO estado) {
}