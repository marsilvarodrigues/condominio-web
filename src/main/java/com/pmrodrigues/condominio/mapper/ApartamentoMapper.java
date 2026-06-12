package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.morador.model.Pessoa;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for converting between {@link Apartamento} entities and their DTO
 * representations.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class ApartamentoMapper {

  protected EntityManager em;

  @Autowired(required = false)
  public void setEntityManager(EntityManager em) {
    this.em = em;
  }

  /**
   * Converts an {@link Apartamento} entity to its full DTO representation.
   *
   * @param apartamento source entity
   * @return mapped DTO
   */
  @Mapping(target = "blocoNome", source = "bloco.bloco")
  @Mapping(target = "blocoId", source = "bloco.id")
  @Mapping(
      target = "quantidadeMoradores",
      source = "moradores",
      qualifiedByName = "countMoradores")
  public abstract ApartamentoDTO toDTO(Apartamento apartamento);

  /**
   * Creates a new {@link Apartamento} entity from a creation payload, resolving {@code blocoId} to
   * a {@link Bloco}.
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
  @Mapping(target = "moradores", ignore = true)
  @Mapping(target = "proprietarios", ignore = true)
  @Mapping(target = "condominio", ignore = true)
  public abstract Apartamento toEntity(CreateApartamentoDTO dto);

  /**
   * Applies non-null DTO fields onto an existing entity, ignoring id, bloco, soft-delete, and audit
   * fields.
   *
   * @param apartamento target entity to update
   * @param dto source data
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "bloco", ignore = true)
  @Mapping(target = "condominio", ignore = true)
  @Mapping(target = "moradores", ignore = true)
  @Mapping(target = "proprietarios", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  public abstract void updateEntity(@MappingTarget Apartamento apartamento, ApartamentoDTO dto);

  /**
   * Returns the number of moradores in the list; null-safe.
   *
   * @param moradores morador list from the entity
   * @return count, 0 if null
   */
  @Named("countMoradores")
  protected int countMoradores(List<Pessoa> moradores) {
    return moradores == null ? 0 : moradores.size();
  }

  /**
   * Resolves a {@link Bloco} JPA reference by id; returns a Hibernate proxy managed within the
   * current session.
   *
   * @param id bloco primary key; returns {@code null} if {@code id} is {@code null}
   * @return JPA proxy for the given id
   */
  protected Bloco blocoFromId(Long id) {
    if (id == null) return null;
    return em.getReference(Bloco.class, id);
  }
}
