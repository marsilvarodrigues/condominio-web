package com.pmrodrigues.condominio.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.BlocoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.service.BlocoService;
import com.pmrodrigues.commons.versioning.ApiVersion;
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
 * REST controller exposing CRUD endpoints for building blocks ({@code /blocos}).
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/blocos")
@RequiredArgsConstructor
public class BlocoController {

    private final BlocoService blocoService;

    /**
     * Returns all blocos that match the given filter criteria.
     *
     * @param filter optional filter parameters (bloco name)
     * @param request current HTTP request used to extract the correlation request ID
     * @return list of matching blocos wrapped in an {@link ApiResponse}
     */
    @GetMapping
    @Timed(value = "bloco.controller.findAll", description = "Find all blocos")
    public ResponseEntity<ApiResponse<List<BlocoDTO>>> findAll(
            @ModelAttribute BlocoFilterDTO filter,
            HttpServletRequest request) {
        log.info("GET /blocos - filter={}", filter);
        var result = blocoService.filterBy(filter);
        log.info("GET /blocos - returning {} blocos", result.size());
        return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
    }

    /**
     * Returns a single bloco by its identifier.
     *
     * @param id bloco primary key
     * @param request current HTTP request used to extract the correlation request ID
     * @return the found bloco wrapped in an {@link ApiResponse}
     * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
     */
    @GetMapping("/{id}")
    @Timed(value = "bloco.controller.findById", description = "Find bloco by id")
    public ResponseEntity<ApiResponse<BlocoDTO>> findById(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /blocos/{} - finding bloco by id", id);
        var blocoDTO = blocoService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Bloco not found: " + id));
        log.info("GET /blocos/{} - bloco found", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), blocoDTO));
    }

    /**
     * Creates a new bloco; requires ADMIN role.
     *
     * @param dto creation payload containing numero and bloco name
     * @param request current HTTP request used to extract the correlation request ID
     * @return the created bloco with HTTP 201
     */
    @PostMapping
    @Timed(value = "bloco.controller.create", description = "Create bloco")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlocoDTO>> create(@RequestBody @Valid CreateBlocoDTO dto,
                                                         HttpServletRequest request) {
        log.info("POST /blocos - creating bloco: numero={}, bloco={}", dto.numero(), dto.bloco());
        var created = blocoService.create(dto);
        log.info("POST /blocos - bloco created with id: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates an existing bloco; requires ADMIN role.
     *
     * @param id bloco primary key
     * @param dto updated bloco data
     * @param request current HTTP request used to extract the correlation request ID
     * @return the updated bloco wrapped in an {@link ApiResponse}
     */
    @PutMapping("/{id}")
    @Timed(value = "bloco.controller.update", description = "Update bloco")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlocoDTO>> update(@PathVariable Long id,
                                                         @RequestBody @Valid BlocoDTO dto,
                                                         HttpServletRequest request) {
        log.info("PUT /blocos/{} - updating bloco", id);
        var updated = blocoService.update(new BlocoDTO(id, dto.numero(), dto.bloco(), dto.createdAt(), dto.updatedAt()));
        log.info("PUT /blocos/{} - bloco updated", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Soft-deletes a bloco by its identifier; requires ADMIN role.
     *
     * @param id bloco primary key
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Timed(value = "bloco.controller.delete", description = "Delete bloco")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /blocos/{} - deleting bloco", id);
        blocoService.delete(id);
        log.info("DELETE /blocos/{} - bloco deleted", id);
        return ResponseEntity.noContent().build();
    }
}
