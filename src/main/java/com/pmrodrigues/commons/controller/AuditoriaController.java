package com.pmrodrigues.commons.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.dto.RevisaoDTO;
import com.pmrodrigues.commons.service.AuditoriaService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing audit history endpoints for whitelisted entities. The entity whitelist is
 * enforced in {@link AuditoriaService} — this controller never accepts arbitrary class names.
 *
 * <p>Requires {@code ADMIN} or {@code SINDICO} role.
 */
@Slf4j
@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {

  private final AuditoriaService auditoriaService;

  /**
   * Constructs the controller with the auditoria service.
   *
   * @param auditoriaService service that queries Envers revision history
   */
  public AuditoriaController(AuditoriaService auditoriaService) {
    this.auditoriaService = auditoriaService;
  }

  /**
   * Returns a paginated audit history for a whitelisted entity instance, sorted most-recent first.
   *
   * @param entidade path segment identifying the entity type (e.g., {@code "cobrancas"})
   * @param id       primary key of the entity
   * @param pageable pagination parameters (default page size 20)
   * @param request  current HTTP request (used to extract the request ID)
   * @return page of {@link RevisaoDTO} sorted by revision number descending, or 400 if the entity
   *         type is not supported
   */
  @GetMapping("/{entidade}/{id}")
  @Timed(value = "auditoria.controller.historico", description = "Get audit history for entity")
  @PreAuthorize("hasAnyRole('ADMIN', 'SINDICO')")
  public ResponseEntity<ApiResponse<Page<RevisaoDTO>>> historico(
      @PathVariable String entidade,
      @PathVariable Long id,
      @PageableDefault(size = 20) Pageable pageable,
      HttpServletRequest request) {
    log.info("GET /auditoria/{}/{} page={}", entidade, id, pageable.getPageNumber());
    var result = auditoriaService.historico(entidade, id, pageable);
    log.info("GET /auditoria/{}/{} - {} revisões (total={})", entidade, id,
        result.getNumberOfElements(), result.getTotalElements());
    return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
  }
}
