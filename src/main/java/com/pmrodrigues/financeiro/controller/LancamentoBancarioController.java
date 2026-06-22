package com.pmrodrigues.financeiro.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.CreateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.LancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.LancamentoBancarioFilterDTO;
import com.pmrodrigues.financeiro.dto.UpdateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.service.LancamentoBancarioService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing CRUD endpoints for bank account entries (lancamentos bancários). */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/lancamentos-bancarios")
@RequiredArgsConstructor
public class LancamentoBancarioController {

  private final LancamentoBancarioService lancamentoBancarioService;

  /**
   * Returns a paginated list of lancamentos for the current condominium.
   *
   * @param filter optional filter parameters
   * @param pageable pagination parameters
   * @param request current HTTP request
   */
  @GetMapping
  @Timed(
      value = "lancamento.bancario.controller.findAll",
      description = "Find all lancamentos bancarios")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<Page<LancamentoBancarioDTO>>> findAll(
      @ModelAttribute LancamentoBancarioFilterDTO filter,
      Pageable pageable,
      HttpServletRequest request) {
    log.info("GET /lancamentos-bancarios - filter={}", filter);
    var result = lancamentoBancarioService.filterBy(filter, pageable);
    log.info("GET /lancamentos-bancarios - returning {} lancamentos", result.getTotalElements());
    return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
  }

  /**
   * Returns a single lancamento bancario by its primary key.
   *
   * @param id the lancamento identifier
   * @param request current HTTP request
   */
  @GetMapping("/{id}")
  @Timed(
      value = "lancamento.bancario.controller.findById",
      description = "Find lancamento bancario by id")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<LancamentoBancarioDTO>> findById(
      @PathVariable Long id, HttpServletRequest request) {
    log.info("GET /lancamentos-bancarios/{} - finding lancamento by id", id);
    var result = lancamentoBancarioService.findById(id);
    log.info("GET /lancamentos-bancarios/{} - lancamento found", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
  }

  /**
   * Creates a new lancamento bancario. Requires ADMIN role.
   *
   * @param dto creation payload
   * @param request current HTTP request
   */
  @PostMapping
  @Timed(
      value = "lancamento.bancario.controller.create",
      description = "Create lancamento bancario")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<LancamentoBancarioDTO>> create(
      @Valid @RequestBody CreateLancamentoBancarioDTO dto, HttpServletRequest request) {
    log.info("POST /lancamentos-bancarios - creating lancamento: tipo={}", dto.tipo());
    var created = lancamentoBancarioService.create(dto);
    log.info("POST /lancamentos-bancarios - lancamento created with id: {}", created.id());
    return ResponseEntity.created(URI.create("/lancamentos-bancarios/" + created.id()))
        .body(ApiResponse.of(requestId(request), created));
  }

  /**
   * Updates mutable fields of a lancamento bancario. Requires ADMIN role.
   *
   * @param id the lancamento identifier
   * @param dto update payload
   * @param request current HTTP request
   */
  @PutMapping("/{id}")
  @Timed(
      value = "lancamento.bancario.controller.update",
      description = "Update lancamento bancario")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<LancamentoBancarioDTO>> update(
      @PathVariable Long id,
      @Valid @RequestBody UpdateLancamentoBancarioDTO dto,
      HttpServletRequest request) {
    log.info("PUT /lancamentos-bancarios/{} - updating lancamento", id);
    var updated = lancamentoBancarioService.update(id, dto);
    log.info("PUT /lancamentos-bancarios/{} - lancamento updated", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
  }

  /**
   * Soft-deletes a lancamento bancario. Requires ADMIN role.
   *
   * @param id the lancamento identifier
   * @param request current HTTP request
   */
  @DeleteMapping("/{id}")
  @Timed(
      value = "lancamento.bancario.controller.delete",
      description = "Delete lancamento bancario")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    log.info("DELETE /lancamentos-bancarios/{} - deleting lancamento", id);
    lancamentoBancarioService.delete(id);
    log.info("DELETE /lancamentos-bancarios/{} - lancamento deleted", id);
    return ResponseEntity.noContent().build();
  }
}
