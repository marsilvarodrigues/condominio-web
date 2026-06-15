package com.pmrodrigues.cobranca.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

import com.pmrodrigues.cobranca.dto.CancelarCobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaFilterDTO;
import com.pmrodrigues.cobranca.dto.CobrancaResumoDTO;
import com.pmrodrigues.cobranca.dto.GerarCobrancasDTO;
import com.pmrodrigues.cobranca.service.CobrancaService;
import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.gateway.config.AsaasProperties;
import com.pmrodrigues.gateway.dto.AsaasWebhookPayload;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Cobrança module endpoints.
 *
 * <p>All mutating operations (charge generation, cancellation, email resend) require the {@code
 * ADMIN} role. Read operations are accessible to any authenticated user. The Asaas webhook endpoint
 * is unauthenticated — it validates the {@code access_token} request header against the configured
 * API key.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/cobrancas")
@RequiredArgsConstructor
public class CobrancaController {

  private final CobrancaService cobrancaService;
  private final AsaasProperties asaasProperties;

  /**
   * Generates charges for all apartment units of the given rateio execution.
   *
   * @param dto generation parameters (execucaoId, vencimento)
   * @param request current HTTP request (for requestId)
   * @return 201 Created with list of generated charges
   */
  @PostMapping("/gerar")
  @Timed(
      value = "cobranca.controller.gerar",
      description = "Generate charges for a rateio execution")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<CobrancaDTO>>> gerar(
      @RequestBody @Valid GerarCobrancasDTO dto, HttpServletRequest request) {
    log.info("POST /cobrancas/gerar execucaoId={}", dto.execucaoId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(requestId(request), cobrancaService.gerarCobrancas(dto)));
  }

  /**
   * Returns aggregate totals of pending and overdue charges for the current tenant.
   *
   * @param request current HTTP request (for requestId)
   * @return 200 OK with {@link com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO}
   */
  @GetMapping("/resumo")
  @Timed(value = "cobranca.controller.resumo", description = "Aggregate pending/overdue charges")
  public ResponseEntity<ApiResponse<com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO>> resumo(
      HttpServletRequest request) {
    log.info("GET /cobrancas/resumo");
    return ResponseEntity.ok(ApiResponse.of(requestId(request), cobrancaService.resumo()));
  }

  /**
   * Returns a paginated list of charges matching the supplied filter criteria.
   *
   * @param filter optional filter parameters
   * @param pageable pagination parameters
   * @param request current HTTP request (for requestId)
   * @return 200 OK with paginated charge list
   */
  @GetMapping
  @Timed(value = "cobranca.controller.filterBy", description = "List charges with filters")
  public ResponseEntity<ApiResponse<Page<CobrancaDTO>>> filterBy(
      @ModelAttribute CobrancaFilterDTO filter,
      @PageableDefault(size = 20) Pageable pageable,
      HttpServletRequest request) {
    log.info("GET /cobrancas filter={}", filter);
    return ResponseEntity.ok(
        ApiResponse.of(requestId(request), cobrancaService.filterBy(filter, pageable)));
  }

  /**
   * Returns the full detail of a single charge.
   *
   * @param id charge primary key
   * @param request current HTTP request (for requestId)
   * @return 200 OK with the charge DTO, or 404 if not found
   */
  @GetMapping("/{id}")
  @Timed(value = "cobranca.controller.findById", description = "Get charge by ID")
  public ResponseEntity<ApiResponse<CobrancaDTO>> findById(
      @PathVariable Long id, HttpServletRequest request) {
    log.info("GET /cobrancas/{}", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), cobrancaService.findById(id)));
  }

  /**
   * Cancels a charge and its corresponding Asaas payment.
   *
   * @param id charge primary key
   * @param dto cancellation payload with reason
   * @param request current HTTP request (for requestId)
   * @return 200 OK with the updated charge DTO, or 404 if not found
   */
  @PostMapping("/{id}/cancelar")
  @Timed(value = "cobranca.controller.cancelar", description = "Cancel a charge")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<CobrancaDTO>> cancelar(
      @PathVariable Long id,
      @RequestBody @Valid CancelarCobrancaDTO dto,
      HttpServletRequest request) {
    log.info("POST /cobrancas/{}/cancelar", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), cobrancaService.cancelar(id, dto)));
  }

  /**
   * Re-sends the billing email for a charge.
   *
   * @param id charge primary key
   * @param request current HTTP request
   * @return 204 No Content on success, or 404 if not found
   */
  @PostMapping("/{id}/reenviar-email")
  @Timed(value = "cobranca.controller.reenviarEmail", description = "Resend billing email")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> reenviarEmail(@PathVariable Long id, HttpServletRequest request) {
    log.info("POST /cobrancas/{}/reenviar-email", id);
    cobrancaService.reenviarEmail(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Returns a paginated list of charge summaries for the given apartment.
   *
   * @param apartamentoId apartment primary key
   * @param pageable pagination parameters (default: newest first)
   * @param request current HTTP request (for requestId)
   * @return 200 OK with paginated charge summary list
   */
  @GetMapping("/apartamentos/{apartamentoId}/cobrancas")
  @Timed(value = "cobranca.controller.porApartamento", description = "List charges by apartment")
  public ResponseEntity<ApiResponse<Page<CobrancaResumoDTO>>> porApartamento(
      @PathVariable Long apartamentoId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      HttpServletRequest request) {
    log.info("GET /cobrancas/apartamentos/{}/cobrancas", apartamentoId);
    return ResponseEntity.ok(
        ApiResponse.of(
            requestId(request), cobrancaService.porApartamento(apartamentoId, pageable)));
  }

  /**
   * Asaas payment webhook endpoint — no JWT authentication required.
   *
   * <p>Validates the incoming {@code access_token} header against the configured API key before
   * processing. Returns 403 if the token does not match.
   *
   * @param payload webhook body from Asaas
   * @param token value of the {@code access_token} header sent by Asaas
   * @return 200 OK on successful processing, 403 if the token is invalid
   */
  @PostMapping("/webhook")
  @Timed(value = "cobranca.controller.webhook", description = "Process Asaas payment webhook")
  public ResponseEntity<Void> webhook(
      @RequestBody AsaasWebhookPayload payload, @RequestHeader("access_token") String token) {
    if (!asaasProperties.apiKey().equals(token)) {
      log.warn("Webhook received with invalid token");
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    cobrancaService.processarWebhook(payload);
    return ResponseEntity.ok().build();
  }
}
