package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.model.CotaRateio;
import com.pmrodrigues.financeiro.model.RateioExecucao;
import com.pmrodrigues.financeiro.repository.CotaRateioRepository;
import com.pmrodrigues.financeiro.repository.RateioExecucaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotaRateioServiceTest {

    @Mock CotaRateioRepository cotaRateioRepository;
    @Mock RateioExecucaoRepository rateioExecucaoRepository;

    CotaRateioService service;

    @BeforeEach
    void setUp() {
        service = new CotaRateioService(cotaRateioRepository, rateioExecucaoRepository);
    }

    private RateioExecucao execucao(Long id) {
        var e = new RateioExecucao();
        e.setId(id);
        return e;
    }

    private CotaRateio cota(Long id) {
        var c = new CotaRateio();
        c.setId(id);
        return c;
    }

    // ── findExecucaoById ──────────────────────────────────────────────────────

    @Test
    void findExecucaoById_deveRetornarEntidade_quandoExiste() {
        when(rateioExecucaoRepository.findById(1L)).thenReturn(Optional.of(execucao(1L)));

        var result = service.findExecucaoById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findExecucaoById_deveLancar404_quandoNaoExiste() {
        when(rateioExecucaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findExecucaoById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── findExecucaoOptional ──────────────────────────────────────────────────

    @Test
    void findExecucaoOptional_deveRetornarOptionalPreenchido() {
        when(rateioExecucaoRepository.findById(1L)).thenReturn(Optional.of(execucao(1L)));

        var result = service.findExecucaoOptional(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findExecucaoOptional_deveRetornarOptionalVazio() {
        when(rateioExecucaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findExecucaoOptional(99L)).isEmpty();
    }

    // ── findByExecucaoId ──────────────────────────────────────────────────────

    @Test
    void findByExecucaoId_deveRetornarListaDeCotas() {
        when(cotaRateioRepository.findByRateioExecucaoId(42L))
                .thenReturn(List.of(cota(1L), cota(2L)));

        var result = service.findByExecucaoId(42L);

        assertThat(result).hasSize(2);
    }
}
