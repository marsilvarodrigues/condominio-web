package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.mapper.EnderecoMapper;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioEnderecoDTO;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.EntityManager;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for converting between {@link Condominio} entities and their DTO representations.
 */
@Mapper(componentModel = "spring",
        uses = {EnderecoMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class CondominioMapper {

    protected EntityManager em;

    @Autowired(required = false)
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    /**
     * Converts a {@link Condominio} entity to its full DTO representation.
     *
     * @param condominio source entity
     * @return mapped DTO
     */
    public abstract CondominioDTO toDTO(Condominio condominio);

    /**
     * Creates a new {@link Condominio} entity from a creation payload.
     * The address state is resolved via a JPA reference, avoiding unnecessary database lookups at mapping time.
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
    public abstract Condominio toEntity(CreateCondominioDTO dto);

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
    public abstract void updateEntity(@MappingTarget Condominio condominio, CondominioDTO dto);

    /**
     * Converts a {@link CreateCondominioEnderecoDTO} to the {@link Endereco} embeddable,
     * resolving the estado via a JPA reference instead of a hollow entity.
     */
    @Named("createEnderecoToEmbeddable")
    protected Endereco createEnderecoToEmbeddable(CreateCondominioEnderecoDTO dto) {
        if (dto == null) return null;
        return Endereco.builder()
                .logradouro(dto.logradouro())
                .cep(dto.cep())
                .cidade(dto.cidade())
                .estado(em.getReference(Estado.class, dto.estado()))
                .build();
    }
}
