package com.pmrodrigues.financeiro.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.service.OrcamentoAnualService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller exposing CRUD, approval, and item management endpoints for annual budgets.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
public class OrcamentoAnualController {

    private final OrcamentoAnualService service;

    /**
     * Returns all budgets matching the given filter.
     */
    @GetMapping
    @Timed(value = "orcamento.controller.findAll", description = "Find all orcamentos")
    public ResponseEntity<ApiResponse<List<OrcamentoAnualDTO>>> findAll(
            @ModelAttribute OrcamentoAnualFilterDTO filter,
            HttpServletRequest request) {
        log.info("GET /orcamentos - filter={}", filter);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.filterBy(filter)));
    }

    /**
     * Returns a single budget by its identifier including items.
     */
    @GetMapping("/{id}")
    @Timed(value = "orcamento.controller.findById", description = "Find orcamento by id")
    public ResponseEntity<ApiResponse<OrcamentoAnualDTO>> findById(@PathVariable Long id,
                                                                    HttpServletRequest request) {
        log.info("GET /orcamentos/{}", id);
        return service.findById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.of(requestId(request), dto)))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "OrcamentoAnual not found: " + id));
    }

    /**
     * Creates a new budget in RASCUNHO state; requires ADMIN role.
     */
    @PostMapping
    @Timed(value = "orcamento.controller.create", description = "Create orcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrcamentoAnualDTO>> create(@RequestBody @Valid CreateOrcamentoAnualDTO dto,
                                                                  HttpServletRequest request) {
        log.info("POST /orcamentos - exercicio={}", dto.exercicio());
        var created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates a RASCUNHO budget; requires ADMIN role.
     */
    @PutMapping("/{id}")
    @Timed(value = "orcamento.controller.update", description = "Update orcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrcamentoAnualDTO>> update(@PathVariable Long id,
                                                                  @RequestBody @Valid CreateOrcamentoAnualDTO dto,
                                                                  HttpServletRequest request) {
        log.info("PUT /orcamentos/{}", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.update(id, dto)));
    }

    /**
     * Approves a RASCUNHO budget and computes the estimated monthly fee; requires ADMIN role.
     */
    @PatchMapping("/{id}/aprovar")
    @Timed(value = "orcamento.controller.aprovar", description = "Aprovar orcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrcamentoAnualDTO>> aprovar(@PathVariable Long id,
                                                                   @RequestBody @Valid AprovarOrcamentoDTO dto,
                                                                   HttpServletRequest request) {
        log.info("PATCH /orcamentos/{}/aprovar", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.aprovar(id, dto)));
    }

    /**
     * Closes an APROVADO budget; requires ADMIN role.
     */
    @PatchMapping("/{id}/encerrar")
    @Timed(value = "orcamento.controller.encerrar", description = "Encerrar orcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrcamentoAnualDTO>> encerrar(@PathVariable Long id,
                                                                    HttpServletRequest request) {
        log.info("PATCH /orcamentos/{}/encerrar", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.encerrar(id)));
    }

    /**
     * Soft-deletes a RASCUNHO budget and its items; requires ADMIN role.
     */
    @DeleteMapping("/{id}")
    @Timed(value = "orcamento.controller.delete", description = "Delete orcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /orcamentos/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a line item to a RASCUNHO budget; requires ADMIN role.
     */
    @PostMapping("/{id}/itens")
    @Timed(value = "orcamento.controller.addItem", description = "Add item to orcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemOrcamentoDTO>> addItem(@PathVariable Long id,
                                                                  @RequestBody @Valid CreateItemOrcamentoDTO dto,
                                                                  HttpServletRequest request) {
        log.info("POST /orcamentos/{}/itens", id);
        var item = service.addItem(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), item));
    }

    /**
     * Updates a line item's predicted value in a RASCUNHO budget; requires ADMIN role.
     */
    @PutMapping("/{id}/itens/{itemId}")
    @Timed(value = "orcamento.controller.updateItem", description = "Update orcamento item")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemOrcamentoDTO>> updateItem(@PathVariable Long id,
                                                                     @PathVariable Long itemId,
                                                                     @RequestBody @Valid CreateItemOrcamentoDTO dto,
                                                                     HttpServletRequest request) {
        log.info("PUT /orcamentos/{}/itens/{}", id, itemId);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.updateItem(id, itemId, dto)));
    }

    /**
     * Soft-deletes a line item from a RASCUNHO budget; requires ADMIN role.
     */
    @DeleteMapping("/{id}/itens/{itemId}")
    @Timed(value = "orcamento.controller.deleteItem", description = "Delete orcamento item")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        log.info("DELETE /orcamentos/{}/itens/{}", id, itemId);
        service.deleteItem(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
