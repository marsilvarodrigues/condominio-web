package com.pmrodrigues.condominio.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.condominio.specification.BlocoSpecification.hasBloco;

import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.BlocoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.mapper.BlocoMapper;
import com.pmrodrigues.condominio.repository.BlocoRepository;
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
 * Application service for managing blocos (building blocks), with tenant-scoped caching and
 * soft-delete support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlocoService {

  static final String CACHE = "blocos";
  static final String TENANT_KEY =
      "T(com.pmrodrigues.commons.tenant.TenantContext).getCondominioId()";

  private final BlocoRepository repository;
  private final BlocoMapper mapper;

  /**
   * Returns blocos matching the supplied filter criteria.
   *
   * @param dto filter containing an optional bloco name substring; absent field is ignored
   * @return matching bloco DTOs
   */
  @Transactional(readOnly = true)
  @Timed(value = "bloco.service.filterBy", description = "Filter blocos")
  @Cacheable(value = CACHE, key = TENANT_KEY + " + ':filter:' + #dto.bloco()")
  public List<BlocoDTO> filterBy(BlocoFilterDTO dto) {
    log.info("Filtering blocos: bloco={}", dto.bloco());
    var result =
        repository.findAll(Specification.allOf(hasBloco(dto.bloco()))).stream()
            .map(mapper::toDTO)
            .toList();
    log.info("Filter blocos: {} results", result.size());
    return result;
  }

  /**
   * Looks up a single bloco by its primary key.
   *
   * @param id bloco primary key
   * @return the DTO wrapped in an Optional, or empty if not found
   */
  @Transactional(readOnly = true)
  @Timed(value = "bloco.service.findById", description = "Find bloco by id")
  @Cacheable(value = CACHE, key = TENANT_KEY + " + ':' + #id", unless = "#result == null")
  public Optional<BlocoDTO> findById(Long id) {
    log.info("Looking up bloco by id: {}", id);
    var result = repository.findById(id).map(mapper::toDTO);
    log.info("Bloco lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
    return result;
  }

  /**
   * Persists a new bloco and evicts the tenant cache.
   *
   * @param dto creation payload with bloco name and numero
   * @return the persisted bloco as a DTO
   */
  @Transactional
  @Timed(value = "bloco.service.create", description = "Create bloco")
  @CacheEvict(value = CACHE, allEntries = true)
  public BlocoDTO create(CreateBlocoDTO dto) {
    log.info("Creating bloco: numero={}, bloco={}", dto.numero(), dto.bloco());
    var saved = repository.save(mapper.toEntity(dto));
    log.info("Bloco created successfully with id: {}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Applies changes from the DTO to the existing bloco entity and saves it.
   *
   * @param dto updated bloco data including the target id
   * @return the updated bloco as a DTO
   * @throws org.springframework.web.server.ResponseStatusException if no bloco exists with the
   *     given id
   */
  @Transactional
  @Timed(value = "bloco.service.update", description = "Update bloco")
  @CacheEvict(value = CACHE, allEntries = true)
  public BlocoDTO update(BlocoDTO dto) {
    log.info("Updating bloco with id: {}", dto.id());
    var entity = repository.findById(dto.id()).orElseThrow(() -> notFound("Bloco", dto.id()));
    mapper.updateEntity(entity, dto);
    var saved = repository.save(entity);
    log.info("Bloco updated successfully: {}", dto.id());
    return mapper.toDTO(saved);
  }

  /**
   * Soft-deletes the bloco with the given id.
   *
   * @param id primary key of the bloco to delete
   * @throws org.springframework.web.server.ResponseStatusException if no bloco exists with the
   *     given id
   */
  @Transactional
  @Timed(value = "bloco.service.delete", description = "Delete bloco")
  @CacheEvict(value = CACHE, allEntries = true)
  public void delete(Long id) {
    log.info("Deleting bloco with id: {}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("Bloco", id));
    repository.delete(entity);
    log.info("Bloco soft-deleted successfully: {}", id);
  }
}
