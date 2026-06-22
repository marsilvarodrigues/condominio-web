package com.pmrodrigues.financeiro.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.CreateGrupoDespesaDTO;
import com.pmrodrigues.financeiro.dto.GrupoDespesaDTO;
import com.pmrodrigues.financeiro.dto.GrupoDespesaFilterDTO;
import com.pmrodrigues.financeiro.service.GrupoDespesaService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * REST controller exposing CRUD endpoints for {@link
 * com.pmrodrigues.financeiro.model.GrupoDespesa}.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/grupos-despesa")
@RequiredArgsConstructor
public class GrupoDespesaController {

  private final GrupoDespesaService service;

  /**
   * Returns all expense groups matching the given filter criteria.
   *
   * @param filter optional filter (tipoRateio, escopo)
   * @param request current HTTP request
   */
  @GetMapping
  @Timed(value = "grupo.despesa.controller.findAll", description = "List grupos de despesa")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<List<GrupoDespesaDTO>>> findAll(
      @ModelAttribute GrupoDespesaFilterDTO filter, HttpServletRequest request) {
    log.info("GET /grupos-despesa - filter={}", filter);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), service.filterBy(filter)));
  }

  /**
   * Returns a single expense group by its identifier.
   *
   * @param id the group identifier
   * @param request current HTTP request
   */
  @GetMapping("/{id}")
  @Timed(value = "grupo.despesa.controller.findById", description = "Find grupo de despesa by id")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<GrupoDespesaDTO>> findById(
      @PathVariable Long id, HttpServletRequest request) {
    log.info("GET /grupos-despesa/{}", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), service.findById(id)));
  }

  /**
   * Creates a new expense group. Requires ADMIN role.
   *
   * @param dto creation payload
   * @param request current HTTP request
   */
  @PostMapping
  @Timed(value = "grupo.despesa.controller.create", description = "Create grupo de despesa")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<GrupoDespesaDTO>> create(
      @Valid @RequestBody CreateGrupoDespesaDTO dto, HttpServletRequest request) {
    log.info("POST /grupos-despesa - nome={}", dto.nome());
    var created = service.create(dto);
    return ResponseEntity.created(URI.create("/grupos-despesa/" + created.id()))
        .body(ApiResponse.of(requestId(request), created));
  }

  /**
   * Updates an existing expense group. Requires ADMIN role.
   *
   * @param id the group identifier
   * @param dto update payload
   * @param request current HTTP request
   */
  @PutMapping("/{id}")
  @Timed(value = "grupo.despesa.controller.update", description = "Update grupo de despesa")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<GrupoDespesaDTO>> update(
      @PathVariable Long id, @Valid @RequestBody GrupoDespesaDTO dto, HttpServletRequest request) {
    log.info("PUT /grupos-despesa/{}", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), service.update(id, dto)));
  }

  /**
   * Soft-deletes an expense group. Requires ADMIN role.
   *
   * @param id the group identifier
   * @param request current HTTP request
   */
  @DeleteMapping("/{id}")
  @Timed(value = "grupo.despesa.controller.delete", description = "Delete grupo de despesa")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    log.info("DELETE /grupos-despesa/{}", id);
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
