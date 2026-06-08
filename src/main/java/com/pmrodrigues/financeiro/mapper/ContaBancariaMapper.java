package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.ContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.CreateContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.UpdateContaBancariaDTO;
import com.pmrodrigues.financeiro.model.ContaBancaria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** MapStruct mapper for converting between {@link ContaBancaria} entities and their DTOs. */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ContaBancariaMapper {

  /** Converts a {@link ContaBancaria} entity to its DTO, flattening banco fields. */
  @Mapping(target = "bancoId", source = "banco.id")
  @Mapping(target = "bancoNome", source = "banco.nome")
  @Mapping(target = "bancoCodigo", source = "banco.codigo")
  ContaBancariaDTO toDTO(ContaBancaria entity);

  /**
   * Creates a new {@link ContaBancaria} entity from a creation payload. The {@code banco} and
   * {@code condominio} associations are set by the service.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "banco", ignore = true)
  @Mapping(target = "saldoContabil", ignore = true)
  @Mapping(target = "ativa", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  ContaBancaria toEntity(CreateContaBancariaDTO dto);

  /**
   * Updates mutable fields of an existing {@link ContaBancaria}, ignoring tenant and audit fields.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "banco", ignore = true)
  @Mapping(target = "tipo", ignore = true)
  @Mapping(target = "saldoContabil", ignore = true)
  @Mapping(target = "ativa", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  void updateEntity(@MappingTarget ContaBancaria entity, UpdateContaBancariaDTO dto);
}
