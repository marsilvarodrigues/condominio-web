package com.pmrodrigues.commons.mapper;

import com.pmrodrigues.commons.dto.BancoDTO;
import com.pmrodrigues.commons.dto.CreateBancoDTO;
import com.pmrodrigues.commons.dto.UpdateBancoDTO;
import com.pmrodrigues.commons.model.Banco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link Banco} entities and their DTOs.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BancoMapper {

    /**
     * Converts a {@link Banco} entity to its DTO.
     */
    BancoDTO toDTO(Banco banco);

    /**
     * Creates a new {@link Banco} entity from a creation payload.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Banco toEntity(CreateBancoDTO dto);

    /**
     * Updates mutable fields of an existing {@link Banco}, ignoring id and audit fields.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(@MappingTarget Banco banco, UpdateBancoDTO dto);
}
