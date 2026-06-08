package com.pmrodrigues.financeiro.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.financeiro.specification.LancamentoBancarioSpecification.dataFim;
import static com.pmrodrigues.financeiro.specification.LancamentoBancarioSpecification.dataInicio;
import static com.pmrodrigues.financeiro.specification.LancamentoBancarioSpecification.hasContaBancaria;
import static com.pmrodrigues.financeiro.specification.LancamentoBancarioSpecification.hasOrigem;
import static com.pmrodrigues.financeiro.specification.LancamentoBancarioSpecification.hasStatus;
import static com.pmrodrigues.financeiro.specification.LancamentoBancarioSpecification.hasTipo;

import com.pmrodrigues.financeiro.dto.CreateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.LancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.LancamentoBancarioFilterDTO;
import com.pmrodrigues.financeiro.dto.UpdateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.mapper.LancamentoBancarioMapper;
import com.pmrodrigues.financeiro.model.ContaBancaria;
import com.pmrodrigues.financeiro.model.LancamentoBancario;
import com.pmrodrigues.financeiro.repository.LancamentoBancarioRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing bank account entries (lancamentos) for the current condominium tenant.
 * Cross-module validation of ContaBancaria goes through {@link ContaBancariaService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LancamentoBancarioService {

  private final LancamentoBancarioRepository repository;
  private final ContaBancariaService contaBancariaService;
  private final LancamentoBancarioMapper mapper;

  /**
   * Returns a paginated list of lancamentos matching the given filter.
   *
   * @param dto filter parameters
   * @param pageable pagination
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "lancamento.bancario.service.filterBy",
      description = "Filter lancamentos bancarios")
  public Page<LancamentoBancarioDTO> filterBy(LancamentoBancarioFilterDTO dto, Pageable pageable) {
    log.info(
        "Filtering lancamentos bancarios: contaBancariaId={}, tipo={}, status={}",
        dto.contaBancariaId(),
        dto.tipo(),
        dto.status());
    var spec =
        Specification.allOf(
            hasContaBancaria(dto.contaBancariaId()),
            hasTipo(dto.tipo()),
            hasOrigem(dto.origem()),
            hasStatus(dto.status()),
            dataInicio(dto.dataInicio()),
            dataFim(dto.dataFim()));
    var result = repository.findAll(spec, pageable).map(mapper::toDTO);
    log.info("Filter lancamentos bancarios: {} results", result.getTotalElements());
    return result;
  }

  /**
   * Looks up a single lancamento by its primary key.
   *
   * @param id the lancamento identifier
   * @throws org.springframework.web.server.ResponseStatusException 404 if not found
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "lancamento.bancario.service.findById",
      description = "Find lancamento bancario by id")
  public LancamentoBancarioDTO findById(Long id) {
    log.info("Looking up lancamento bancario by id: {}", id);
    var result =
        repository
            .findById(id)
            .map(mapper::toDTO)
            .orElseThrow(() -> notFound("LancamentoBancario", id));
    log.info("LancamentoBancario lookup by id {}: found", id);
    return result;
  }

  /**
   * Creates a new lancamento bancario for the current condominium. Validates that the referenced
   * conta bancaria exists.
   *
   * @param dto creation payload
   * @throws org.springframework.web.server.ResponseStatusException 404 if conta bancaria not found
   */
  @Transactional
  @Timed(value = "lancamento.bancario.service.create", description = "Create lancamento bancario")
  public LancamentoBancarioDTO create(CreateLancamentoBancarioDTO dto) {
    log.info(
        "Creating lancamento bancario: contaBancariaId={}, tipo={}, valor={}",
        dto.contaBancariaId(),
        dto.tipo(),
        dto.valor());
    contaBancariaService.findById(dto.contaBancariaId());
    var contaRef = new ContaBancaria();
    contaRef.setId(dto.contaBancariaId());
    LancamentoBancario entity = mapper.toEntity(dto);
    entity.setContaBancaria(contaRef);
    var saved = repository.save(entity);
    log.info("LancamentoBancario created with id: {}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Updates mutable fields of an existing lancamento.
   *
   * @param id the lancamento identifier
   * @param dto update payload
   * @throws org.springframework.web.server.ResponseStatusException 404 if not found
   */
  @Transactional
  @Timed(value = "lancamento.bancario.service.update", description = "Update lancamento bancario")
  public LancamentoBancarioDTO update(Long id, UpdateLancamentoBancarioDTO dto) {
    log.info("Updating lancamento bancario with id: {}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("LancamentoBancario", id));
    mapper.updateEntity(entity, dto);
    var saved = repository.save(entity);
    log.info("LancamentoBancario updated: {}", id);
    return mapper.toDTO(saved);
  }

  /**
   * Soft-deletes a lancamento bancario.
   *
   * @param id the lancamento identifier
   * @throws org.springframework.web.server.ResponseStatusException 404 if not found
   */
  @Transactional
  @Timed(value = "lancamento.bancario.service.delete", description = "Delete lancamento bancario")
  public void delete(Long id) {
    log.info("Deleting lancamento bancario with id: {}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("LancamentoBancario", id));
    repository.delete(entity);
    log.info("LancamentoBancario deleted: {}", id);
  }

  /**
   * Soft-deletes all lancamentos whose conta bancaria belongs to the given condominio.
   *
   * @param condominioId the condominio whose lancamentos must be soft-deleted
   */
  @Transactional
  @Timed(
      value = "lancamento.bancario.service.softDeleteByCondominioId",
      description = "Cascade soft-delete lancamentos bancarios by condominio")
  public void softDeleteByCondominioId(Long condominioId) {
    log.info("Cascade soft-deleting lancamentos bancarios for condominioId={}", condominioId);
    repository.softDeleteByCondominioId(condominioId);
  }
}
