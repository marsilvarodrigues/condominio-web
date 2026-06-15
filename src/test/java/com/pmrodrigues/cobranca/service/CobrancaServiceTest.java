package com.pmrodrigues.cobranca.service;

import com.pmrodrigues.cobranca.dto.CancelarCobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaFilterDTO;
import com.pmrodrigues.cobranca.dto.CobrancaResumoDTO;
import com.pmrodrigues.cobranca.dto.GerarCobrancasDTO;
import com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO;
import com.pmrodrigues.cobranca.mapper.CobrancaMapper;
import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.cobranca.model.CobrancaConfiguracao;
import com.pmrodrigues.cobranca.model.StatusCobranca;
import com.pmrodrigues.cobranca.repository.CobrancaConfiguracaoRepository;
import com.pmrodrigues.cobranca.repository.CobrancaRepository;
import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.financeiro.model.CotaRateio;
import com.pmrodrigues.financeiro.model.RateioExecucao;
import com.pmrodrigues.financeiro.service.CotaRateioService;
import com.pmrodrigues.gateway.dto.AsaasEmissaoResult;
import com.pmrodrigues.gateway.dto.AsaasWebhookPayload;
import com.pmrodrigues.gateway.service.AsaasGatewayService;
import com.pmrodrigues.morador.dto.PessoaDTO;
import com.pmrodrigues.morador.service.PessoaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CobrancaServiceTest {

    @Mock CobrancaRepository cobrancaRepository;
    @Mock CobrancaConfiguracaoRepository configuracaoRepo;
    @Mock AsaasGatewayService gatewayService;
    @Mock MailService mailService;
    @Mock CobrancaMapper mapper;
    @Mock CotaRateioService cotaRateioService;
    @Mock PessoaService pessoaService;

    CobrancaService service;

    @BeforeEach
    void setUp() {
        service = new CobrancaService(cobrancaRepository, configuracaoRepo, gatewayService,
                mailService, mapper, cotaRateioService, pessoaService);

        lenient().when(mapper.toDTO(any(Cobranca.class))).thenAnswer(inv -> {
            Cobranca c = inv.getArgument(0);
            return cobrancaDTO(c.getId() != null ? c.getId() : 1L,
                    c.getStatus() != null ? c.getStatus() : StatusCobranca.PENDENTE);
        });
        lenient().when(mapper.toResumoDTO(any(Cobranca.class))).thenReturn(resumoDTO());
        lenient().when(configuracaoRepo.findByCondominioId(any()))
                .thenReturn(Optional.of(CobrancaConfiguracao.builder().condominioId(1L).build()));
        lenient().when(pessoaService.listarPorApartamento(any())).thenReturn(List.of(pessoaDTO(1L)));
        lenient().when(gatewayService.emitir(any())).thenReturn(emissaoResult());
        lenient().when(cobrancaRepository.save(any(Cobranca.class))).thenAnswer(inv -> {
            Cobranca c = inv.getArgument(0);
            if (c.getId() == null) c.setId(10L);
            return c;
        });
        lenient().when(cobrancaRepository.countByStatusIn(any())).thenReturn(0L);
        lenient().when(cobrancaRepository.sumValorByStatusIn(any())).thenReturn(BigDecimal.ZERO);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CobrancaDTO cobrancaDTO(Long id, StatusCobranca status) {
        return new CobrancaDTO(id, 1L, "101", null, 1L, "Morador Test", "m@t.com",
                new BigDecimal("500.00"), LocalDate.now(), status,
                "http://boleto.url", "123456", null, null, false, null, null, null);
    }

    private CobrancaResumoDTO resumoDTO() {
        return new CobrancaResumoDTO(1L, LocalDate.now(), new BigDecimal("500.00"),
                StatusCobranca.PENDENTE, null, null, false);
    }

    private PessoaDTO pessoaDTO(Long id) {
        return new PessoaDTO(id, "Morador Test", "MORADOR", "123.456.789-09", "m@t.com",
                "11999999999", 1L, "101", null, null, null);
    }

    private AsaasEmissaoResult emissaoResult() {
        return new AsaasEmissaoResult("pay_1", "cus_1", "http://boleto.url", "123456", null, null);
    }

    private CotaRateio cota(Long id, Long aptId) {
        var cota = new CotaRateio();
        cota.setId(id);
        cota.setApartamento(Apartamento.builder().id(aptId).numero("101").build());
        cota.setValor(new BigDecimal("500.00"));
        return cota;
    }

    private Cobranca cobranca(Long id, String asaasId) {
        return Cobranca.builder()
                .id(id)
                .asaasId(asaasId)
                .apartamento(Apartamento.builder().id(1L).numero("101").build())
                .cotaRateioId(1L)
                .moradorId(1L)
                .valor(new BigDecimal("500.00"))
                .vencimento(LocalDate.now())
                .status(StatusCobranca.PENDENTE)
                .build();
    }

    // ── gerarCobrancas ────────────────────────────────────────────────────────

    @Test
    void gerarCobrancas_deveRetornarListaDeCobrancasGeradas() {
        var execucao = new RateioExecucao();
        execucao.setId(1L);
        when(cotaRateioService.findExecucaoById(1L)).thenReturn(execucao);
        when(cotaRateioService.findByExecucaoId(1L)).thenReturn(List.of(cota(1L, 10L), cota(2L, 11L)));

        var result = service.gerarCobrancas(new GerarCobrancasDTO(1L, LocalDate.now().plusDays(10)));

        assertThat(result).hasSize(2);
    }

    @Test
    void gerarCobrancas_deveLancar404_quandoExecucaoNaoExiste() {
        when(cotaRateioService.findExecucaoById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.gerarCobrancas(new GerarCobrancasDTO(99L, LocalDate.now())))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── processarCota ─────────────────────────────────────────────────────────

    @Test
    void processarCota_deveCriarCobrancaEEnviarEmail_quandoMoradorExiste() {
        var result = service.processarCota(cota(1L, 10L), LocalDate.now().plusDays(10));

        assertThat(result).isNotNull();
        verify(cobrancaRepository, times(2)).save(any(Cobranca.class));
        verify(mailService).sendEmail(eq("m@t.com"), any());
    }

    @Test
    void processarCota_deveRetornarNull_quandoSemMorador() {
        when(pessoaService.listarPorApartamento(any())).thenReturn(List.of());

        var result = service.processarCota(cota(1L, 10L), LocalDate.now().plusDays(10));

        assertThat(result).isNull();
        verify(cobrancaRepository, never()).save(any(Cobranca.class));
    }

    @Test
    void processarCota_deveRetornarNull_quandoCobrancaJaExiste_idempotencia() {
        when(cobrancaRepository.findByCotaRateioId(1L)).thenReturn(Optional.of(cobranca(5L, "pay_5")));

        var result = service.processarCota(cota(1L, 10L), LocalDate.now().plusDays(10));

        assertThat(result).isNull();
        verify(cobrancaRepository, never()).save(any(Cobranca.class));
    }

    // ── filterBy ──────────────────────────────────────────────────────────────

    @Test
    void filterBy_deveRetornarPaginaFiltrada() {
        var page = new PageImpl<>(List.of(cobranca(1L, "pay_1")));
        when(cobrancaRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        var result = service.filterBy(new CobrancaFilterDTO(null, null, null, null, null),
                PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_deveRetornarDTO() {
        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca(1L, "pay_1")));

        var result = service.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findById_deveLancar404_quandoNaoExiste() {
        when(cobrancaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── porApartamento ────────────────────────────────────────────────────────

    @Test
    void porApartamento_deveRetornarPagina() {
        var page = new PageImpl<>(List.of(cobranca(1L, "pay_1")));
        when(cobrancaRepository.findByApartamentoIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(page);

        var result = service.porApartamento(1L, PageRequest.of(0, 10));

        assertThat(result).isNotEmpty();
    }

    // ── resumo ────────────────────────────────────────────────────────────────

    @Test
    void resumo_deveRetornarContadoresEValores() {
        when(cobrancaRepository.countByStatusIn(List.of(StatusCobranca.PENDENTE, StatusCobranca.ENVIADA)))
                .thenReturn(3L);
        when(cobrancaRepository.countByStatusIn(List.of(StatusCobranca.VENCIDA)))
                .thenReturn(1L);
        when(cobrancaRepository.sumValorByStatusIn(List.of(StatusCobranca.PENDENTE, StatusCobranca.ENVIADA)))
                .thenReturn(new BigDecimal("1500.00"));
        when(cobrancaRepository.sumValorByStatusIn(List.of(StatusCobranca.VENCIDA)))
                .thenReturn(new BigDecimal("300.00"));

        ResumoCobrancasDTO result = service.resumo();

        assertThat(result.quantidadePendente()).isEqualTo(3L);
        assertThat(result.totalPendente()).isEqualByComparingTo("1500.00");
        assertThat(result.quantidadeVencida()).isEqualTo(1L);
        assertThat(result.totalVencido()).isEqualByComparingTo("300.00");
    }

    // ── cancelar ──────────────────────────────────────────────────────────────

    @Test
    void cancelar_deveCancelarNaAsaasEAtualizarStatus() {
        var cobranca = cobranca(1L, "pay_1");
        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca));
        when(cobrancaRepository.save(cobranca)).thenReturn(cobranca);

        var result = service.cancelar(1L, new CancelarCobrancaDTO("Motivo teste"));

        verify(gatewayService).cancelar("pay_1");
        assertThat(result.status()).isEqualTo(StatusCobranca.CANCELADA);
    }

    @Test
    void cancelar_deveLancar404_quandoNaoExiste() {
        when(cobrancaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(99L, new CancelarCobrancaDTO("Motivo")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── reenviarEmail ─────────────────────────────────────────────────────────

    @Test
    void reenviarEmail_deveReenviarEmail_quandoMoradorExiste() {
        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca(1L, "pay_1")));

        service.reenviarEmail(1L);

        verify(mailService).sendEmail(any(), any());
        verify(cobrancaRepository).save(any(Cobranca.class));
    }

    @Test
    void reenviarEmail_deveLancarIllegalState_quandoSemMorador() {
        when(cobrancaRepository.findById(1L)).thenReturn(Optional.of(cobranca(1L, "pay_1")));
        when(pessoaService.listarPorApartamento(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.reenviarEmail(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── processarWebhook ──────────────────────────────────────────────────────

    @Test
    void processarWebhook_deveMarcarComoPaga_quandoPaymentReceived() {
        var cobranca = cobranca(1L, "pay_1");
        when(cobrancaRepository.findByAsaasIdNative("pay_1")).thenReturn(Optional.of(cobranca));

        service.processarWebhook(new AsaasWebhookPayload("PAYMENT_RECEIVED",
                new AsaasWebhookPayload.AsaasWebhookPayment("pay_1", "RECEIVED", "500.00", "2026-06-08")));

        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.PAGA);
        verify(cobrancaRepository).save(cobranca);
    }

    @Test
    void processarWebhook_deveIgnorar_quandoEventoNaoEhPagamento() {
        service.processarWebhook(new AsaasWebhookPayload("PAYMENT_OVERDUE",
                new AsaasWebhookPayload.AsaasWebhookPayment("pay_1", "OVERDUE", "500.00", "2026-06-08")));

        verify(cobrancaRepository, never()).findByAsaasIdNative(any());
    }

    @Test
    void processarWebhook_deveIgnorar_quandoCobrancaJaPaga() {
        var cobranca = cobranca(1L, "pay_1");
        cobranca.setStatus(StatusCobranca.PAGA);
        when(cobrancaRepository.findByAsaasIdNative("pay_1")).thenReturn(Optional.of(cobranca));

        service.processarWebhook(new AsaasWebhookPayload("PAYMENT_RECEIVED",
                new AsaasWebhookPayload.AsaasWebhookPayment("pay_1", "RECEIVED", "500.00", "2026-06-08")));

        verify(cobrancaRepository, never()).save(any());
    }

    // ── softDeleteByCondominioId ───────────────────────────────────────────────

    @Test
    void softDeleteByCondominioId_callsRepository() {
        service.softDeleteByCondominioId(42L);

        verify(cobrancaRepository).softDeleteByCondominioId(42L);
    }
}
