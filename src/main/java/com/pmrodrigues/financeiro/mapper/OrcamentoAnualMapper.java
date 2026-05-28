package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CreateOrcamentoAnualDTO;
import com.pmrodrigues.financeiro.dto.ItemOrcamentoDTO;
import com.pmrodrigues.financeiro.dto.OrcamentoAnualDTO;
import com.pmrodrigues.financeiro.model.ItemOrcamento;
import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.PlanoContas;
import jakarta.persistence.EntityManager;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MapStruct mapper for OrcamentoAnual and ItemOrcamento conversions.
 *
 * <p>Abstract class required because {@code planoContasFromId} uses
 * {@code EntityManager.getReference()} to obtain a JPA proxy, which cannot be
 * injected via the standard MapStruct {@code uses} mechanism.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class OrcamentoAnualMapper {

    protected EntityManager em;

    @Autowired(required = false)
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    /**
     * Maps OrcamentoAnual to its full DTO including items.
     */
    public abstract OrcamentoAnualDTO toDTO(OrcamentoAnual entity);

    /**
     * Maps a CreateOrcamentoAnualDTO to a new OrcamentoAnual entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "taxaEstimadaUnidade", ignore = true)
    @Mapping(target = "itens", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    public abstract OrcamentoAnual toEntity(CreateOrcamentoAnualDTO dto);

    /**
     * Maps an ItemOrcamento to its DTO.
     */
    @Mapping(target = "planoContasId", source = "planoContas.id")
    @Mapping(target = "planoContasDescricao", source = "planoContas.descricao")
    @Mapping(target = "tipoConta", source = "planoContas.tipo")
    public abstract ItemOrcamentoDTO toItemDTO(ItemOrcamento item);

    protected PlanoContas planoContasFromId(Long id) {
        if (id == null) return null;
        return em.getReference(PlanoContas.class, id);
    }
}
