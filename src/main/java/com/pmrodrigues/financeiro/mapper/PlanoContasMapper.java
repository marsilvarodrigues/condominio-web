package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CreatePlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasDTO;
import com.pmrodrigues.financeiro.model.PlanoContas;
import jakarta.persistence.EntityManager;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for PlanoContas conversions.
 *
 * <p>Abstract class required because {@code paiFromId} uses {@code EntityManager.getReference()} to
 * obtain a JPA proxy for the parent node, which cannot be injected via the standard MapStruct
 * {@code uses} mechanism. Setter injection with {@code required = false} prevents injection
 * failures in cache-only test contexts where no EntityManager bean is present.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class PlanoContasMapper {

  protected EntityManager em;

  @Autowired(required = false)
  public void setEntityManager(EntityManager em) {
    this.em = em;
  }

  /** Maps a PlanoContas entity to its flat DTO representation. */
  @Mapping(target = "paiId", source = "pai.id")
  public abstract PlanoContasDTO toDTO(PlanoContas entity);

  /** Maps a CreatePlanoContasDTO to a new PlanoContas entity. */
  @Mapping(target = "pai", source = "paiId")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "filhos", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  public abstract PlanoContas toEntity(CreatePlanoContasDTO dto);

  /** Applies changes from a PlanoContasDTO to an existing entity. */
  @Mapping(target = "pai", source = "paiId")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "filhos", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  public abstract void updateEntity(@MappingTarget PlanoContas entity, PlanoContasDTO dto);

  protected PlanoContas paiFromId(Long id) {
    if (id == null) return null;
    return em.getReference(PlanoContas.class, id);
  }
}
