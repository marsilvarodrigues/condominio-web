package com.pmrodrigues.financeiro.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.CreatePlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasFilterDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasNodeDTO;
import com.pmrodrigues.financeiro.service.PlanoContasService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller exposing CRUD and tree endpoints for the chart of accounts.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/plano-contas")
@RequiredArgsConstructor
public class PlanoContasController {

    private final PlanoContasService service;

    /**
     * Returns all PlanoContas nodes matching the given filter criteria.
     */
    @GetMapping
    @Timed(value = "planocontas.controller.findAll", description = "Find all plano de contas")
    public ResponseEntity<ApiResponse<List<PlanoContasDTO>>> findAll(
            @ModelAttribute PlanoContasFilterDTO filter,
            HttpServletRequest request) {
        log.info("GET /plano-contas - filter={}", filter);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.filterBy(filter)));
    }

    /**
     * Returns the full chart-of-accounts tree from root nodes.
     */
    @GetMapping("/arvore")
    @Timed(value = "planocontas.controller.arvore", description = "Get plano de contas tree")
    public ResponseEntity<ApiResponse<List<PlanoContasNodeDTO>>> getArvore(HttpServletRequest request) {
        log.info("GET /plano-contas/arvore");
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.getArvore()));
    }

    /**
     * Returns a single PlanoContas node by its identifier.
     */
    @GetMapping("/{id}")
    @Timed(value = "planocontas.controller.findById", description = "Find plano de contas by id")
    public ResponseEntity<ApiResponse<PlanoContasDTO>> findById(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /plano-contas/{}", id);
        return service.findById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.of(requestId(request), dto)))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(NOT_FOUND, "PlanoContas not found: " + id));
    }

    /**
     * Creates a new PlanoContas node; requires ADMIN role.
     */
    @PostMapping
    @Timed(value = "planocontas.controller.create", description = "Create plano de contas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlanoContasDTO>> create(@RequestBody @Valid CreatePlanoContasDTO dto,
                                                               HttpServletRequest request) {
        log.info("POST /plano-contas - codigo={}", dto.codigo());
        var created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates an existing PlanoContas node; requires ADMIN role.
     */
    @PutMapping("/{id}")
    @Timed(value = "planocontas.controller.update", description = "Update plano de contas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlanoContasDTO>> update(@PathVariable Long id,
                                                               @RequestBody @Valid PlanoContasDTO dto,
                                                               HttpServletRequest request) {
        log.info("PUT /plano-contas/{}", id);
        var updated = service.update(new PlanoContasDTO(id, dto.codigo(), dto.descricao(), dto.tipo(),
                dto.tipoRateio(), dto.escopoRateio(), dto.paiId(), dto.createdAt(), dto.updatedAt()));
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Soft-deletes a PlanoContas node; requires ADMIN role.
     */
    @DeleteMapping("/{id}")
    @Timed(value = "planocontas.controller.delete", description = "Delete plano de contas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /plano-contas/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
