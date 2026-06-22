package com.pmrodrigues.condominio.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CondominioFilterDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.service.CondominioService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** REST controller exposing CRUD endpoints for condominium complexes ({@code /condominios}). */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/condominios")
@RequiredArgsConstructor
public class CondominioController {

  private final CondominioService condominioService;

  /**
   * Returns all condominios that match the given filter criteria.
   *
   * @param filter optional filter parameters (nome, cnpj)
   * @param request current HTTP request used to extract the correlation request ID
   * @return list of matching condominios wrapped in an {@link ApiResponse}
   */
  @GetMapping
  @Timed(value = "condominio.controller.findAll", description = "Find all condominios")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<List<CondominioDTO>>> findAll(
      @ModelAttribute CondominioFilterDTO filter, HttpServletRequest request) {
    log.info("GET /condominios - filter={}", filter);
    var result = condominioService.filterBy(filter);
    log.info("GET /condominios - returning {} condominios", result.size());
    return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
  }

  /**
   * Returns a single condominio by its identifier.
   *
   * @param id condominio primary key
   * @param request current HTTP request used to extract the correlation request ID
   * @return the found condominio wrapped in an {@link ApiResponse}
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   */
  @GetMapping("/{id}")
  @Timed(value = "condominio.controller.findById", description = "Find condominio by id")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<CondominioDTO>> findById(
      @PathVariable Long id, HttpServletRequest request) {
    log.info("GET /condominios/{} - finding condominio by id", id);
    var condominio =
        condominioService
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Condominio not found: " + id));
    log.info("GET /condominios/{} - condominio found", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), condominio));
  }

  /**
   * Creates a new condominio; requires ADMIN role.
   *
   * @param dto creation payload: nome, cnpj (validated), email (validated), endereco with 8-digit
   *     cep and numeric estado id
   * @param request current HTTP request used to extract the correlation request ID
   * @return the created condominio with HTTP 201
   */
  @PostMapping
  @Timed(value = "condominio.controller.create", description = "Create condominio")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<CondominioDTO>> create(
      @RequestBody @Valid CreateCondominioDTO dto, HttpServletRequest request) {
    log.info("POST /condominios - creating condominio with cnpj: {}", dto.cnpj());
    var created = condominioService.create(dto);
    log.info("POST /condominios - condominio created with id: {}", created.id());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(requestId(request), created));
  }

  /**
   * Updates an existing condominio; requires ADMIN role.
   *
   * @param id condominio primary key
   * @param dto updated condominio data
   * @param request current HTTP request used to extract the correlation request ID
   * @return the updated condominio wrapped in an {@link ApiResponse}
   */
  @PutMapping("/{id}")
  @Timed(value = "condominio.controller.update", description = "Update condominio")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<CondominioDTO>> update(
      @PathVariable Long id, @RequestBody @Valid CondominioDTO dto, HttpServletRequest request) {
    log.info("PUT /condominios/{} - updating condominio", id);
    var updated =
        condominioService.update(
            new CondominioDTO(
                id,
                dto.nome(),
                dto.cnpj(),
                dto.email(),
                dto.endereco(),
                dto.createdAt(),
                dto.updatedAt()));
    log.info("PUT /condominios/{} - condominio updated", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
  }

  /**
   * Soft-deletes a condominio by its identifier; requires ADMIN role.
   *
   * @param id condominio primary key
   * @return HTTP 204 No Content
   */
  @DeleteMapping("/{id}")
  @Timed(value = "condominio.controller.delete", description = "Delete condominio")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    log.info("DELETE /condominios/{} - deleting condominio", id);
    condominioService.delete(id);
    log.info("DELETE /condominios/{} - condominio deleted", id);
    return ResponseEntity.noContent().build();
  }
}
