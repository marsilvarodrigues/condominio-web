package com.pmrodrigues.commons.mapper;

import com.pmrodrigues.commons.dto.EnderecoDTO;
import com.pmrodrigues.commons.embeddable.Endereco;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link Endereco} embeddables and {@link EnderecoDTO} records.
 */
@Mapper(componentModel = "spring",
        uses = {EstadoMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EnderecoMapper {

    /**
     * Converts an {@link Endereco} embeddable to its DTO representation.
     *
     * @return the mapped {@link EnderecoDTO}
     */
    EnderecoDTO toDTO(Endereco endereco);

    /**
     * Converts an {@link EnderecoDTO} to its embeddable representation.
     *
     * @return the mapped {@link Endereco}
     */
    Endereco toEntity(EnderecoDTO dto);
}