package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link Apartamento} entities and their DTO representations.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ApartamentoMapper {

    /**
     * Converts an {@link Apartamento} entity to its full DTO representation.
     *
     * @param apartamento source entity
     * @return mapped DTO
     */
    ApartamentoDTO toDTO(Apartamento apartamento);

    /**
     * Creates a new {@link Apartamento} entity from a creation payload, resolving {@code blocoId} to a {@link Bloco}.
     *
     * @param dto creation payload
     * @return new entity (id, audit fields, and deleted flag are ignored)
     */
    @Mapping(target = "bloco", source = "blocoId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Apartamento toEntity(CreateApartamentoDTO dto);

    /**
     * Applies non-null DTO fields onto an existing entity, ignoring id, bloco, soft-delete, and audit fields.
     *
     * @param apartamento target entity to update
     * @param dto source data
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bloco", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(@MappingTarget Apartamento apartamento, ApartamentoDTO dto);

    /**
     * Constructs a proxy {@link Bloco} with only its identifier set, used by MapStruct for FK resolution.
     *
     * @param id bloco primary key; returns {@code null} if {@code id} is {@code null}
     * @return shallow {@link Bloco} instance
     */
    default Bloco blocoFromId(Long id) {
        if (id == null) return null;
        var bloco = new Bloco();
        bloco.setId(id);
        return bloco;
    }
}
