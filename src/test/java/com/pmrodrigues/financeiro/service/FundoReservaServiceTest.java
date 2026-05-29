package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.mapper.FundoReservaMapper;
import com.pmrodrigues.financeiro.model.FundoReserva;
import com.pmrodrigues.financeiro.model.FundoReservaMovimentacao;
import com.pmrodrigues.financeiro.model.TipoMovimentacao;
import com.pmrodrigues.financeiro.repository.FundoReservaMovimentacaoRepository;
import com.pmrodrigues.financeiro.repository.FundoReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundoReservaServiceTest {

    @Mock FundoReservaRepository repository;
    @Mock FundoReservaMovimentacaoRepository movimentacaoRepository;
    @Mock FundoReservaMapper mapper;

    FundoReservaService service;

    private final FundoReserva entity = FundoReserva.builder().id(1L)
            .percentualArrecadacao(new BigDecimal("10.00"))
            .saldoAtual(new BigDecimal("5000.00"))
            .contaBancariaDestino("001-1").build();

    private final FundoReservaDTO dto = new FundoReservaDTO(1L, new BigDecimal("10.00"),
            new BigDecimal("5000.00"), "001-1", LocalDateTime.now(), LocalDateTime.now());

    @BeforeEach
    void setUp() {
        service = new FundoReservaService(repository, movimentacaoRepository, mapper);
        lenient().when(mapper.toDTO(any(FundoReserva.class))).thenReturn(dto);
        lenient().when(mapper.toEntity(any(CreateFundoReservaDTO.class))).thenReturn(entity);
    }

    // ── get ───────────────────────────────────────────────────────────────

    @Test
    void get_whenExists_returnsDTO() {
        when(repository.findFirstBy()).thenReturn(Optional.of(entity));

        var result = service.get();

        assertThat(result.id()).isEqualTo(1L);
        verify(repository).findFirstBy();
    }

    @Test
    void get_whenNotFound_throwsNotFound() {
        when(repository.findFirstBy()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_whenNoneExists_saves() {
        when(repository.existsByDeletedFalse()).thenReturn(false);
        when(repository.save(entity)).thenReturn(entity);

        service.create(new CreateFundoReservaDTO(new BigDecimal("10.00"), "001-1"));

        verify(repository).save(entity);
    }

    @Test
    void create_whenAlreadyExists_throwsConflict() {
        when(repository.existsByDeletedFalse()).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateFundoReservaDTO(new BigDecimal("10.00"), "001-1")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenExists_updatesFields() {
        var updateDTO = new UpdateFundoReservaDTO(new BigDecimal("15.00"), "002-2");
        when(repository.findFirstBy()).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        service.update(updateDTO);

        verify(mapper).updateEntity(eq(entity), eq(updateDTO));
        verify(repository).save(entity);
    }

    // ── creditar ──────────────────────────────────────────────────────────

    @Test
    void creditar_increasesBalance() {
        when(repository.findFirstBy()).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);
        var movEntity = FundoReservaMovimentacao.builder().id(10L)
                .tipo(TipoMovimentacao.CREDITO).valor(new BigDecimal("1000.00"))
                .dataMovimentacao(LocalDate.now()).build();
        when(movimentacaoRepository.save(any())).thenReturn(movEntity);
        when(mapper.toMovimentacaoDTO(movEntity)).thenReturn(
                new FundoReservaMovimentacaoDTO(10L, TipoMovimentacao.CREDITO, new BigDecimal("1000.00"),
                        null, LocalDate.now(), LocalDateTime.now()));

        var result = service.creditar(new CreditarFundoDTO(new BigDecimal("1000.00"), null, null));

        assertThat(result.tipo()).isEqualTo(TipoMovimentacao.CREDITO);
        verify(repository).save(any(FundoReserva.class));
        verify(movimentacaoRepository).save(any(FundoReservaMovimentacao.class));
    }

    @Test
    void creditar_whenFundoNotFound_throwsNotFound() {
        when(repository.findFirstBy()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creditar(new CreditarFundoDTO(new BigDecimal("100"), null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── debitar ───────────────────────────────────────────────────────────

    @Test
    void debitar_decreasesBalance() {
        when(repository.findFirstBy()).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);
        var movEntity = FundoReservaMovimentacao.builder().id(11L)
                .tipo(TipoMovimentacao.DEBITO).valor(new BigDecimal("500.00"))
                .dataMovimentacao(LocalDate.now()).build();
        when(movimentacaoRepository.save(any())).thenReturn(movEntity);
        when(mapper.toMovimentacaoDTO(movEntity)).thenReturn(
                new FundoReservaMovimentacaoDTO(11L, TipoMovimentacao.DEBITO, new BigDecimal("500.00"),
                        "Conserto", LocalDate.now(), LocalDateTime.now()));

        var result = service.debitar(new DebitarFundoDTO(new BigDecimal("500.00"), "Conserto", null));

        assertThat(result.tipo()).isEqualTo(TipoMovimentacao.DEBITO);
        verify(repository).save(any(FundoReserva.class));
    }

    @Test
    void debitar_whenInsufficientBalance_throwsConflict() {
        when(repository.findFirstBy()).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.debitar(new DebitarFundoDTO(new BigDecimal("99999.00"), "x", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    // ── listMovimentacoes ─────────────────────────────────────────────────

    @Test
    void listMovimentacoes_returnsPaginatedResults() {
        var pageable = PageRequest.of(0, 10);
        when(repository.findFirstBy()).thenReturn(Optional.of(entity));
        when(movimentacaoRepository.findByFundoReservaIdOrderByDataMovimentacaoDesc(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        var page = service.listMovimentacoes(pageable);

        assertThat(page.getContent()).isEmpty();
    }

    // ── softDeleteByCondominioId ──────────────────────────────────────────

    @Test
    void softDeleteByCondominioId_softDeletesMovimentacoesFirst() {
        var inOrder = inOrder(movimentacaoRepository, repository);

        service.softDeleteByCondominioId(42L);

        inOrder.verify(movimentacaoRepository).softDeleteByCondominioId(42L);
        inOrder.verify(repository).softDeleteByCondominioId(42L);
    }
}
