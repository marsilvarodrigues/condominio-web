package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CreateDespesaDTO;
import com.pmrodrigues.financeiro.dto.DespesaDTO;
import com.pmrodrigues.financeiro.model.Despesa;
import org.mapstruct.*;

/** MapStruct mapper for {@link Despesa} conversions. */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DespesaMapper {

  /** Maps a {@link Despesa} entity to its DTO representation. */
  @Mapping(target = "grupoDespesaId", source = "grupoDespesa.id")
  @Mapping(target = "grupoDespesaNome", source = "grupoDespesa.nome")
  DespesaDTO toDTO(Despesa entity);

  /**
   * Maps a {@link CreateDespesaDTO} to a new entity. The {@code grupoDespesa} association must be
   * set by the service after mapping.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "grupoDespesa", ignore = true)
  @Mapping(target = "rateioStatus", ignore = true)
  @Mapping(target = "dataUltimoRateio", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  Despesa toEntity(CreateDespesaDTO dto);
}
