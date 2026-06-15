package com.pmrodrigues.morador.mapper;

import com.pmrodrigues.morador.dto.HistoricoOcupacaoDTO;
import com.pmrodrigues.morador.model.HistoricoOcupacao;
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.model.Pessoa;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link HistoricoOcupacao} → {@link HistoricoOcupacaoDTO}.
 */
@Mapper(componentModel = "spring")
public interface HistoricoOcupacaoMapper {

  /**
   * Maps a single entity to its DTO representation.
   *
   * @param entity source entity
   * @return mapped DTO
   */
  @Mapping(target = "apartamentoId", source = "apartamento.id")
  @Mapping(target = "pessoaId", source = "pessoa.id")
  @Mapping(target = "nomeMorador", source = "pessoa.name")
  @Mapping(target = "emailMorador", source = "pessoa.email")
  @Mapping(target = "cpfMorador", source = "pessoa", qualifiedByName = "cpfFromPessoa")
  HistoricoOcupacaoDTO toDTO(HistoricoOcupacao entity);

  /**
   * Maps a list of entities to DTOs.
   *
   * @param entities source list
   * @return list of DTOs; empty list when input is empty
   */
  List<HistoricoOcupacaoDTO> toDTOList(List<HistoricoOcupacao> entities);

  /** Extracts CPF from a {@link Morador}; returns null for other Pessoa subtypes. */
  @Named("cpfFromPessoa")
  default String cpfFromPessoa(Pessoa pessoa) {
    if (pessoa instanceof Morador m) return m.getCpf();
    return null;
  }
}
