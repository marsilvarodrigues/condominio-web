package com.pmrodrigues.commons.mapper;

import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.model.Estado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link Estado} entities and {@link EstadoDTO} records.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EstadoMapper {

  /**
   * Converts an {@link Estado} entity to its DTO representation.
   *
   * @return the mapped {@link EstadoDTO}
   */
  EstadoDTO toDTO(Estado estado);

  /**
   * Converts an {@link EstadoDTO} to a new {@link Estado} entity.
   *
   * @return the mapped {@link Estado}
   */
  Estado toEntity(EstadoDTO dto);

  /**
   * Updates an existing {@link Estado} entity in-place from the given DTO, ignoring the {@code id}
   * field.
   *
   * @param estado the entity to update
   * @param dto source values to apply
   */
  @Mapping(target = "id", ignore = true)
  void updateEntity(@MappingTarget Estado estado, EstadoDTO dto);
}
