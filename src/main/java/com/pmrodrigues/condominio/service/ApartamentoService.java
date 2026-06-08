package com.pmrodrigues.condominio.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.condominio.specification.ApartamentoSpecification.hasBlocoId;
import static com.pmrodrigues.condominio.specification.ApartamentoSpecification.hasNumero;

import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.ApartamentoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.mapper.ApartamentoMapper;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for managing apartamentos, with tenant-scoped caching and soft-delete
 * support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApartamentoService {

  static final String CACHE = "apartamentos";
  static final String TENANT_KEY =
      "T(com.pmrodrigues.commons.tenant.TenantContext).getCondominioId()";

  private final ApartamentoRepository repository;
  private final ApartamentoMapper mapper;

  /**
   * Returns apartamentos matching the supplied filter criteria.
   *
   * @param dto filter containing optional blocoId and numero; absent fields are ignored
   * @return matching apartamento DTOs
   */
  @Transactional(readOnly = true)
  @Timed(value = "apartamento.service.filterBy", description = "Filter apartamentos")
  @Cacheable(
      value = CACHE,
      key = TENANT_KEY + " + ':filter:' + #dto.blocoId() + ':' + #dto.numero()")
  public List<ApartamentoDTO> filterBy(ApartamentoFilterDTO dto) {
    log.info("Filtering apartamentos: blocoId={}, numero={}", dto.blocoId(), dto.numero());
    var result =
        repository
            .findAll(Specification.allOf(hasBlocoId(dto.blocoId()), hasNumero(dto.numero())))
            .stream()
            .map(mapper::toDTO)
            .toList();
    log.info("Filter apartamentos: {} results", result.size());
    return result;
  }

  /**
   * Looks up a single apartamento by its primary key.
   *
   * @param id apartamento primary key
   * @return the DTO wrapped in an Optional, or empty if not found
   */
  @Transactional(readOnly = true)
  @Timed(value = "apartamento.service.findById", description = "Find apartamento by id")
  @Cacheable(value = CACHE, key = TENANT_KEY + " + ':' + #id", unless = "#result == null")
  public Optional<ApartamentoDTO> findById(Long id) {
    log.info("Looking up apartamento by id: {}", id);
    var result = repository.findById(id).map(mapper::toDTO);
    log.info("Apartamento lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
    return result;
  }

  /**
   * Persists a new apartamento and evicts the tenant cache.
   *
   * @param dto creation payload with bloco FK and apartment number
   * @return the persisted apartamento as a DTO
   */
  @Transactional
  @Timed(value = "apartamento.service.create", description = "Create apartamento")
  @CacheEvict(value = CACHE, allEntries = true)
  public ApartamentoDTO create(CreateApartamentoDTO dto) {
    log.info("Creating apartamento: bloco={}, numero={}", dto.blocoId(), dto.numero());
    var saved = repository.save(mapper.toEntity(dto));
    log.info("Apartamento created successfully with id: {}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Applies changes from the DTO to the existing apartamento entity and saves it.
   *
   * @param dto updated apartamento data including the target id
   * @return the updated apartamento as a DTO
   * @throws org.springframework.web.server.ResponseStatusException if no apartamento exists with
   *     the given id
   */
  @Transactional
  @Timed(value = "apartamento.service.update", description = "Update apartamento")
  @CacheEvict(value = CACHE, allEntries = true)
  public ApartamentoDTO update(ApartamentoDTO dto) {
    log.info("Updating apartamento with id: {}", dto.id());
    var entity = repository.findById(dto.id()).orElseThrow(() -> notFound("Apartamento", dto.id()));
    mapper.updateEntity(entity, dto);
    var saved = repository.save(entity);
    log.info("Apartamento updated successfully: {}", dto.id());
    return mapper.toDTO(saved);
  }

  /**
   * Soft-deletes the apartamento with the given id.
   *
   * @param id primary key of the apartamento to delete
   * @throws org.springframework.web.server.ResponseStatusException if no apartamento exists with
   *     the given id
   */
  @Transactional
  @Timed(value = "apartamento.service.delete", description = "Delete apartamento")
  @CacheEvict(value = CACHE, allEntries = true)
  public void delete(Long id) {
    log.info("Deleting apartamento with id: {}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("Apartamento", id));
    repository.delete(entity);
    log.info("Apartamento soft-deleted successfully: {}", id);
  }

  /**
   * Returns the raw {@link com.pmrodrigues.condominio.model.Apartamento} entity by id. Intended for
   * use by other services that need to associate the entity (e.g. rateio module).
   *
   * @param id the apartamento primary key
   * @return an Optional containing the entity, or empty if not found
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "apartamento.service.findEntityById",
      description = "Find apartamento entity by id")
  public Optional<com.pmrodrigues.condominio.model.Apartamento> findEntityById(Long id) {
    log.info("Finding apartamento entity by id: {}", id);
    return repository.findById(id);
  }
}
