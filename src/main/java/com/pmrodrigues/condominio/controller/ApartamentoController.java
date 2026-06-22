package com.pmrodrigues.condominio.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.ApartamentoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.service.ApartamentoService;
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

/** REST controller exposing CRUD endpoints for apartment units ({@code /apartamentos}). */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/apartamentos")
@RequiredArgsConstructor
public class ApartamentoController {

  private final ApartamentoService apartamentoService;

  /**
   * Returns all apartments that match the given filter criteria.
   *
   * @param filter optional filter parameters (bloco, numero)
   * @param request current HTTP request used to extract the correlation request ID
   * @return paginated list of matching apartments wrapped in an {@link ApiResponse}
   */
  @GetMapping
  @Timed(value = "apartamento.controller.findAll", description = "Find all apartamentos")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO','PROPRIETARIO','MORADOR')")
  public ResponseEntity<ApiResponse<List<ApartamentoDTO>>> findAll(
      @ModelAttribute ApartamentoFilterDTO filter, HttpServletRequest request) {
    log.info("GET /apartamentos - filter={}", filter);
    var result = apartamentoService.filterBy(filter);
    log.info("GET /apartamentos - returning {} apartamentos", result.size());
    return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
  }

  /**
   * Returns a single apartment by its identifier.
   *
   * @param id apartment primary key
   * @param request current HTTP request used to extract the correlation request ID
   * @return the found apartment wrapped in an {@link ApiResponse}
   * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
   */
  @GetMapping("/{id}")
  @Timed(value = "apartamento.controller.findById", description = "Find apartamento by id")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO','PROPRIETARIO','MORADOR')")
  public ResponseEntity<ApiResponse<ApartamentoDTO>> findById(
      @PathVariable Long id, HttpServletRequest request) {
    log.info("GET /apartamentos/{} - finding apartamento by id", id);
    var apartamentoDto =
        apartamentoService
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Apartamento not found: " + id));
    log.info("GET /apartamentos/{} - apartamento found", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), apartamentoDto));
  }

  /**
   * Creates a new apartment; requires ADMIN role.
   *
   * @param dto creation payload containing blocoId and numero
   * @param request current HTTP request used to extract the correlation request ID
   * @return the created apartment with HTTP 201
   */
  @PostMapping
  @Timed(value = "apartamento.controller.create", description = "Create apartamento")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<ApartamentoDTO>> create(
      @RequestBody @Valid CreateApartamentoDTO dto, HttpServletRequest request) {
    log.info(
        "POST /apartamentos - creating apartamento: bloco={}, numero={}",
        dto.blocoId(),
        dto.numero());
    var created = apartamentoService.create(dto);
    log.info("POST /apartamentos - apartamento created with id: {}", created.id());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(requestId(request), created));
  }

  /**
   * Updates an existing apartment; requires ADMIN role.
   *
   * @param id apartment primary key
   * @param dto updated apartment data
   * @param request current HTTP request used to extract the correlation request ID
   * @return the updated apartment wrapped in an {@link ApiResponse}
   */
  @PutMapping("/{id}")
  @Timed(value = "apartamento.controller.update", description = "Update apartamento")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<ApartamentoDTO>> update(
      @PathVariable Long id, @RequestBody @Valid ApartamentoDTO dto, HttpServletRequest request) {
    log.info("PUT /apartamentos/{} - updating apartamento", id);
    var updated =
        apartamentoService.update(
            new ApartamentoDTO(
                id,
                dto.blocoId(),
                dto.blocoNome(),
                dto.numero(),
                dto.createdAt(),
                dto.updatedAt(),
                dto.areaConstruida(),
                dto.fracaoIdeal(),
                dto.andar(),
                0));
    log.info("PUT /apartamentos/{} - apartamento updated", id);
    return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
  }

  /**
   * Soft-deletes an apartment by its identifier; requires ADMIN role.
   *
   * @param id apartment primary key
   * @return HTTP 204 No Content
   */
  @DeleteMapping("/{id}")
  @Timed(value = "apartamento.controller.delete", description = "Delete apartamento")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    log.info("DELETE /apartamentos/{} - deleting apartamento", id);
    apartamentoService.delete(id);
    log.info("DELETE /apartamentos/{} - apartamento deleted", id);
    return ResponseEntity.noContent().build();
  }
}
