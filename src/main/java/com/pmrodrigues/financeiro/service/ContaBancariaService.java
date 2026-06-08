package com.pmrodrigues.financeiro.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.financeiro.specification.ContaBancariaSpecification.hasAgencia;
import static com.pmrodrigues.financeiro.specification.ContaBancariaSpecification.hasConta;
import static com.pmrodrigues.financeiro.specification.ContaBancariaSpecification.hasTipo;
import static com.pmrodrigues.financeiro.specification.ContaBancariaSpecification.isAtiva;

import com.pmrodrigues.commons.model.Banco;
import com.pmrodrigues.commons.service.BancoService;
import com.pmrodrigues.financeiro.dto.ContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.ContaBancariaFilterDTO;
import com.pmrodrigues.financeiro.dto.CreateContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.UpdateContaBancariaDTO;
import com.pmrodrigues.financeiro.mapper.ContaBancariaMapper;
import com.pmrodrigues.financeiro.model.ContaBancaria;
import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import com.pmrodrigues.financeiro.repository.ContaBancariaRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service managing bank accounts for the current condominium tenant. Enforces the
 * single-FUNDO_RESERVA-per-condominium constraint at the service layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContaBancariaService {

  static final String CACHE = "contas-bancarias";

  private final ContaBancariaRepository repository;
  private final BancoService bancoService;
  private final ContaBancariaMapper mapper;

  /** Returns a paginated list of bank accounts matching the given filter. */
  @Transactional(readOnly = true)
  @Timed(value = "conta.bancaria.service.filterBy", description = "Filter contas bancarias")
  public Page<ContaBancariaDTO> filterBy(ContaBancariaFilterDTO dto, Pageable pageable) {
    log.info("Filtering contas bancarias: tipo={}, ativa={}", dto.tipo(), dto.ativa());
    var spec =
        Specification.allOf(
            hasTipo(dto.tipo()),
            isAtiva(dto.ativa()),
            hasAgencia(dto.agencia()),
            hasConta(dto.conta()));
    var result = repository.findAll(spec, pageable).map(mapper::toDTO);
    log.info("Filter contas bancarias: {} results", result.getTotalElements());
    return result;
  }

  /**
   * Looks up a single bank account by its primary key.
   *
   * @throws ResponseStatusException 404 if not found
   */
  @Transactional(readOnly = true)
  @Timed(value = "conta.bancaria.service.findById", description = "Find conta bancaria by id")
  @Cacheable(value = CACHE, key = "#id", unless = "#result == null")
  public ContaBancariaDTO findById(Long id) {
    log.info("Looking up conta bancaria by id: {}", id);
    var result =
        repository.findById(id).map(mapper::toDTO).orElseThrow(() -> notFound("ContaBancaria", id));
    log.info("ContaBancaria lookup by id {}: found", id);
    return result;
  }

  /**
   * Creates a new bank account for the current condominium. Refuses if a FUNDO_RESERVA account
   * already exists and the new type is also FUNDO_RESERVA.
   *
   * @throws ResponseStatusException 409 if a FUNDO_RESERVA already exists
   * @throws ResponseStatusException 404 if the referenced banco does not exist
   */
  @Transactional
  @Timed(value = "conta.bancaria.service.create", description = "Create conta bancaria")
  @CacheEvict(value = CACHE, allEntries = true)
  public ContaBancariaDTO create(CreateContaBancariaDTO dto) {
    log.info("Creating conta bancaria: tipo={}, bancoId={}", dto.tipo(), dto.bancoId());
    if (TipoContaBancaria.FUNDO_RESERVA == dto.tipo()
        && repository.existsByTipoAndDeletedFalse(TipoContaBancaria.FUNDO_RESERVA)) {
      log.error("FUNDO_RESERVA account already exists for this condominium");
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Já existe uma conta do tipo FUNDO_RESERVA para este condomínio");
    }
    bancoService.findById(dto.bancoId()); // throws 404 if banco not found
    var bancoRef = new Banco();
    bancoRef.setId(dto.bancoId());
    ContaBancaria entity = mapper.toEntity(dto);
    entity.setBanco(bancoRef);
    var saved = repository.save(entity);
    log.info("ContaBancaria created with id: {}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Updates mutable fields of an existing bank account.
   *
   * @throws ResponseStatusException 404 if not found
   */
  @Transactional
  @Timed(value = "conta.bancaria.service.update", description = "Update conta bancaria")
  @CacheEvict(value = CACHE, allEntries = true)
  public ContaBancariaDTO update(Long id, UpdateContaBancariaDTO dto) {
    log.info("Updating conta bancaria with id: {}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("ContaBancaria", id));
    mapper.updateEntity(entity, dto);
    var saved = repository.save(entity);
    log.info("ContaBancaria updated: {}", id);
    return mapper.toDTO(saved);
  }

  /**
   * Activates or deactivates a bank account.
   *
   * @throws ResponseStatusException 404 if not found
   */
  @Transactional
  @Timed(value = "conta.bancaria.service.setAtiva", description = "Toggle ativa for conta bancaria")
  @CacheEvict(value = CACHE, allEntries = true)
  public ContaBancariaDTO setAtiva(Long id, boolean ativa) {
    log.info("Setting conta bancaria {} ativa={}", id, ativa);
    var entity = repository.findById(id).orElseThrow(() -> notFound("ContaBancaria", id));
    entity.setAtiva(ativa);
    var saved = repository.save(entity);
    log.info("ContaBancaria {} ativa updated to {}", id, ativa);
    return mapper.toDTO(saved);
  }

  /**
   * Soft-deletes a bank account.
   *
   * @throws ResponseStatusException 404 if not found
   */
  @Transactional
  @Timed(value = "conta.bancaria.service.delete", description = "Delete conta bancaria")
  @CacheEvict(value = CACHE, allEntries = true)
  public void delete(Long id) {
    log.info("Deleting conta bancaria with id: {}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("ContaBancaria", id));
    repository.delete(entity);
    log.info("ContaBancaria deleted: {}", id);
  }

  /**
   * Soft-deletes all bank accounts belonging to the given condominio.
   *
   * @param condominioId the condominio whose accounts must be soft-deleted
   */
  @Transactional
  @Timed(
      value = "conta.bancaria.service.softDeleteByCondominioId",
      description = "Cascade soft-delete contas bancarias by condominio")
  @CacheEvict(value = CACHE, allEntries = true)
  public void softDeleteByCondominioId(Long condominioId) {
    log.info("Cascade soft-deleting contas bancarias for condominioId={}", condominioId);
    repository.softDeleteByCondominioId(condominioId);
  }
}
