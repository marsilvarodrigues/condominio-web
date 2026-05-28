package com.pmrodrigues.commons.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.dto.EstadoFilterDTO;
import com.pmrodrigues.commons.service.EstadoService;
import com.pmrodrigues.commons.versioning.ApiVersion;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller exposing read-only endpoints for Brazilian states (estados).
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/estados")
@RequiredArgsConstructor
public class EstadoController {

    private final EstadoService estadoService;

    /**
     * Returns a list of estados, optionally filtered by nome (partial, case-insensitive) or uf (exact, case-insensitive).
     *
     * @param filter optional filter parameters (nome substring, uf abbreviation)
     * @param request current HTTP request used to extract the correlation request ID
     * @return all matching estados wrapped in an {@link ApiResponse}
     */
    @GetMapping()
    @Timed(value = "estado.controller.findAll", description = "Find all estados")
    public ResponseEntity<ApiResponse<List<EstadoDTO>>> findAll(
            @ModelAttribute EstadoFilterDTO filter,
            HttpServletRequest request) {
        log.info("GET /estados - filter={}", filter);
        var result = estadoService.filterBy(filter);
        log.info("GET /estados - returning {} estados", result.size());
        return ResponseEntity.ok(ApiResponse.of(requestId(request), result));
    }

    /**
     * Returns a single estado by its primary key.
     *
     * @param id the estado identifier
     * @param request current HTTP request used to extract the correlation request ID
     * @return the found estado wrapped in an {@link ApiResponse}
     * @throws org.springframework.web.server.ResponseStatusException 404 if no estado exists with the given id
     */
    @GetMapping("/{id}")
    @Timed(value = "estado.controller.findById", description = "Find estado by id")
    public ResponseEntity<ApiResponse<EstadoDTO>> findById(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /estados/{} - finding estado by id", id);
        var estado = estadoService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Estado not found: " + id));
        log.info("GET /estados/{} - estado found", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), estado));
    }
}
