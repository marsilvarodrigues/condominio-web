package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CoeficienteRateioDTO;
import com.pmrodrigues.financeiro.dto.CreateCoeficienteRateioDTO;
import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import org.mapstruct.*;

/** MapStruct mapper for {@link CoeficienteRateio} conversions. */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CoeficienteRateioMapper {

  /** Maps a {@link CoeficienteRateio} entity to its DTO representation. */
  @Mapping(target = "grupoDespesaId", source = "grupoDespesa.id")
  @Mapping(target = "apartamentoId", source = "apartamento.id")
  @Mapping(target = "apartamentoNumero", source = "apartamento.numero")
  CoeficienteRateioDTO toDTO(CoeficienteRateio entity);

  /**
   * Maps a {@link CreateCoeficienteRateioDTO} to a new entity. The {@code grupoDespesa} association
   * must be set by the service after mapping.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "grupoDespesa", ignore = true)
  @Mapping(target = "apartamento", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  CoeficienteRateio toEntity(CreateCoeficienteRateioDTO dto);
}
