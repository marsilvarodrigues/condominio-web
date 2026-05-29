package com.pmrodrigues.financeiro.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.financeiro.dto.ContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.ContaBancariaFilterDTO;
import com.pmrodrigues.financeiro.dto.CreateContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.UpdateContaBancariaDTO;
import com.pmrodrigues.financeiro.service.ContaBancariaService;
import com.pmrodrigues.commons.versioning.ApiVersion;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

/**
 * REST controller exposing CRUD endpoints for condominium bank accounts.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/contas-bancarias")
@RequiredArgsConstructor
public class ContaBancariaController {

    private final ContaBancariaService contaBancariaService;

    /**
     * Returns a paginated list of bank accounts for the current condominium.
     *
     * @param filter optional filter parameters
     * @param pageable pagination parameters
     * @param request current HTTP request
     */
    @GetMapping
    @Timed(value = "conta.bancaria.controller.findAll", description = "Find all contas bancarias")
    public ResponseEntity<ApiResponse<Page<ContaBancariaDTO>>> findAll(
            @ModelAttribute ContaBancariaFilterDTO filter,
            Pageable pageable,
            HttpServletRequest request) {
        log.info("GET /contas-bancarias - filter={}", filter);
        var result = contaBancariaService.filterBy(filter, pageable);
        log.info("GET /contas-bancarias - returning {} contas", result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
    }

    /**
     * Returns a single bank account by its primary key.
     *
     * @param id the conta bancaria identifier
     * @param request current HTTP request
     */
    @GetMapping("/{id}")
    @Timed(value = "conta.bancaria.controller.findById", description = "Find conta bancaria by id")
    public ResponseEntity<ApiResponse<ContaBancariaDTO>> findById(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /contas-bancarias/{} - finding conta bancaria by id", id);
        var result = contaBancariaService.findById(id);
        log.info("GET /contas-bancarias/{} - conta bancaria found", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
    }

    /**
     * Creates a new bank account for the current condominium. Requires ADMIN role.
     *
     * @param dto creation payload
     * @param request current HTTP request
     */
    @PostMapping
    @Timed(value = "conta.bancaria.controller.create", description = "Create conta bancaria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContaBancariaDTO>> create(
            @Valid @RequestBody CreateContaBancariaDTO dto,
            HttpServletRequest request) {
        log.info("POST /contas-bancarias - creating conta: tipo={}", dto.tipo());
        var created = contaBancariaService.create(dto);
        log.info("POST /contas-bancarias - conta created with id: {}", created.id());
        return ResponseEntity.created(URI.create("/contas-bancarias/" + created.id()))
                .body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates mutable fields of a bank account. Requires ADMIN role.
     *
     * @param id the conta bancaria identifier
     * @param dto update payload
     * @param request current HTTP request
     */
    @PutMapping("/{id}")
    @Timed(value = "conta.bancaria.controller.update", description = "Update conta bancaria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContaBancariaDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContaBancariaDTO dto,
            HttpServletRequest request) {
        log.info("PUT /contas-bancarias/{} - updating conta bancaria", id);
        var updated = contaBancariaService.update(id, dto);
        log.info("PUT /contas-bancarias/{} - conta bancaria updated", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Activates a bank account. Requires ADMIN role.
     *
     * @param id the conta bancaria identifier
     * @param request current HTTP request
     */
    @PatchMapping("/{id}/ativar")
    @Timed(value = "conta.bancaria.controller.ativar", description = "Activate conta bancaria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContaBancariaDTO>> ativar(@PathVariable Long id, HttpServletRequest request) {
        log.info("PATCH /contas-bancarias/{}/ativar", id);
        var updated = contaBancariaService.setAtiva(id, true);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Deactivates a bank account. Requires ADMIN role.
     *
     * @param id the conta bancaria identifier
     * @param request current HTTP request
     */
    @PatchMapping("/{id}/desativar")
    @Timed(value = "conta.bancaria.controller.desativar", description = "Deactivate conta bancaria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContaBancariaDTO>> desativar(@PathVariable Long id, HttpServletRequest request) {
        log.info("PATCH /contas-bancarias/{}/desativar", id);
        var updated = contaBancariaService.setAtiva(id, false);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Soft-deletes a bank account. Requires ADMIN role.
     *
     * @param id the conta bancaria identifier
     * @param request current HTTP request
     */
    @DeleteMapping("/{id}")
    @Timed(value = "conta.bancaria.controller.delete", description = "Delete conta bancaria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        log.info("DELETE /contas-bancarias/{} - deleting conta bancaria", id);
        contaBancariaService.delete(id);
        log.info("DELETE /contas-bancarias/{} - conta bancaria deleted", id);
        return ResponseEntity.noContent().build();
    }
}
