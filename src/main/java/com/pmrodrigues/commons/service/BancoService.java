package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.dto.BancoDTO;
import com.pmrodrigues.commons.dto.BancoFilterDTO;
import com.pmrodrigues.commons.dto.CreateBancoDTO;
import com.pmrodrigues.commons.dto.UpdateBancoDTO;
import com.pmrodrigues.commons.mapper.BancoMapper;
import com.pmrodrigues.commons.repository.BancoRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.pmrodrigues.commons.specification.BancoSpecification.hasCodigo;
import static com.pmrodrigues.commons.specification.BancoSpecification.hasNome;
import static com.pmrodrigues.commons.util.Exceptions.notFound;

/**
 * Service providing CRUD operations for {@link com.pmrodrigues.commons.model.Banco} with caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BancoService {

    static final String CACHE = "bancos";

    private final BancoRepository repository;
    private final BancoMapper mapper;

    /**
     * Returns bancos matching the supplied filter criteria.
     *
     * @param dto filter containing optional codigo prefix and nome substring
     * @return matching banco DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "banco.service.filterBy", description = "Filter bancos")
    @Cacheable(value = CACHE, key = "'filter:' + #dto.codigo() + ':' + #dto.nome()")
    public List<BancoDTO> filterBy(BancoFilterDTO dto) {
        log.info("Filtering bancos: codigo={}, nome={}", dto.codigo(), dto.nome());
        var result = repository.findAll(Specification.allOf(hasCodigo(dto.codigo()), hasNome(dto.nome())))
                .stream().map(mapper::toDTO).toList();
        log.info("Filter bancos: {} results", result.size());
        return result;
    }

    /**
     * Looks up a single banco by its primary key.
     *
     * @param id the banco identifier
     * @return the found DTO, or empty if none exists
     */
    @Transactional(readOnly = true)
    @Timed(value = "banco.service.findById", description = "Find banco by id")
    @Cacheable(value = CACHE, key = "#id", unless = "#result == null")
    public Optional<BancoDTO> findById(Long id) {
        log.info("Looking up banco by id: {}", id);
        var result = repository.findById(id).map(mapper::toDTO);
        log.info("Banco lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Persists a new banco and evicts the bancos cache.
     *
     * @param dto data for the new banco
     * @return the persisted banco as a DTO
     */
    @Transactional
    @Timed(value = "banco.service.create", description = "Create banco")
    @CacheEvict(value = CACHE, allEntries = true)
    public BancoDTO create(CreateBancoDTO dto) {
        log.info("Creating banco with codigo: {}", dto.codigo());
        var saved = repository.save(mapper.toEntity(dto));
        log.info("Banco created successfully with id: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Updates an existing banco identified by {@code id} and evicts the bancos cache.
     *
     * @param id  the banco identifier
     * @param dto updated values
     * @return the updated banco as a DTO
     * @throws org.springframework.web.server.ResponseStatusException 404 if no banco exists with the given id
     */
    @Transactional
    @Timed(value = "banco.service.update", description = "Update banco")
    @CacheEvict(value = CACHE, allEntries = true)
    public BancoDTO update(Long id, UpdateBancoDTO dto) {
        log.info("Updating banco with id: {}", id);
        var entity = repository.findById(id).orElseThrow(() -> notFound("Banco", id));
        mapper.updateEntity(entity, dto);
        var saved = repository.save(entity);
        log.info("Banco updated successfully: {}", id);
        return mapper.toDTO(saved);
    }

    /**
     * Soft-deletes a banco by its primary key and evicts the bancos cache.
     *
     * @param id the banco identifier
     * @throws org.springframework.web.server.ResponseStatusException 404 if no banco exists with the given id
     */
    @Transactional
    @Timed(value = "banco.service.delete", description = "Delete banco")
    @CacheEvict(value = CACHE, allEntries = true)
    public void delete(Long id) {
        log.info("Deleting banco with id: {}", id);
        var entity = repository.findById(id).orElseThrow(() -> notFound("Banco", id));
        repository.delete(entity);
        log.info("Banco deleted successfully: {}", id);
    }
}
