package com.pmrodrigues.morador.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.morador.dto.CreateProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioFilterDTO;
import com.pmrodrigues.morador.dto.UpdateProprietarioDTO;
import com.pmrodrigues.morador.service.ProprietarioService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD endpoints for proprietários ({@code /proprietarios}) and apartment
 * association sub-resources.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequiredArgsConstructor
public class ProprietarioController {

  private final ProprietarioService proprietarioService;

  /**
   * Returns a paginated list of proprietarios, optionally filtered.
   *
   * @param filter optional filter parameters
   * @param pageable pagination and sort parameters
   * @param request current HTTP request
   * @return page of proprietario DTOs wrapped in an {@link ApiResponse}
   */
  @GetMapping("/proprietarios")
  @Timed(value = "proprietario.controller.filterBy", description = "Filter proprietarios")
  public ResponseEntity<ApiResponse<Page<ProprietarioDTO>>> filterBy(
      @ModelAttribute ProprietarioFilterDTO filter,
      @PageableDefault(size = 20) Pageable pageable,
      HttpServletRequest request) {
    log.info("GET /proprietarios - filter={}", filter);
    var result = proprietarioService.filterBy(filter, pageable);
    log.info("GET /proprietarios - returning {} proprietarios", result.getNumberOfElements());
    return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
  }

  /**
   * Returns a single proprietario by its identifier.
   *
   * @param id proprietario primary key
   * @param request current HTTP request
   * @return the found proprietario wrapped in an {@link ApiResponse}
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   */
  @GetMapping("/proprietarios/{id}")
  @Timed(value = "proprietario.controller.findById", description = "Find proprietario by id")
  public ResponseEntity<ApiResponse<ProprietarioDTO>> findById(
      @PathVariable Long id, HttpServletRequest request) {
    log.info("GET /proprietarios/{} - finding proprietario", id);
    var dto = proprietarioService.findById(id);
    log.info("GET /proprietarios/{} - found", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), dto));
  }

  /**
   * Creates a new proprietario; requires ADMIN role.
   *
   * @param dto creation payload (tipo = PROP_PF or PROP_PJ)
   * @param request current HTTP request
   * @return the created proprietario with HTTP 201
   */
  @PostMapping("/proprietarios")
  @Timed(value = "proprietario.controller.create", description = "Create proprietario")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ProprietarioDTO>> create(
      @RequestBody @Valid CreateProprietarioDTO dto, HttpServletRequest request) {
    log.info(
        "POST /proprietarios - creating proprietario: nome={}, tipo={}", dto.nome(), dto.tipo());
    var created = proprietarioService.create(dto);
    log.info("POST /proprietarios - created with id: {}", created.id());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(requestId(request), created));
  }

  /**
   * Updates the mutable fields of an existing proprietario; requires ADMIN role. The tipo
   * discriminator cannot be changed after creation.
   *
   * @param id proprietario primary key
   * @param dto partial-update payload (null fields are ignored)
   * @param request current HTTP request
   * @return the updated proprietario wrapped in an {@link ApiResponse}
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   */
  @PutMapping("/proprietarios/{id}")
  @Timed(value = "proprietario.controller.update", description = "Update proprietario")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ProprietarioDTO>> update(
      @PathVariable Long id, @RequestBody UpdateProprietarioDTO dto, HttpServletRequest request) {
    log.info("PUT /proprietarios/{} - updating proprietario", id);
    var updated = proprietarioService.update(id, dto);
    log.info("PUT /proprietarios/{} - updated", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
  }

  /**
   * Associates a proprietario with an apartment; requires ADMIN role.
   *
   * @param id proprietario primary key
   * @param apartamentoId apartment primary key (query parameter)
   * @param request current HTTP request
   * @return the updated proprietario wrapped in an {@link ApiResponse}
   */
  @PostMapping("/proprietarios/{id}/apartamentos")
  @Timed(
      value = "proprietario.controller.associarApartamento",
      description = "Associate proprietario with apartamento")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ProprietarioDTO>> associarApartamento(
      @PathVariable Long id, @RequestParam Long apartamentoId, HttpServletRequest request) {
    log.info(
        "POST /proprietarios/{}/apartamentos - associating with apartamento {}", id, apartamentoId);
    var updated = proprietarioService.associarApartamento(id, apartamentoId);
    log.info("POST /proprietarios/{}/apartamentos - associated", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
  }

  /**
   * Dissociates a proprietario from a specific apartment; requires ADMIN role.
   *
   * @param id proprietario primary key
   * @param aptId apartment primary key
   * @param request current HTTP request
   * @return the updated proprietario wrapped in an {@link ApiResponse}
   */
  @DeleteMapping("/proprietarios/{id}/apartamentos/{aptId}")
  @Timed(
      value = "proprietario.controller.desassociarApartamento",
      description = "Dissociate proprietario from apartamento")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ProprietarioDTO>> desassociarApartamento(
      @PathVariable Long id, @PathVariable Long aptId, HttpServletRequest request) {
    log.info("DELETE /proprietarios/{}/apartamentos/{} - dissociating", id, aptId);
    var updated = proprietarioService.desassociarApartamento(id, aptId);
    log.info("DELETE /proprietarios/{}/apartamentos/{} - dissociated", id, aptId);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
  }

  /**
   * Soft-deletes a proprietario; requires ADMIN role.
   *
   * @param id proprietario primary key
   * @return HTTP 204 No Content
   */
  @DeleteMapping("/proprietarios/{id}")
  @Timed(value = "proprietario.controller.delete", description = "Delete proprietario")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    log.info("DELETE /proprietarios/{} - deleting", id);
    proprietarioService.delete(id);
    log.info("DELETE /proprietarios/{} - deleted", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Returns the list of proprietarios for a given apartment.
   *
   * @param aptId apartment primary key
   * @param request current HTTP request
   * @return list of proprietario DTOs wrapped in an {@link ApiResponse}
   */
  @GetMapping("/apartamentos/{aptId}/proprietarios")
  @Timed(
      value = "proprietario.controller.findByApartamento",
      description = "List proprietarios for apartamento")
  public ResponseEntity<ApiResponse<List<ProprietarioDTO>>> findByApartamento(
      @PathVariable Long aptId, HttpServletRequest request) {
    log.info("GET /apartamentos/{}/proprietarios", aptId);
    var result = proprietarioService.findByApartamento(aptId);
    log.info("GET /apartamentos/{}/proprietarios - {} results", aptId, result.size());
    return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
  }
}
