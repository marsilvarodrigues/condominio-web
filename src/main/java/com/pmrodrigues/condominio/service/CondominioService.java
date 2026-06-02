package com.pmrodrigues.condominio.service;

import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CondominioFilterDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.mapper.CondominioMapper;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import com.pmrodrigues.condominio.repository.BlocoRepository;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.financeiro.service.ContaBancariaService;
import com.pmrodrigues.financeiro.service.FundoReservaService;
import com.pmrodrigues.financeiro.service.LancamentoBancarioService;
import com.pmrodrigues.financeiro.service.OrcamentoAnualService;
import com.pmrodrigues.financeiro.service.PlanoContasService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.condominio.specification.CondominioSpecification.hasCnpj;
import static com.pmrodrigues.condominio.specification.CondominioSpecification.hasNome;
import org.springframework.data.jpa.domain.Specification;

/**
 * Application service for managing condominios, with result caching and soft-delete support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CondominioService {

    private final CondominioRepository repository;
    private final CondominioMapper mapper;
    private final BlocoRepository blocoRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final PlanoContasService planoContasService;
    private final FundoReservaService fundoReservaService;
    private final OrcamentoAnualService orcamentoAnualService;
    private final ContaBancariaService contaBancariaService;
    private final LancamentoBancarioService lancamentoBancarioService;

    /**
     * Returns condominios matching the supplied filter criteria.
     *
     * @param dto filter containing optional nome substring and exact cnpj; absent fields are ignored
     * @return matching condominio DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "condominio.service.filterBy", description = "Filter condominios")
    @Cacheable(value = "condominios", key = "'filter:' + #dto.nome() + ':' + #dto.cnpj()")
    public List<CondominioDTO> filterBy(CondominioFilterDTO dto) {
        log.info("Filtering condominios: nome={}, cnpj={}", dto.nome(), dto.cnpj());
        var result = repository.findAll(Specification.allOf(hasNome(dto.nome()), hasCnpj(dto.cnpj())))
                .stream().map(mapper::toDTO).toList();
        log.info("Filter condominios: {} results", result.size());
        return result;
    }

    /**
     * Looks up a single condominio by its primary key.
     *
     * @param id condominio primary key
     * @return the DTO wrapped in an Optional, or empty if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "condominio.service.findById", description = "Find condominio by id")
    @Cacheable(value = "condominios", key = "#id", unless = "#result == null")
    public Optional<CondominioDTO> findById(Long id) {
        log.info("Looking up condominio by id: {}", id);
        var result = repository.findById(id).map(mapper::toDTO);
        log.info("Condominio lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Looks up a single condominio entity (not DTO) by its primary key.
     * Use this when you need a JPA entity reference for association management.
     *
     * @param id condominio primary key
     * @return the entity wrapped in an Optional, or empty if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "condominio.service.findEntityById", description = "Find condominio entity by id")
    public Optional<Condominio> findEntityById(Long id) {
        log.info("Looking up condominio entity by id: {}", id);
        var result = repository.findById(id);
        log.info("Condominio entity lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Convenience lookup that finds a condominio by its exact CNPJ.
     *
     * @param cnpj the CNPJ to search for
     * @return the DTO wrapped in an Optional, or empty if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "condominio.service.findByCnpj", description = "Find condominio by CNPJ")
    @Cacheable(value = "condominios", key = "#cnpj", unless = "#result == null")
    public Optional<CondominioDTO> findByCnpj(String cnpj) {
        log.info("Looking up condominio by cnpj: {}", cnpj);
        var result = repository.findByCnpj(cnpj).map(mapper::toDTO);
        log.info("Condominio lookup by cnpj {}: {}", cnpj, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Persists a new condominio and evicts the cache.
     *
     * @param dto creation payload (nome, cnpj, email, endereco)
     * @return the persisted condominio as a DTO
     */
    @Transactional
    @Timed(value = "condominio.service.create", description = "Create condominio")
    @CacheEvict(value = "condominios", allEntries = true)
    public CondominioDTO create(CreateCondominioDTO dto) {
        log.info("Creating condominio with cnpj: {}", dto.cnpj());
        var saved = repository.save(mapper.toEntity(dto));
        log.info("Condominio created successfully with id: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Applies changes from the DTO to the existing condominio entity and saves it.
     *
     * @param dto updated condominio data including the target id
     * @return the updated condominio as a DTO
     * @throws org.springframework.web.server.ResponseStatusException if no condominio exists with the given id
     */
    @Transactional
    @Timed(value = "condominio.service.update", description = "Update condominio")
    @CacheEvict(value = "condominios", allEntries = true)
    public CondominioDTO update(CondominioDTO dto) {
        log.info("Updating condominio with id: {}", dto.id());
        var entity = repository.findById(dto.id())
                .orElseThrow(() -> notFound("Condominio", dto.id()));
        mapper.updateEntity(entity, dto);
        var saved = repository.save(entity);
        log.info("Condominio updated successfully: {}", dto.id());
        return mapper.toDTO(saved);
    }

    /**
     * Soft-deletes the condominio with the given id, cascading to all its blocos and apartamentos.
     *
     * @param id primary key of the condominio to delete
     * @throws org.springframework.web.server.ResponseStatusException if no condominio exists with the given id
     */
    @Transactional
    @Timed(value = "condominio.service.delete", description = "Delete condominio")
    @Caching(evict = {
            @CacheEvict(value = "condominios",       allEntries = true),
            @CacheEvict(value = "blocos",            allEntries = true),
            @CacheEvict(value = "apartamentos",      allEntries = true),
            @CacheEvict(value = "plano-contas",      allEntries = true),
            @CacheEvict(value = "fundo-reserva",     allEntries = true),
            @CacheEvict(value = "orcamentos",        allEntries = true),
            @CacheEvict(value = "contas-bancarias",  allEntries = true)
    })
    public void delete(Long id) {
        log.info("Deleting condominio with id: {}", id);
        var entity = repository.findById(id)
                .orElseThrow(() -> notFound("Condominio", id));
        log.info("Cascade soft-deleting blocos and apartamentos for condominioId: {}", id);
        apartamentoRepository.softDeleteByCondominioId(id);
        blocoRepository.softDeleteByCondominioId(id);
        log.info("Cascade soft-deleting financeiro data for condominioId: {}", id);
        planoContasService.softDeleteByCondominioId(id);
        fundoReservaService.softDeleteByCondominioId(id);
        orcamentoAnualService.softDeleteByCondominioId(id);
        contaBancariaService.softDeleteByCondominioId(id);
        lancamentoBancarioService.softDeleteByCondominioId(id);
        repository.delete(entity);
        log.info("Condominio soft-deleted successfully: {}", id);
    }
}
