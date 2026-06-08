package com.pmrodrigues.financeiro.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.RateioExecucaoDTO;
import com.pmrodrigues.financeiro.dto.RateioExecucaoFilterDTO;
import com.pmrodrigues.financeiro.dto.RateioLoteResultado;
import com.pmrodrigues.financeiro.dto.RecalcularRateioRequest;
import com.pmrodrigues.financeiro.dto.SimularRateioRequest;
import com.pmrodrigues.financeiro.dto.SimularRateioResponse;
import com.pmrodrigues.financeiro.service.RateioService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing rateio simulation, manual trigger, forced recalculation and execution
 * history endpoints.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/rateio")
@RequiredArgsConstructor
public class RateioController {

  private final RateioService service;

  /**
   * Simulates a rateio for the given group and total without persisting any results.
   *
   * @param body simulation parameters
   * @param request current HTTP request
   */
  @PostMapping("/simular")
  @Timed(value = "rateio.controller.simular", description = "Simulate rateio")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<SimularRateioResponse>> simular(
      @Valid @RequestBody SimularRateioRequest body, HttpServletRequest request) {
    log.info(
        "POST /rateio/simular - grupoDespesaId={}, total={}",
        body.grupoDespesaId(),
        body.despesaTotal());
    return ResponseEntity.ok(ApiResponse.of(requestId(request), service.simular(body)));
  }

  /**
   * Manually triggers rateio for a specific expense. Requires ADMIN role.
   *
   * @param despesaId the expense identifier
   * @param request current HTTP request
   */
  @PostMapping("/despesa/{despesaId}")
  @Timed(value = "rateio.controller.ratearDespesa", description = "Rate a single despesa")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<RateioExecucaoDTO>> ratearDespesa(
      @PathVariable Long despesaId, HttpServletRequest request) {
    log.info("POST /rateio/despesa/{}", despesaId);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), service.ratearManual(despesaId)));
  }

  /**
   * Forces recalculation of all expenses in the current tenant.
   *
   * <p><strong>Warning:</strong> all previously calculated quotas are permanently replaced. The
   * request body must contain {@code confirmar: true}.
   *
   * @param body must have {@code confirmar == true}
   * @param request current HTTP request
   */
  @PostMapping("/recalcular")
  @Timed(value = "rateio.controller.recalcular", description = "Force full rateio recalculation")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<RateioLoteResultado>> recalcular(
      @Valid @RequestBody RecalcularRateioRequest body, HttpServletRequest request) {
    Long condominioId = TenantContext.getCondominioId();
    log.info("POST /rateio/recalcular - condominioId={}", condominioId);
    var resultado = service.recalcularTudo(condominioId);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), resultado));
  }

  /**
   * Returns a paginated history of rateio execution audit records.
   *
   * @param filter optional filter (status, tipoExecucao, periodo)
   * @param pageable pagination parameters
   * @param request current HTTP request
   */
  @GetMapping("/execucoes")
  @Timed(value = "rateio.controller.execucoes", description = "List rateio executions")
  public ResponseEntity<ApiResponse<Page<RateioExecucaoDTO>>> execucoes(
      @ModelAttribute RateioExecucaoFilterDTO filter,
      Pageable pageable,
      HttpServletRequest request) {
    log.info("GET /rateio/execucoes - filter={}", filter);
    return ResponseEntity.ok(
        ApiResponse.of(requestId(request), service.findExecucoes(filter, pageable)));
  }
}
