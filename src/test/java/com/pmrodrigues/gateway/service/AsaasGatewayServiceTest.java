package com.pmrodrigues.gateway.service;

import com.pmrodrigues.gateway.client.AsaasClient;
import com.pmrodrigues.gateway.client.PixQrCodeGenerator;
import com.pmrodrigues.gateway.dto.AsaasCobrancaResponse;
import com.pmrodrigues.gateway.dto.AsaasEmissaoRequest;
import com.pmrodrigues.gateway.dto.AsaasPixResponse;
import com.pmrodrigues.gateway.model.AsaasCustomer;
import com.pmrodrigues.gateway.repository.AsaasCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsaasGatewayServiceTest {

    @Mock AsaasClient asaasClient;
    @Mock PixQrCodeGenerator qrCodeGenerator;
    @Mock AsaasCustomerRepository asaasCustomerRepository;

    AsaasGatewayService service;

    @BeforeEach
    void setUp() {
        service = new AsaasGatewayService(asaasClient, qrCodeGenerator, asaasCustomerRepository);
    }

    private AsaasEmissaoRequest req() {
        return new AsaasEmissaoRequest(
                1L, "João Silva", "123.456.789-09", "joao@test.com",
                new BigDecimal("500.00"), LocalDate.now().plusDays(10),
                "Taxa condominial", new BigDecimal("2"), new BigDecimal("1"));
    }

    private AsaasCobrancaResponse cobrancaResp(AsaasPixResponse pix) {
        return new AsaasCobrancaResponse("pay_111", "PENDING", "http://boleto.url", null, "123456", pix);
    }

    private AsaasCustomer customer(String customerId) {
        return AsaasCustomer.builder().pessoaId(1L).customerId(customerId).build();
    }

    // ── emitir ────────────────────────────────────────────────────────────────

    @Test
    void emitir_deveReutilizarCustomerExistente_semCriarNovo() {
        when(asaasCustomerRepository.findByPessoaId(1L))
                .thenReturn(Optional.of(customer("cus_existing")));
        when(asaasClient.criarCobranca(any())).thenReturn(cobrancaResp(null));

        var result = service.emitir(req());

        assertThat(result.asaasCustomerId()).isEqualTo("cus_existing");
        verify(asaasClient, never()).criarCustomer(any());
        verify(asaasCustomerRepository, never()).save(any());
    }

    @Test
    void emitir_deveCriarNovoCustomer_quandoNaoExiste() {
        when(asaasCustomerRepository.findByPessoaId(1L)).thenReturn(Optional.empty());
        when(asaasClient.criarCustomer(any())).thenReturn("cus_novo");
        when(asaasClient.criarCobranca(any())).thenReturn(cobrancaResp(null));

        var result = service.emitir(req());

        assertThat(result.asaasCustomerId()).isEqualTo("cus_novo");
        verify(asaasClient).criarCustomer(any());
        verify(asaasCustomerRepository).save(any());
    }

    @Test
    void emitir_deveGerarQrCodeBase64_quandoPixPayloadPresente() {
        var pix = new AsaasPixResponse(null, "pix.payload.string", null);
        when(asaasCustomerRepository.findByPessoaId(1L))
                .thenReturn(Optional.of(customer("cus_111")));
        when(asaasClient.criarCobranca(any())).thenReturn(cobrancaResp(pix));
        when(qrCodeGenerator.gerarBase64("pix.payload.string")).thenReturn("base64data==");

        var result = service.emitir(req());

        assertThat(result.pixQrCodeBase64()).isEqualTo("base64data==");
        assertThat(result.pixCopiaCola()).isEqualTo("pix.payload.string");
    }

    @Test
    void emitir_naoDeveGerarQrCode_quandoPixPayloadAusente() {
        when(asaasCustomerRepository.findByPessoaId(1L))
                .thenReturn(Optional.of(customer("cus_111")));
        when(asaasClient.criarCobranca(any())).thenReturn(cobrancaResp(null));

        var result = service.emitir(req());

        assertThat(result.pixQrCodeBase64()).isNull();
        verify(qrCodeGenerator, never()).gerarBase64(any());
    }

    // ── cancelar ──────────────────────────────────────────────────────────────

    @Test
    void cancelar_deveDelegarParaAsaasClient() {
        service.cancelar("pay_123");

        verify(asaasClient).cancelarCobranca("pay_123");
    }
}
