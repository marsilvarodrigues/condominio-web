package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.model.Bloco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** MapStruct mapper for converting between {@link Bloco} entities and their DTO representations. */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BlocoMapper {

  /**
   * Converts a {@link Bloco} entity to its full DTO representation.
   *
   * @param bloco source entity
   * @return mapped DTO
   */
  BlocoDTO toDTO(Bloco bloco);

  /**
   * Creates a new {@link Bloco} entity from a creation payload.
   *
   * @param dto creation payload
   * @return new entity (id, audit fields, and deleted flag are ignored)
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  Bloco toEntity(CreateBlocoDTO dto);

  /**
   * Applies non-null DTO fields onto an existing entity, ignoring id, soft-delete, and audit
   * fields.
   *
   * @param bloco target entity to update
   * @param dto source data
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  void updateEntity(@MappingTarget Bloco bloco, BlocoDTO dto);
}
