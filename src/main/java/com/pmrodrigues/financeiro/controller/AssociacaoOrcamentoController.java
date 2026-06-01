package com.pmrodrigues.financeiro.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.AssociarItemOrcamentoRequest;
import com.pmrodrigues.financeiro.dto.ContribuicaoResponse;
import com.pmrodrigues.financeiro.dto.DesassociarItemOrcamentoRequest;
import com.pmrodrigues.financeiro.dto.ItemExtratoComOrcamentoResponse;
import com.pmrodrigues.financeiro.dto.SugestaoItemOrcamentoResponse;
import com.pmrodrigues.financeiro.service.AssociacaoOrcamentoService;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

/**
 * REST controller managing the association between bank statement items and budget line items
 * after bank reconciliation.
 *
 * <p>Base path: {@code /conciliacao/associacao}
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/conciliacao/associacao")
@RequiredArgsConstructor
public class AssociacaoOrcamentoController {

    private final AssociacaoOrcamentoService associacaoService;

    /**
     * Associates a statement item with a budget line item. Requires ADMIN role.
     *
     * @param itemExtratoId the statement item to associate
     * @param request       payload containing the target {@code itemOrcamentoId}
     * @param httpRequest   current HTTP request
     */
    @PostMapping("/{itemExtratoId}")
    @Timed(value = "associacao.orcamento.controller.associar", description = "Associate ItemExtrato to ItemOrcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemExtratoComOrcamentoResponse>> associar(
            @PathVariable Long itemExtratoId,
            @Valid @RequestBody AssociarItemOrcamentoRequest request,
            HttpServletRequest httpRequest) {
        log.info("POST /conciliacao/associacao/{} - itemOrcamentoId={}", itemExtratoId, request.itemOrcamentoId());
        var result = associacaoService.associar(itemExtratoId, request.itemOrcamentoId());
        log.info("POST /conciliacao/associacao/{} - associado com sucesso", itemExtratoId);
        return ResponseEntity.ok(ApiResponse.of(requestId(httpRequest), result));
    }

    /**
     * Removes the association between a statement item and its current budget line item.
     * Requires ADMIN role.
     *
     * @param itemExtratoId the statement item to disassociate
     * @param request       payload containing the mandatory justification
     * @param httpRequest   current HTTP request
     */
    @DeleteMapping("/{itemExtratoId}")
    @Timed(value = "associacao.orcamento.controller.desassociar", description = "Disassociate ItemExtrato from ItemOrcamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ItemExtratoComOrcamentoResponse>> desassociar(
            @PathVariable Long itemExtratoId,
            @Valid @RequestBody DesassociarItemOrcamentoRequest request,
            HttpServletRequest httpRequest) {
        log.info("DELETE /conciliacao/associacao/{} - justificativa={}", itemExtratoId, request.justificativa());
        var result = associacaoService.desassociar(itemExtratoId, request);
        log.info("DELETE /conciliacao/associacao/{} - desassociado com sucesso", itemExtratoId);
        return ResponseEntity.ok(ApiResponse.of(requestId(httpRequest), result));
    }

    /**
     * Returns scored budget line item suggestions for a given statement item.
     * Up to five candidates are returned, ordered by descending score.
     *
     * @param itemExtratoId the statement item to find suggestions for
     * @param httpRequest   current HTTP request
     */
    @GetMapping("/sugestoes/{itemExtratoId}")
    @Timed(value = "associacao.orcamento.controller.sugestoes", description = "Suggest ItemOrcamento for ItemExtrato")
    public ResponseEntity<ApiResponse<List<SugestaoItemOrcamentoResponse>>> sugestoes(
            @PathVariable Long itemExtratoId,
            HttpServletRequest httpRequest) {
        log.info("GET /conciliacao/associacao/sugestoes/{} - buscando sugestões", itemExtratoId);
        var result = associacaoService.sugerirItemOrcamento(itemExtratoId);
        log.info("GET /conciliacao/associacao/sugestoes/{} - {} sugestões retornadas", itemExtratoId, result.size());
        return ResponseEntity.ok(ApiResponse.of(requestId(httpRequest), result));
    }

    /**
     * Calculates the realisation contribution for a budget line item from all its associated
     * statement items.
     *
     * @param itemOrcamentoId the budget line item identifier
     * @param httpRequest     current HTTP request
     */
    @GetMapping("/contribuicao/{itemOrcamentoId}")
    @Timed(value = "associacao.orcamento.controller.contribuicao", description = "Calculate contribution for ItemOrcamento")
    public ResponseEntity<ApiResponse<ContribuicaoResponse>> contribuicao(
            @PathVariable Long itemOrcamentoId,
            HttpServletRequest httpRequest) {
        log.info("GET /conciliacao/associacao/contribuicao/{} - calculando contribuição", itemOrcamentoId);
        var result = associacaoService.calcularContribuicao(itemOrcamentoId);
        log.info("GET /conciliacao/associacao/contribuicao/{} - contribuição calculada", itemOrcamentoId);
        return ResponseEntity.ok(ApiResponse.of(requestId(httpRequest), result));
    }
}
