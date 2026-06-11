package com.pmrodrigues.morador.mapper;

import com.pmrodrigues.morador.dto.HistoricoOcupacaoDTO;
import com.pmrodrigues.morador.model.HistoricoOcupacao;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link HistoricoOcupacao} → {@link HistoricoOcupacaoDTO}. All fields share
 * the same names so no explicit {@code @Mapping} annotations are needed.
 */
@Mapper(componentModel = "spring")
public interface HistoricoOcupacaoMapper {

  /**
   * Maps a single entity to its DTO representation.
   *
   * @param entity source entity
   * @return mapped DTO
   */
  HistoricoOcupacaoDTO toDTO(HistoricoOcupacao entity);

  /**
   * Maps a list of entities to DTOs.
   *
   * @param entities source list
   * @return list of DTOs; empty list when input is empty
   */
  List<HistoricoOcupacaoDTO> toDTOList(List<HistoricoOcupacao> entities);
}
