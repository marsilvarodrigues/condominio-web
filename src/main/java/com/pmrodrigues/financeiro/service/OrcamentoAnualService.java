package com.pmrodrigues.financeiro.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.financeiro.specification.OrcamentoAnualSpecification.hasExercicio;
import static com.pmrodrigues.financeiro.specification.OrcamentoAnualSpecification.hasStatus;

import com.pmrodrigues.financeiro.dto.AprovarOrcamentoDTO;
import com.pmrodrigues.financeiro.dto.CreateItemOrcamentoDTO;
import com.pmrodrigues.financeiro.dto.CreateOrcamentoAnualDTO;
import com.pmrodrigues.financeiro.dto.ItemOrcamentoDTO;
import com.pmrodrigues.financeiro.dto.OrcamentoAnualDTO;
import com.pmrodrigues.financeiro.dto.OrcamentoAnualFilterDTO;
import com.pmrodrigues.financeiro.mapper.OrcamentoAnualMapper;
import com.pmrodrigues.financeiro.model.ItemOrcamento;
import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import com.pmrodrigues.financeiro.repository.ItemOrcamentoRepository;
import com.pmrodrigues.financeiro.repository.OrcamentoAnualRepository;
import io.micrometer.core.annotation.Timed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Application service managing annual budgets through their lifecycle, including item management
 * and approval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrcamentoAnualService {

  static final String CACHE = "orcamentos";
  static final String TENANT_KEY =
      "T(com.pmrodrigues.commons.tenant.TenantContext).getCondominioId()";

  private final OrcamentoAnualRepository repository;
  private final ItemOrcamentoRepository itemRepository;
  private final OrcamentoAnualMapper mapper;

  /** Returns budgets matching the supplied filter criteria. */
  @Transactional(readOnly = true)
  @Timed(value = "orcamento.service.filterBy", description = "Filter orcamentos anuais")
  @Cacheable(
      value = CACHE,
      key = TENANT_KEY + " + ':filter:' + #dto.exercicio() + ':' + #dto.status()")
  public List<OrcamentoAnualDTO> filterBy(OrcamentoAnualFilterDTO dto) {
    log.info("Filtering orcamentos: exercicio={}, status={}", dto.exercicio(), dto.status());
    var result =
        repository
            .findAll(Specification.allOf(hasExercicio(dto.exercicio()), hasStatus(dto.status())))
            .stream()
            .map(mapper::toDTO)
            .toList();
    log.info("Filter orcamentos: {} results", result.size());
    return result;
  }

  /** Looks up a single budget by its primary key, including its items. */
  @Transactional(readOnly = true)
  @Timed(value = "orcamento.service.findById", description = "Find orcamento by id")
  @Cacheable(value = CACHE, key = TENANT_KEY + " + ':' + #id", unless = "#result == null")
  public Optional<OrcamentoAnualDTO> findById(Long id) {
    log.info("Looking up orcamento by id: {}", id);
    var result = repository.findById(id).map(mapper::toDTO);
    log.info("Orcamento lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
    return result;
  }

  /** Persists a new OrcamentoAnual in RASCUNHO state and evicts the cache. */
  @Transactional
  @Timed(value = "orcamento.service.create", description = "Create orcamento anual")
  @CacheEvict(value = CACHE, allEntries = true)
  public OrcamentoAnualDTO create(CreateOrcamentoAnualDTO dto) {
    log.info("Creating orcamento anual: exercicio={}", dto.exercicio());
    var saved = repository.save(mapper.toEntity(dto));
    log.info("OrcamentoAnual created with id: {}", saved.getId());
    return mapper.toDTO(saved);
  }

  /**
   * Updates the exercise year of a RASCUNHO budget.
   *
   * @throws ResponseStatusException 404 if not found; 409 if budget is not in RASCUNHO
   */
  @Transactional
  @Timed(value = "orcamento.service.update", description = "Update orcamento anual")
  @CacheEvict(value = CACHE, allEntries = true)
  public OrcamentoAnualDTO update(Long id, CreateOrcamentoAnualDTO dto) {
    log.info("Updating orcamento anual with id: {}", id);
    var entity = findRascunho(id);
    entity.setExercicio(dto.exercicio());
    var saved = repository.save(entity);
    log.info("OrcamentoAnual updated: {}", id);
    return mapper.toDTO(saved);
  }

  /**
   * Approves the budget, computing the estimated monthly fee per unit.
   *
   * @param id budget primary key
   * @param dto contains the number of active units used for fee computation
   * @throws ResponseStatusException 404 if not found; 409 if not RASCUNHO or year already approved
   */
  @Transactional
  @Timed(value = "orcamento.service.aprovar", description = "Aprovar orcamento anual")
  @CacheEvict(value = CACHE, allEntries = true)
  public OrcamentoAnualDTO aprovar(Long id, AprovarOrcamentoDTO dto) {
    log.info("Approving orcamento anual id={}, numeroUnidades={}", id, dto.numeroUnidades());
    var entity = findRascunho(id);

    if (repository.existsByExercicioAndStatus(entity.getExercicio(), StatusOrcamento.APROVADO)) {
      log.error("Orcamento already approved for exercicio={}", entity.getExercicio());
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Já existe um orçamento aprovado para o exercício " + entity.getExercicio());
    }

    var items = itemRepository.findByOrcamentoAnualId(id);
    var totalPrevisto =
        items.stream()
            .map(ItemOrcamento::getValorPrevisto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    var taxa =
        totalPrevisto.divide(
            BigDecimal.valueOf(12L * dto.numeroUnidades()), 2, RoundingMode.HALF_UP);

    entity.setStatus(StatusOrcamento.APROVADO);
    entity.setTaxaEstimadaUnidade(taxa);
    var saved = repository.save(entity);
    log.info("OrcamentoAnual approved: id={}, taxa={}", id, taxa);
    return mapper.toDTO(saved);
  }

  /**
   * Closes an approved budget.
   *
   * @throws ResponseStatusException 404 if not found; 409 if not APROVADO
   */
  @Transactional
  @Timed(value = "orcamento.service.encerrar", description = "Encerrar orcamento anual")
  @CacheEvict(value = CACHE, allEntries = true)
  public OrcamentoAnualDTO encerrar(Long id) {
    log.info("Closing orcamento anual id={}", id);
    var entity = repository.findById(id).orElseThrow(() -> notFound("OrcamentoAnual", id));
    if (entity.getStatus() != StatusOrcamento.APROVADO) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Apenas orçamentos APROVADOS podem ser encerrados");
    }
    entity.setStatus(StatusOrcamento.ENCERRADO);
    var saved = repository.save(entity);
    log.info("OrcamentoAnual closed: {}", id);
    return mapper.toDTO(saved);
  }

  /**
   * Soft-deletes a RASCUNHO budget and all its items.
   *
   * @throws ResponseStatusException 404 if not found; 409 if not RASCUNHO
   */
  @Transactional
  @Timed(value = "orcamento.service.delete", description = "Delete orcamento anual")
  @CacheEvict(value = CACHE, allEntries = true)
  public void delete(Long id) {
    log.info("Deleting orcamento anual id={}", id);
    findRascunho(id);
    itemRepository.softDeleteByOrcamentoAnualId(id);
    repository.deleteById(id);
    log.info("OrcamentoAnual soft-deleted: {}", id);
  }

  /**
   * Adds a new item to a RASCUNHO budget.
   *
   * @throws ResponseStatusException 404 if budget not found; 409 if not RASCUNHO
   */
  @Transactional
  @Timed(value = "orcamento.service.addItem", description = "Add item to orcamento")
  @CacheEvict(value = CACHE, allEntries = true)
  public ItemOrcamentoDTO addItem(Long orcamentoId, CreateItemOrcamentoDTO dto) {
    log.info("Adding item to orcamento id={}: planoContasId={}", orcamentoId, dto.planoContasId());
    var orcamento = findRascunho(orcamentoId);
    var item = mapper.toItemEntity(orcamento, dto);
    var saved = itemRepository.save(item);
    log.info("Item added to orcamento: itemId={}", saved.getId());
    return mapper.toItemDTO(saved);
  }

  /**
   * Updates the predicted value of a budget item in a RASCUNHO budget.
   *
   * @throws ResponseStatusException 404 if budget or item not found; 409 if not RASCUNHO
   */
  @Transactional
  @Timed(value = "orcamento.service.updateItem", description = "Update orcamento item")
  @CacheEvict(value = CACHE, allEntries = true)
  public ItemOrcamentoDTO updateItem(Long orcamentoId, Long itemId, CreateItemOrcamentoDTO dto) {
    log.info("Updating item id={} in orcamento id={}", itemId, orcamentoId);
    findRascunho(orcamentoId);
    var item = itemRepository.findById(itemId).orElseThrow(() -> notFound("ItemOrcamento", itemId));
    item.setValorPrevisto(dto.valorPrevisto());
    var saved = itemRepository.save(item);
    log.info("Item updated: {}", itemId);
    return mapper.toItemDTO(saved);
  }

  /**
   * Soft-deletes a budget item from a RASCUNHO budget.
   *
   * @throws ResponseStatusException 404 if budget or item not found; 409 if not RASCUNHO
   */
  @Transactional
  @Timed(value = "orcamento.service.deleteItem", description = "Delete orcamento item")
  @CacheEvict(value = CACHE, allEntries = true)
  public void deleteItem(Long orcamentoId, Long itemId) {
    log.info("Deleting item id={} from orcamento id={}", itemId, orcamentoId);
    findRascunho(orcamentoId);
    var item = itemRepository.findById(itemId).orElseThrow(() -> notFound("ItemOrcamento", itemId));
    itemRepository.delete(item);
    log.info("Item soft-deleted: {}", itemId);
  }

  /**
   * Soft-deletes all OrcamentoAnual items and the budgets themselves for the given condominio.
   *
   * @param condominioId the condominio whose budget data must be soft-deleted
   */
  @Transactional
  @Timed(
      value = "orcamento.service.softDeleteByCondominioId",
      description = "Cascade soft-delete orcamentos by condominio")
  @CacheEvict(value = CACHE, allEntries = true)
  public void softDeleteByCondominioId(Long condominioId) {
    log.info("Cascade soft-deleting orcamentos for condominioId={}", condominioId);
    itemRepository.softDeleteByCondominioId(condominioId);
    repository.softDeleteByCondominioId(condominioId);
  }

  private OrcamentoAnual findRascunho(Long id) {
    var entity = repository.findById(id).orElseThrow(() -> notFound("OrcamentoAnual", id));
    if (entity.getStatus() != StatusOrcamento.RASCUNHO) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Apenas orçamentos em RASCUNHO podem ser modificados");
    }
    return entity;
  }
}
