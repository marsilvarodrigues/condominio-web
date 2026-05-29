package com.pmrodrigues.commons.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.dto.BancoDTO;
import com.pmrodrigues.commons.dto.BancoFilterDTO;
import com.pmrodrigues.commons.dto.CreateBancoDTO;
import com.pmrodrigues.commons.dto.UpdateBancoDTO;
import com.pmrodrigues.commons.service.BancoService;
import com.pmrodrigues.commons.versioning.ApiVersion;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller exposing CRUD endpoints for {@link com.pmrodrigues.commons.model.Banco}.
 * Read operations are public; write operations require the ADMIN role.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/bancos")
@RequiredArgsConstructor
public class BancoController {

    private final BancoService bancoService;

    /**
     * Returns a list of bancos matching optional filter parameters.
     *
     * @param filter optional filter (codigo prefix, nome substring)
     * @param request current HTTP request
     */
    @GetMapping
    @Timed(value = "banco.controller.findAll", description = "Find all bancos")
    public ResponseEntity<ApiResponse<List<BancoDTO>>> findAll(
            @ModelAttribute BancoFilterDTO filter,
            HttpServletRequest request) {
        log.info("GET /bancos - filter={}", filter);
        var result = bancoService.filterBy(filter);
        log.info("GET /bancos - returning {} bancos", result.size());
        return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
    }

    /**
     * Returns a single banco by its primary key.
     *
     * @param id the banco identifier
     * @param request current HTTP request
     */
    @GetMapping("/{id}")
    @Timed(value = "banco.controller.findById", description = "Find banco by id")
    public ResponseEntity<ApiResponse<BancoDTO>> findById(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /bancos/{} - finding banco by id", id);
        var banco = bancoService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Banco not found: " + id));
        log.info("GET /bancos/{} - banco found", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), banco));
    }

    /**
     * Creates a new banco. Requires ADMIN role.
     *
     * @param dto creation payload
     * @param request current HTTP request
     */
    @PostMapping
    @Timed(value = "banco.controller.create", description = "Create banco")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BancoDTO>> create(
            @Valid @RequestBody CreateBancoDTO dto,
            HttpServletRequest request) {
        log.info("POST /bancos - creating banco: {}", dto.codigo());
        var created = bancoService.create(dto);
        log.info("POST /bancos - banco created with id: {}", created.id());
        return ResponseEntity.created(URI.create("/bancos/" + created.id()))
                .body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates an existing banco. Requires ADMIN role.
     *
     * @param id the banco identifier
     * @param dto update payload
     * @param request current HTTP request
     */
    @PutMapping("/{id}")
    @Timed(value = "banco.controller.update", description = "Update banco")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BancoDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBancoDTO dto,
            HttpServletRequest request) {
        log.info("PUT /bancos/{} - updating banco", id);
        var updated = bancoService.update(id, dto);
        log.info("PUT /bancos/{} - banco updated", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Soft-deletes a banco. Requires ADMIN role.
     *
     * @param id the banco identifier
     * @param request current HTTP request
     */
    @DeleteMapping("/{id}")
    @Timed(value = "banco.controller.delete", description = "Delete banco")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        log.info("DELETE /bancos/{} - deleting banco", id);
        bancoService.delete(id);
        log.info("DELETE /bancos/{} - banco deleted", id);
        return ResponseEntity.noContent().build();
    }
}
