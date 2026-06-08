package com.pmrodrigues.commons.service;

import static com.pmrodrigues.commons.specification.EstadoSpecification.hasNome;
import static com.pmrodrigues.commons.specification.EstadoSpecification.hasUf;
import static com.pmrodrigues.commons.util.Exceptions.notFound;

import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.dto.EstadoFilterDTO;
import com.pmrodrigues.commons.mapper.EstadoMapper;
import com.pmrodrigues.commons.repository.EstadoRepository;
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
 * Service providing CRUD operations for {@link com.pmrodrigues.commons.model.Estado} with Redis
 * caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstadoService {

  private final EstadoRepository repository;
  private final EstadoMapper mapper;

  static final String CACHE = "estados";

  /**
   * Returns estados matching the supplied filter criteria.
   *
   * @param dto filter containing optional nome substring and exact uf; absent fields are ignored
   * @return matching estado DTOs
   */
  @Transactional(readOnly = true)
  @Timed(value = "estado.service.filterBy", description = "Filter estados")
  @Cacheable(value = CACHE, key = "'filter:' + #dto.nome() + ':' + #dto.uf()")
  public List<EstadoDTO> filterBy(EstadoFilterDTO dto) {
    log.info("Filtering estados: nome={}, uf={}", dto.nome(), dto.uf());
    var result =
        repository.findAll(Specification.allOf(hasNome(dto.nome()), hasUf(dto.uf()))).stream()
            .map(mapper::toDTO)
            .toList();
    log.info("Filter estados: {} results", result.size());
    return result;
  }

  /**
   * Looks up a single estado by its primary key.
   *
   * @param id the estado identifier
   * @return the found DTO, or empty if none exists
   */
  @Transactional(readOnly = true)
  @Timed(value = "estado.service.findById", description = "Find estado by id")
  @Cacheable(value = CACHE, key = "#id", unless = "#result == null")
  public Optional<EstadoDTO> findById(Long id) {
    log.info("Looking up estado by id: {}", id);
    var result = repository.findById(id).map(mapper::toDTO);
    log.info("Estado lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
    return result;
  }

  /**
   * Persists a new estado and evicts the estados cache.
   *
   * @param dto data for the new estado
   * @return the persisted estado as a DTO
   */
  @Transactional
  @Timed(value = "estado.service.create", description = "Create estado")
  @CacheEvict(value = CACHE, allEntries = true)
  public EstadoDTO create(EstadoDTO dto) {
    log.info("Creating estado with uf: {}", dto.uf());
    var saved = repository.save(mapper.toEntity(dto));
    log.info("Estado created successfully with id: {}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Updates an existing estado identified by {@code dto.id()} and evicts the estados cache.
   *
   * @param dto updated values; {@code id} must reference an existing estado
   * @return the updated estado as a DTO
   * @throws org.springframework.web.server.ResponseStatusException 404 if no estado exists with the
   *     given id
   */
  @Transactional
  @Timed(value = "estado.service.update", description = "Update estado")
  @CacheEvict(value = CACHE, allEntries = true)
  public EstadoDTO update(EstadoDTO dto) {
    log.info("Updating estado with id: {}", dto.id());
    var entity = repository.findById(dto.id()).orElseThrow(() -> notFound("Estado", dto.id()));
    mapper.updateEntity(entity, dto);
    var saved = repository.save(entity);
    log.info("Estado updated successfully: {}", dto.id());
    return mapper.toDTO(saved);
  }

  /**
   * Deletes an estado by its primary key and evicts the estados cache.
   *
   * @param id the estado identifier
   * @throws org.springframework.web.server.ResponseStatusException 404 if no estado exists with the
   *     given id
   */
  @Transactional
  @Timed(value = "estado.service.delete", description = "Delete estado")
  @CacheEvict(value = CACHE, allEntries = true)
  public void delete(Long id) {
    log.info("Deleting estado with id: {}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("Estado", id));
    repository.delete(entity);
    log.info("Estado deleted successfully: {}", id);
  }
}
