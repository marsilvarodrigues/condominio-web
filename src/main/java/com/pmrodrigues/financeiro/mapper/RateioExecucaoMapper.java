package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.RateioExecucaoDTO;
import com.pmrodrigues.financeiro.model.RateioExecucao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** MapStruct mapper for {@link RateioExecucao} read-only conversions. */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RateioExecucaoMapper {

  /** Maps a {@link RateioExecucao} entity to its DTO representation. */
  @Mapping(target = "despesaId", source = "despesa.id")
  @Mapping(target = "despesaDescricao", source = "despesa.descricao")
  RateioExecucaoDTO toDTO(RateioExecucao entity);
}
