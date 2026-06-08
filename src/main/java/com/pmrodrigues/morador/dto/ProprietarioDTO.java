package com.pmrodrigues.morador.dto;

import com.pmrodrigues.condominio.dto.ApartamentoResumoDTO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read DTO for a {@code Proprietario} entity, including the list of owned apartments and
 * discriminated type (PROP_PF or PROP_PJ).
 */
public record ProprietarioDTO(
    Long id,
    String nome,
    /** Discriminator: PROP_PF | PROP_PJ */
    String tipo,
    String cpf,
    String cnpj,
    String razaoSocial,
    String email,
    String telefone,
    List<ApartamentoResumoDTO> apartamentos,
    Long userId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
