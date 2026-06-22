package com.pmrodrigues.financeiro.controller;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.commons.versioning.ApiVersion;
import com.pmrodrigues.financeiro.dto.CreateFundoReservaDTO;
import com.pmrodrigues.financeiro.dto.CreditarFundoDTO;
import com.pmrodrigues.financeiro.dto.DebitarFundoDTO;
import com.pmrodrigues.financeiro.dto.FundoReservaDTO;
import com.pmrodrigues.financeiro.dto.FundoReservaMovimentacaoDTO;
import com.pmrodrigues.financeiro.dto.UpdateFundoReservaDTO;
import com.pmrodrigues.financeiro.service.FundoReservaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller exposing endpoints for managing the reserve fund and its movements. */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/fundo-reserva")
@RequiredArgsConstructor
public class FundoReservaController {

  private final FundoReservaService service;

  /** Returns the reserve fund for the current condominium. */
  @GetMapping
  @Timed(value = "fundo.controller.get", description = "Get fundo de reserva")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO','PROPRIETARIO')")
  public ResponseEntity<ApiResponse<FundoReservaDTO>> get(HttpServletRequest request) {
    log.info("GET /fundo-reserva");
    return ResponseEntity.ok(ApiResponse.of(requestId(request), service.get()));
  }

  /** Creates the reserve fund; requires ADMIN role. */
  @PostMapping
  @Timed(value = "fundo.controller.create", description = "Create fundo de reserva")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<FundoReservaDTO>> create(
      @RequestBody @Valid CreateFundoReservaDTO dto, HttpServletRequest request) {
    log.info("POST /fundo-reserva");
    var created = service.create(dto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(requestId(request), created));
  }

  /** Updates the reserve fund percentual and bank account; requires ADMIN role. */
  @PutMapping
  @Timed(value = "fundo.controller.update", description = "Update fundo de reserva")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<FundoReservaDTO>> update(
      @RequestBody @Valid UpdateFundoReservaDTO dto, HttpServletRequest request) {
    log.info("PUT /fundo-reserva");
    return ResponseEntity.ok(ApiResponse.of(requestId(request), service.update(dto)));
  }

  /** Credits an amount to the fund; requires ADMIN role. */
  @PostMapping("/creditar")
  @Timed(value = "fundo.controller.creditar", description = "Creditar fundo de reserva")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<FundoReservaMovimentacaoDTO>> creditar(
      @RequestBody @Valid CreditarFundoDTO dto, HttpServletRequest request) {
    log.info("POST /fundo-reserva/creditar - valor={}", dto.valor());
    var mov = service.creditar(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), mov));
  }

  /** Debits an amount from the fund; requires ADMIN role. */
  @PostMapping("/debitar")
  @Timed(value = "fundo.controller.debitar", description = "Debitar fundo de reserva")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO')")
  public ResponseEntity<ApiResponse<FundoReservaMovimentacaoDTO>> debitar(
      @RequestBody @Valid DebitarFundoDTO dto, HttpServletRequest request) {
    log.info("POST /fundo-reserva/debitar - valor={}", dto.valor());
    var mov = service.debitar(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), mov));
  }

  /** Returns paginated movement history for the fund. */
  @GetMapping("/movimentacoes")
  @Timed(value = "fundo.controller.movimentacoes", description = "List movimentacoes")
  @PreAuthorize("hasAnyRole('ADMIN','SINDICO','PROPRIETARIO')")
  public ResponseEntity<ApiResponse<Page<FundoReservaMovimentacaoDTO>>> listMovimentacoes(
      @PageableDefault(size = 20) Pageable pageable, HttpServletRequest request) {
    log.info("GET /fundo-reserva/movimentacoes");
    return ResponseEntity.ok(
        ApiResponse.of(requestId(request), service.listMovimentacoes(pageable)));
  }
}
