package com.pmrodrigues.financeiro.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;

import com.pmrodrigues.financeiro.dto.ContribuicaoResponse;
import com.pmrodrigues.financeiro.dto.DesassociarItemOrcamentoRequest;
import com.pmrodrigues.financeiro.dto.ItemExtratoComOrcamentoResponse;
import com.pmrodrigues.financeiro.dto.SugestaoItemOrcamentoResponse;
import com.pmrodrigues.financeiro.mapper.ItemExtratoMapper;
import com.pmrodrigues.financeiro.model.ItemExtrato;
import com.pmrodrigues.financeiro.model.ItemOrcamento;
import com.pmrodrigues.financeiro.repository.ItemExtratoRepository;
import com.pmrodrigues.financeiro.repository.ItemOrcamentoRepository;
import com.pmrodrigues.financeiro.util.SugestaoScoreCalculator;
import io.micrometer.core.annotation.Timed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages associations between bank statement items ({@link ItemExtrato}) and budget line items
 * ({@link ItemOrcamento}) after bank reconciliation.
 *
 * <p>Provides association, removal, scored suggestions, and contribution calculation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssociacaoOrcamentoService {

  private final ItemExtratoRepository itemExtratoRepository;
  private final ItemOrcamentoRepository itemOrcamentoRepository;
  private final ItemExtratoMapper mapper;

  /**
   * Associates a statement item with a budget line item.
   *
   * @param itemExtratoId the statement item identifier
   * @param itemOrcamentoId the budget line item identifier
   * @return updated statement item representation
   * @throws org.springframework.web.server.ResponseStatusException 404 if either entity is not
   *     found
   */
  @Transactional
  @Timed(
      value = "associacao.orcamento.service.associar",
      description = "Associate ItemExtrato to ItemOrcamento")
  public ItemExtratoComOrcamentoResponse associar(Long itemExtratoId, Long itemOrcamentoId) {
    log.info("Associando itemExtratoId={} a itemOrcamentoId={}", itemExtratoId, itemOrcamentoId);
    var extrato =
        itemExtratoRepository
            .findById(itemExtratoId)
            .orElseThrow(() -> notFound("ItemExtrato", itemExtratoId));
    var orcamento =
        itemOrcamentoRepository
            .findById(itemOrcamentoId)
            .orElseThrow(() -> notFound("ItemOrcamento", itemOrcamentoId));
    extrato.setItemOrcamento(orcamento);
    var saved = itemExtratoRepository.save(extrato);
    log.info("ItemExtrato {} associado a ItemOrcamento {}", itemExtratoId, itemOrcamentoId);
    return mapper.toDTO(saved);
  }

  /**
   * Removes the association between a statement item and its current budget line item.
   *
   * @param itemExtratoId the statement item identifier
   * @param request desassociation request containing the mandatory justification
   * @return updated statement item representation with {@code itemOrcamento = null}
   * @throws org.springframework.web.server.ResponseStatusException 404 if the statement item is not
   *     found
   */
  @Transactional
  @Timed(
      value = "associacao.orcamento.service.desassociar",
      description = "Disassociate ItemExtrato from ItemOrcamento")
  public ItemExtratoComOrcamentoResponse desassociar(
      Long itemExtratoId, DesassociarItemOrcamentoRequest request) {
    log.info(
        "Desassociando itemExtratoId={}, justificativa={}", itemExtratoId, request.justificativa());
    var extrato =
        itemExtratoRepository
            .findById(itemExtratoId)
            .orElseThrow(() -> notFound("ItemExtrato", itemExtratoId));
    extrato.setItemOrcamento(null);
    var saved = itemExtratoRepository.save(extrato);
    log.info("ItemExtrato {} desassociado com sucesso", itemExtratoId);
    return mapper.toDTO(saved);
  }

  /**
   * Returns up to five scored {@link ItemOrcamento} suggestions for the given statement item,
   * ordered by descending score (best match first).
   *
   * @param itemExtratoId the statement item to find suggestions for
   * @return list of scored suggestions, best match first; empty if no candidates score above zero
   * @throws org.springframework.web.server.ResponseStatusException 404 if the statement item is not
   *     found
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "associacao.orcamento.service.sugerir",
      description = "Suggest ItemOrcamento candidates for ItemExtrato")
  public List<SugestaoItemOrcamentoResponse> sugerirItemOrcamento(Long itemExtratoId) {
    log.info("Sugerindo ItemOrcamento para itemExtratoId={}", itemExtratoId);
    var extrato =
        itemExtratoRepository
            .findById(itemExtratoId)
            .orElseThrow(() -> notFound("ItemExtrato", itemExtratoId));
    var sugestoes =
        itemOrcamentoRepository.findAll().stream()
            .map(o -> buildSugestao(extrato, o))
            .filter(s -> s.score() > 0)
            .sorted(Comparator.comparingInt(SugestaoItemOrcamentoResponse::score).reversed())
            .limit(5)
            .collect(Collectors.toList());
    log.info(
        "Sugestões para itemExtratoId={}: {} candidatos encontrados",
        itemExtratoId,
        sugestoes.size());
    return sugestoes;
  }

  /**
   * Calculates the realisation summary for a budget line item based on all its associated statement
   * items.
   *
   * @param itemOrcamentoId the budget line item identifier
   * @return contribution summary with realized value and percentage
   * @throws org.springframework.web.server.ResponseStatusException 404 if the budget line item is
   *     not found
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "associacao.orcamento.service.calcular",
      description = "Calculate contribution for ItemOrcamento")
  public ContribuicaoResponse calcularContribuicao(Long itemOrcamentoId) {
    log.info("Calculando contribuição para itemOrcamentoId={}", itemOrcamentoId);
    var orcamento =
        itemOrcamentoRepository
            .findById(itemOrcamentoId)
            .orElseThrow(() -> notFound("ItemOrcamento", itemOrcamentoId));
    var itens = itemExtratoRepository.findByItemOrcamentoId(itemOrcamentoId);
    var valorRealizado =
        itens.stream().map(ItemExtrato::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    var percentual =
        orcamento.getValorPrevisto().compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : valorRealizado
                .divide(orcamento.getValorPrevisto(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    log.info(
        "Contribuição para itemOrcamentoId={}: valorRealizado={}, percentual={}",
        itemOrcamentoId,
        valorRealizado,
        percentual);
    return new ContribuicaoResponse(
        itemOrcamentoId, orcamento.getValorPrevisto(), valorRealizado, percentual, itens.size());
  }

  private SugestaoItemOrcamentoResponse buildSugestao(
      ItemExtrato extrato, ItemOrcamento orcamento) {
    int score = SugestaoScoreCalculator.calcularScore(extrato, orcamento);
    return new SugestaoItemOrcamentoResponse(
        orcamento.getId(),
        orcamento.getPlanoContas().getCodigo(),
        orcamento.getPlanoContas().getDescricao(),
        orcamento.getValorPrevisto(),
        orcamento.getValorRealizado(),
        score,
        SugestaoScoreCalculator.tipoCompativel(
            extrato.getTipo(), orcamento.getPlanoContas().getTipo()),
        SugestaoScoreCalculator.motivoSugestao(extrato, orcamento));
  }
}
