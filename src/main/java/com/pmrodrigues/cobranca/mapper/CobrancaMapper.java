package com.pmrodrigues.cobranca.mapper;

import com.pmrodrigues.cobranca.dto.CobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaResumoDTO;
import com.pmrodrigues.cobranca.model.Cobranca;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting {@link Cobranca} entities to their DTO representations.
 *
 * <p>The {@code moradorNome} and {@code moradorEmail} fields in {@link CobrancaDTO} are not
 * populated by this mapper — they must be enriched by the service layer after mapping.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CobrancaMapper {

    /**
     * Maps a {@link Cobranca} entity to its full {@link CobrancaDTO}.
     * Resident name and email fields are left null — the service enriches them separately.
     *
     * @param cobranca source entity
     * @return full DTO
     */
    @Mapping(source = "apartamento.id", target = "apartamentoId")
    @Mapping(source = "apartamento.numero", target = "apartamentoNumero")
    @Mapping(source = "apartamento.bloco.bloco", target = "blocoNome")
    @Mapping(target = "moradorNome", ignore = true)
    @Mapping(target = "moradorEmail", ignore = true)
    CobrancaDTO toDTO(Cobranca cobranca);

    /**
     * Maps a {@link Cobranca} entity to its lightweight {@link CobrancaResumoDTO}.
     *
     * @param cobranca source entity
     * @return summary DTO
     */
    @Mapping(source = "createdAt", target = "criadaEm")
    CobrancaResumoDTO toResumoDTO(Cobranca cobranca);
}
