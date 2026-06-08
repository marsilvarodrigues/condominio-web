package com.pmrodrigues.financeiro.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.financeiro.specification.GrupoDespesaSpecification.hasEscopo;
import static com.pmrodrigues.financeiro.specification.GrupoDespesaSpecification.hasTipoRateio;

import com.pmrodrigues.financeiro.dto.CreateGrupoDespesaDTO;
import com.pmrodrigues.financeiro.dto.GrupoDespesaDTO;
import com.pmrodrigues.financeiro.dto.GrupoDespesaFilterDTO;
import com.pmrodrigues.financeiro.mapper.GrupoDespesaMapper;
import com.pmrodrigues.financeiro.model.GrupoDespesa;
import com.pmrodrigues.financeiro.repository.GrupoDespesaRepository;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for managing {@link GrupoDespesa} entities. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrupoDespesaService {

  private final GrupoDespesaRepository repository;
  private final GrupoDespesaMapper mapper;

  /**
   * Returns all groups matching the given filter criteria.
   *
   * @param filter optional filters (tipoRateio, escopo)
   * @return matching DTOs
   */
  @Transactional(readOnly = true)
  @Timed(value = "grupo.despesa.service.filterBy", description = "Filter grupos de despesa")
  public List<GrupoDespesaDTO> filterBy(GrupoDespesaFilterDTO filter) {
    log.info(
        "Filtering grupos de despesa: tipoRateio={}, escopo={}",
        filter.tipoRateio(),
        filter.escopo());
    var result =
        repository
            .findAll(
                Specification.allOf(hasTipoRateio(filter.tipoRateio()), hasEscopo(filter.escopo())))
            .stream()
            .map(mapper::toDTO)
            .toList();
    log.info("Filter grupos de despesa: {} results", result.size());
    return result;
  }

  /**
   * Returns a single group by its identifier.
   *
   * @param id the group identifier
   * @return the DTO
   * @throws org.springframework.web.server.ResponseStatusException 404 if not found
   */
  @Transactional(readOnly = true)
  @Timed(value = "grupo.despesa.service.findById", description = "Find grupo de despesa by id")
  public GrupoDespesaDTO findById(Long id) {
    log.info("Finding grupo de despesa id={}", id);
    return repository
        .findById(id)
        .map(mapper::toDTO)
        .orElseThrow(() -> notFound("GrupoDespesa", id));
  }

  /**
   * Returns the entity by id; used internally by other services.
   *
   * @param id the group identifier
   * @return an Optional containing the entity
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "grupo.despesa.service.findEntityById",
      description = "Find grupo de despesa entity by id")
  public Optional<GrupoDespesa> findEntityById(Long id) {
    log.info("Finding grupo de despesa entity id={}", id);
    return repository.findById(id);
  }

  /**
   * Creates a new group.
   *
   * @param dto creation payload
   * @return the created DTO
   */
  @Transactional
  @Timed(value = "grupo.despesa.service.create", description = "Create grupo de despesa")
  public GrupoDespesaDTO create(CreateGrupoDespesaDTO dto) {
    log.info("Creating grupo de despesa: nome={}", dto.nome());
    var entity = mapper.toEntity(dto);
    var saved = repository.save(entity);
    log.info("Created grupo de despesa id={}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Updates an existing group.
   *
   * @param id the group identifier
   * @param dto update payload
   * @return the updated DTO
   * @throws org.springframework.web.server.ResponseStatusException 404 if not found
   */
  @Transactional
  @Timed(value = "grupo.despesa.service.update", description = "Update grupo de despesa")
  public GrupoDespesaDTO update(Long id, GrupoDespesaDTO dto) {
    log.info("Updating grupo de despesa id={}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("GrupoDespesa", id));
    mapper.updateEntity(entity, dto);
    var saved = repository.save(entity);
    log.info("Updated grupo de despesa id={}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Soft-deletes a group.
   *
   * @param id the group identifier
   * @throws org.springframework.web.server.ResponseStatusException 404 if not found
   */
  @Transactional
  @Timed(value = "grupo.despesa.service.delete", description = "Delete grupo de despesa")
  public void delete(Long id) {
    log.info("Deleting grupo de despesa id={}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("GrupoDespesa", id));
    repository.delete(entity);
    log.info("Deleted grupo de despesa id={}", id);
  }
}
