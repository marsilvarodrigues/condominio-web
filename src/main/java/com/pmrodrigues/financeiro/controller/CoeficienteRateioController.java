package com.pmrodrigues.financeiro.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.CoeficienteRateioDTO;
import com.pmrodrigues.financeiro.dto.CreateCoeficienteRateioDTO;
import com.pmrodrigues.financeiro.dto.UpdateConsumoDTO;
import com.pmrodrigues.financeiro.service.CoeficienteRateioService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

/**
 * REST controller managing coefficient records for expense groups.
 *
 * <p>Base path: {@code /grupos-despesa/{grupoDespesaId}/coeficientes}
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/grupos-despesa/{grupoDespesaId}/coeficientes")
@RequiredArgsConstructor
public class CoeficienteRateioController {

    private final CoeficienteRateioService service;

    /**
     * Returns all active coefficient records for the given group, newest vigência first.
     *
     * @param grupoDespesaId the group identifier
     * @param request        current HTTP request
     */
    @GetMapping
    @Timed(value = "coeficiente.rateio.controller.findAll", description = "List coeficientes by grupo")
    public ResponseEntity<ApiResponse<List<CoeficienteRateioDTO>>> findAll(
            @PathVariable Long grupoDespesaId,
            HttpServletRequest request) {
        log.info("GET /grupos-despesa/{}/coeficientes", grupoDespesaId);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.findByGrupo(grupoDespesaId)));
    }

    /**
     * Creates a new coefficient record for a unit within the group. Requires ADMIN role.
     *
     * @param grupoDespesaId the group identifier
     * @param dto            creation payload
     * @param request        current HTTP request
     */
    @PostMapping
    @Timed(value = "coeficiente.rateio.controller.create", description = "Create coeficiente de rateio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CoeficienteRateioDTO>> create(
            @PathVariable Long grupoDespesaId,
            @Valid @RequestBody CreateCoeficienteRateioDTO dto,
            HttpServletRequest request) {
        log.info("POST /grupos-despesa/{}/coeficientes - apartamentoId={}", grupoDespesaId, dto.apartamentoId());
        var created = service.create(grupoDespesaId, dto);
        return ResponseEntity.created(
                        URI.create("/grupos-despesa/" + grupoDespesaId + "/coeficientes/" + created.id()))
                .body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates the monthly consumption reading for a specific coefficient record. Requires ADMIN role.
     *
     * @param grupoDespesaId the group identifier (for path consistency)
     * @param coefId         the coefficient record identifier
     * @param dto            new consumption value
     * @param request        current HTTP request
     */
    @PutMapping("/{coefId}/consumo")
    @Timed(value = "coeficiente.rateio.controller.updateConsumo", description = "Update consumo m3")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CoeficienteRateioDTO>> updateConsumo(
            @PathVariable Long grupoDespesaId,
            @PathVariable Long coefId,
            @Valid @RequestBody UpdateConsumoDTO dto,
            HttpServletRequest request) {
        log.info("PUT /grupos-despesa/{}/coeficientes/{}/consumo", grupoDespesaId, coefId);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), service.updateConsumo(coefId, dto)));
    }
}
