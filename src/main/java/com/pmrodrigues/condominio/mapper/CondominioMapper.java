package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.mapper.EnderecoMapper;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioEnderecoDTO;
import com.pmrodrigues.condominio.model.Condominio;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link Condominio} entities and their DTO representations.
 */
@Mapper(componentModel = "spring",
        uses = {EnderecoMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CondominioMapper {

    /**
     * Converts a {@link Condominio} entity to its full DTO representation.
     *
     * @param condominio source entity
     * @return mapped DTO
     */
    CondominioDTO toDTO(Condominio condominio);

    /**
     * Creates a new {@link Condominio} entity from a creation payload.
     * The address state is resolved from its numeric identifier using a JPA reference,
     * avoiding unnecessary database lookups at mapping time.
     *
     * @param dto creation payload
     * @return new entity (audit fields and deleted flag are ignored)
     */
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "endereco", source = "endereco", qualifiedByName = "createEnderecoToEmbeddable")
    Condominio toEntity(CreateCondominioDTO dto);

    /**
     * Applies non-null DTO fields onto an existing entity, ignoring id, soft-delete, and audit fields.
     *
     * @param condominio target entity to update
     * @param dto source data
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(@MappingTarget Condominio condominio, CondominioDTO dto);

    /**
     * Converts a {@link CreateCondominioEnderecoDTO} (with estado as Long) to the {@link Endereco}
     * embeddable, creating a JPA entity reference for Estado using only its id.
     */
    @Named("createEnderecoToEmbeddable")
    default Endereco createEnderecoToEmbeddable(CreateCondominioEnderecoDTO dto) {
        if (dto == null) {
            return null;
        }
        var estado = new Estado();
        estado.setId(dto.estado());
        return Endereco.builder()
                .logradouro(dto.logradouro())
                .cep(dto.cep())
                .cidade(dto.cidade())
                .estado(estado)
                .build();
    }
}