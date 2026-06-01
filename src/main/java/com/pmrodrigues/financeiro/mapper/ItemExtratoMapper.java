package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.ItemExtratoComOrcamentoResponse;
import com.pmrodrigues.financeiro.dto.ItemOrcamentoResumoResponse;
import com.pmrodrigues.financeiro.model.ItemExtrato;
import com.pmrodrigues.financeiro.model.ItemOrcamento;
import org.mapstruct.*;

/**
 * MapStruct mapper converting {@link ItemExtrato} entities to their response DTOs.
 *
 * <p>The nested {@link ItemOrcamento} association is mapped via {@link #toResumoResponse},
 * which MapStruct selects automatically when encountering the matching source/target types.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ItemExtratoMapper {

    /**
     * Converts a {@link ItemExtrato} entity to its full response representation,
     * including the optional budget line item association.
     *
     * @param entity the statement item entity
     * @return the response DTO, with {@code itemOrcamento} set to {@code null} if not associated
     */
    ItemExtratoComOrcamentoResponse toDTO(ItemExtrato entity);

    /**
     * Converts a {@link ItemOrcamento} to a compact summary response,
     * flattening the {@link com.pmrodrigues.financeiro.model.PlanoContas} reference.
     *
     * @param entity the budget line item entity; may be {@code null}
     * @return the summary response, or {@code null} if {@code entity} is {@code null}
     */
    @Mapping(target = "codigoPlanoContas", source = "planoContas.codigo")
    @Mapping(target = "descricaoPlanoContas", source = "planoContas.descricao")
    ItemOrcamentoResumoResponse toResumoResponse(ItemOrcamento entity);
}
