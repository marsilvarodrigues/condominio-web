package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CreateGrupoDespesaDTO;
import com.pmrodrigues.financeiro.dto.GrupoDespesaDTO;
import com.pmrodrigues.financeiro.model.GrupoDespesa;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link GrupoDespesa} conversions.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GrupoDespesaMapper {

    /**
     * Maps a {@link GrupoDespesa} entity to its DTO representation.
     */
    GrupoDespesaDTO toDTO(GrupoDespesa entity);

    /**
     * Maps a {@link CreateGrupoDespesaDTO} to a new entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    GrupoDespesa toEntity(CreateGrupoDespesaDTO dto);

    /**
     * Applies changes from a {@link GrupoDespesaDTO} to an existing entity, ignoring null values.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(@MappingTarget GrupoDespesa entity, GrupoDespesaDTO dto);
}
