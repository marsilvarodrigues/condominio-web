package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.CreatePlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasFilterDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasNodeDTO;
import com.pmrodrigues.financeiro.mapper.PlanoContasMapper;
import com.pmrodrigues.financeiro.model.PlanoContas;
import com.pmrodrigues.financeiro.repository.PlanoContasRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.financeiro.specification.PlanoContasSpecification.hasPai;
import static com.pmrodrigues.financeiro.specification.PlanoContasSpecification.hasTipo;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Application service for managing the chart of accounts, with tree navigation and tenant-scoped caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanoContasService {

    static final String CACHE = "plano-contas";
    static final String TENANT_KEY = "T(com.pmrodrigues.commons.tenant.TenantContext).getCondominioId()";

    private final PlanoContasRepository repository;
    private final PlanoContasMapper mapper;

    /**
     * Returns PlanoContas nodes matching the supplied filter criteria.
     *
     * @param dto filter with optional tipo and paiId
     * @return matching PlanoContas DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "planocontas.service.filterBy", description = "Filter plano de contas")
    @Cacheable(value = CACHE, key = TENANT_KEY + " + ':filter:' + #dto.tipo() + ':' + #dto.paiId()")
    public List<PlanoContasDTO> filterBy(PlanoContasFilterDTO dto) {
        log.info("Filtering plano de contas: tipo={}, paiId={}", dto.tipo(), dto.paiId());
        var result = repository.findAll(Specification.allOf(hasTipo(dto.tipo()), hasPai(dto.paiId())))
                .stream().map(mapper::toDTO).toList();
        log.info("Filter plano de contas: {} results", result.size());
        return result;
    }

    /**
     * Returns the full chart-of-accounts tree starting from root nodes.
     *
     * @return list of root-level nodes, each carrying their subtrees
     */
    @Transactional(readOnly = true)
    @Timed(value = "planocontas.service.getArvore", description = "Get plano de contas tree")
    @Cacheable(value = CACHE, key = TENANT_KEY + " + ':arvore'")
    public List<PlanoContasNodeDTO> getArvore() {
        log.info("Building plano de contas tree");
        var roots = repository.findAllByPaiIsNull();
        var tree = roots.stream().map(this::toNode).toList();
        log.info("Plano de contas tree: {} root nodes", tree.size());
        return tree;
    }

    /**
     * Looks up a single PlanoContas node by its primary key.
     */
    @Transactional(readOnly = true)
    @Timed(value = "planocontas.service.findById", description = "Find plano de contas by id")
    @Cacheable(value = CACHE, key = TENANT_KEY + " + ':' + #id", unless = "#result == null")
    public Optional<PlanoContasDTO> findById(Long id) {
        log.info("Looking up plano de contas by id: {}", id);
        var result = repository.findById(id).map(mapper::toDTO);
        log.info("PlanoContas lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Persists a new PlanoContas node and evicts the tenant cache.
     */
    @Transactional
    @Timed(value = "planocontas.service.create", description = "Create plano de contas")
    @CacheEvict(value = CACHE, allEntries = true)
    public PlanoContasDTO create(CreatePlanoContasDTO dto) {
        log.info("Creating plano de contas: codigo={}, tipo={}", dto.codigo(), dto.tipo());
        var saved = repository.save(mapper.toEntity(dto));
        log.info("PlanoContas created with id: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Updates an existing PlanoContas node and evicts the tenant cache.
     *
     * @throws ResponseStatusException 404 if not found
     */
    @Transactional
    @Timed(value = "planocontas.service.update", description = "Update plano de contas")
    @CacheEvict(value = CACHE, allEntries = true)
    public PlanoContasDTO update(PlanoContasDTO dto) {
        log.info("Updating plano de contas with id: {}", dto.id());
        var entity = repository.findById(dto.id()).orElseThrow(() -> notFound("PlanoContas", dto.id()));
        mapper.updateEntity(entity, dto);
        var saved = repository.save(entity);
        log.info("PlanoContas updated: {}", dto.id());
        return mapper.toDTO(saved);
    }

    /**
     * Soft-deletes a PlanoContas node, refusing if it has active children.
     *
     * @throws ResponseStatusException 404 if not found; 409 if node has children
     */
    @Transactional
    @Timed(value = "planocontas.service.delete", description = "Delete plano de contas")
    @CacheEvict(value = CACHE, allEntries = true)
    public void delete(Long id) {
        log.info("Deleting plano de contas with id: {}", id);
        var entity = repository.findById(id).orElseThrow(() -> notFound("PlanoContas", id));
        if (repository.existsByPaiId(id)) {
            log.error("Cannot delete plano de contas id={}: has children", id);
            throw new ResponseStatusException(CONFLICT, "PlanoContas has children and cannot be deleted");
        }
        repository.delete(entity);
        log.info("PlanoContas soft-deleted: {}", id);
    }

    /**
     * Soft-deletes all PlanoContas belonging to the given condominio.
     *
     * @param condominioId the condominio whose accounts must be soft-deleted
     */
    @Transactional
    @Timed(value = "planocontas.service.softDeleteByCondominioId", description = "Cascade soft-delete plano de contas by condominio")
    @CacheEvict(value = CACHE, allEntries = true)
    public void softDeleteByCondominioId(Long condominioId) {
        log.info("Cascade soft-deleting plano de contas for condominioId={}", condominioId);
        repository.softDeleteByCondominioId(condominioId);
    }

    private PlanoContasNodeDTO toNode(PlanoContas pc) {
        var filhosDTO = pc.getFilhos().stream().map(this::toNode).toList();
        return new PlanoContasNodeDTO(pc.getId(), pc.getCodigo(), pc.getDescricao(), pc.getTipo(),
                pc.getTipoRateio(), pc.getEscopoRateio(),
                pc.getPai() != null ? pc.getPai().getId() : null, filhosDTO);
    }
}
