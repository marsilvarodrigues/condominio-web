package com.pmrodrigues.financeiro.service;

import static com.pmrodrigues.commons.util.Exceptions.notFound;

import com.pmrodrigues.financeiro.model.CotaRateio;
import com.pmrodrigues.financeiro.model.RateioExecucao;
import com.pmrodrigues.financeiro.repository.CotaRateioRepository;
import com.pmrodrigues.financeiro.repository.RateioExecucaoRepository;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service providing read access to {@link CotaRateio} and {@link RateioExecucao}
 * records.
 *
 * <p>Exposes a module boundary so that other modules (e.g. the billing module) can query rateio
 * data without importing the financeiro repositories directly, in accordance with the CLAUDE.md
 * rule that services must not cross module boundaries via repository calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CotaRateioService {

  private final CotaRateioRepository cotaRateioRepository;
  private final RateioExecucaoRepository rateioExecucaoRepository;

  /**
   * Looks up a {@link RateioExecucao} by its primary key, throwing 404 if absent.
   *
   * @param id rateio execution primary key
   * @return the execution entity
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   */
  @Transactional(readOnly = true)
  @Timed(value = "cota.rateio.service.findExecucaoById", description = "Find rateio execucao by id")
  public RateioExecucao findExecucaoById(Long id) {
    log.info("findExecucaoById id={}", id);
    return rateioExecucaoRepository.findById(id).orElseThrow(() -> notFound("RateioExecucao", id));
  }

  /**
   * Returns an {@link Optional} wrapping the {@link RateioExecucao} with the given id.
   *
   * @param id rateio execution primary key
   * @return optional containing the entity, or empty if not found
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "cota.rateio.service.findExecucaoOptional",
      description = "Find rateio execucao optional")
  public Optional<RateioExecucao> findExecucaoOptional(Long id) {
    log.info("findExecucaoOptional id={}", id);
    return rateioExecucaoRepository.findById(id);
  }

  /**
   * Returns all {@link CotaRateio} records belonging to the given rateio execution.
   *
   * @param execucaoId primary key of the {@code RateioExecucao}
   * @return list of cotas (may be empty if the execution had no units)
   */
  @Transactional(readOnly = true)
  @Timed(
      value = "cota.rateio.service.findByExecucaoId",
      description = "Find cotas by rateio execucao id")
  public List<CotaRateio> findByExecucaoId(Long execucaoId) {
    log.info("findByExecucaoId execucaoId={}", execucaoId);
    var result = cotaRateioRepository.findByRateioExecucaoId(execucaoId);
    log.info("findByExecucaoId execucaoId={}: {} cotas found", execucaoId, result.size());
    return result;
  }
}
