package com.pmrodrigues.morador.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.morador.dto.CreatePessoaDTO;
import com.pmrodrigues.morador.dto.PessoaDTO;
import com.pmrodrigues.morador.dto.PessoaFilterDTO;
import com.pmrodrigues.morador.dto.UpdatePessoaDTO;
import com.pmrodrigues.morador.service.PessoaService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

/**
 * REST controller exposing CRUD endpoints for pessoas ({@code /pessoas}).
 * Also exposes apartment assignment sub-resources at {@code /pessoas/{id}/apartamento}.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaService pessoaService;

    /**
     * Returns a paginated list of pessoas matching the given filter criteria.
     *
     * @param filter   optional filter parameters (nome, tipo, cpf, cnpj, email)
     * @param pageable pagination and sort parameters
     * @param request  current HTTP request used to extract the correlation request ID
     * @return paginated list of matching pessoas wrapped in an {@link ApiResponse}
     */
    @GetMapping
    @Timed(value = "pessoa.controller.filterBy", description = "Filter pessoas")
    public ResponseEntity<ApiResponse<Page<PessoaDTO>>> filterBy(
            @ModelAttribute PessoaFilterDTO filter,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        log.info("GET /pessoas - filter={}", filter);
        var result = pessoaService.filterBy(filter, pageable);
        log.info("GET /pessoas - returning {} pessoas", result.getNumberOfElements());
        return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
    }

    /**
     * Returns a single pessoa by its identifier.
     *
     * @param id      pessoa primary key
     * @param request current HTTP request used to extract the correlation request ID
     * @return the found pessoa wrapped in an {@link ApiResponse}
     * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
     */
    @GetMapping("/{id}")
    @Timed(value = "pessoa.controller.findById", description = "Find pessoa by id")
    public ResponseEntity<ApiResponse<PessoaDTO>> findById(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /pessoas/{} - finding pessoa by id", id);
        var dto = pessoaService.findById(id);
        log.info("GET /pessoas/{} - pessoa found", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), dto));
    }

    /**
     * Creates a new pessoa of the sub-type indicated by {@code dto.tipo()}; requires ADMIN role.
     *
     * @param dto     creation payload (tipo = PF or PJ)
     * @param request current HTTP request used to extract the correlation request ID
     * @return the created pessoa with HTTP 201
     */
    @PostMapping
    @Timed(value = "pessoa.controller.create", description = "Create pessoa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PessoaDTO>> create(@RequestBody @Valid CreatePessoaDTO dto,
                                                          HttpServletRequest request) {
        log.info("POST /pessoas - creating pessoa: nome={}", dto.nome());
        var created = pessoaService.create(dto);
        log.info("POST /pessoas - pessoa created with id: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates an existing pessoa; requires ADMIN role.
     *
     * @param id      pessoa primary key
     * @param dto     updated pessoa data
     * @param request current HTTP request used to extract the correlation request ID
     * @return the updated pessoa wrapped in an {@link ApiResponse}
     */
    @PutMapping("/{id}")
    @Timed(value = "pessoa.controller.update", description = "Update pessoa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PessoaDTO>> update(@PathVariable Long id,
                                                          @RequestBody @Valid UpdatePessoaDTO dto,
                                                          HttpServletRequest request) {
        log.info("PUT /pessoas/{} - updating pessoa", id);
        var updated = pessoaService.update(id, dto);
        log.info("PUT /pessoas/{} - pessoa updated", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Soft-deletes a pessoa by its identifier; requires ADMIN role.
     *
     * @param id pessoa primary key
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Timed(value = "pessoa.controller.delete", description = "Delete pessoa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /pessoas/{} - deleting pessoa", id);
        pessoaService.delete(id);
        log.info("DELETE /pessoas/{} - pessoa deleted", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigns the pessoa as morador to the given apartment; requires ADMIN role.
     *
     * @param id             pessoa primary key
     * @param apartamentoId  apartment primary key (request body JSON field)
     * @param request        current HTTP request
     * @return the updated pessoa wrapped in an {@link ApiResponse}
     */
    @PostMapping("/{id}/apartamento")
    @Timed(value = "pessoa.controller.assignToApartamento", description = "Assign pessoa to apartamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PessoaDTO>> assignToApartamento(
            @PathVariable Long id,
            @RequestParam Long apartamentoId,
            HttpServletRequest request) {
        log.info("POST /pessoas/{}/apartamento - assigning to apartamento {}", id, apartamentoId);
        var updated = pessoaService.assignToApartamento(id, apartamentoId);
        log.info("POST /pessoas/{}/apartamento - assigned", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Removes the pessoa from their current apartment assignment; requires ADMIN role.
     *
     * @param id      pessoa primary key
     * @param request current HTTP request
     * @return the updated pessoa wrapped in an {@link ApiResponse}
     */
    @DeleteMapping("/{id}/apartamento")
    @Timed(value = "pessoa.controller.removeFromApartamento", description = "Remove pessoa from apartamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PessoaDTO>> removeFromApartamento(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("DELETE /pessoas/{}/apartamento - removing from apartamento", id);
        var updated = pessoaService.removeFromApartamento(id);
        log.info("DELETE /pessoas/{}/apartamento - removed", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }
}
