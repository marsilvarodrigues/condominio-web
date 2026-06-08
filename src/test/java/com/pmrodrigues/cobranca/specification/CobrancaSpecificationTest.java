package com.pmrodrigues.cobranca.specification;

import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.cobranca.model.StatusCobranca;
import com.pmrodrigues.cobranca.repository.CobrancaRepository;
import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import com.pmrodrigues.condominio.repository.BlocoRepository;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class CobrancaSpecificationTest {

    @Autowired CobrancaRepository cobrancaRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired EstadoRepository estadoRepository;

    private Apartamento apt1;
    private Apartamento apt2;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        var condominio = condominioRepository.save(Condominio.builder()
                .nome("Residencial Spec")
                .cnpj("12.345.678/0001-77")
                .email("spec@test.com")
                .endereco(new Endereco("Rua B, 5", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        var bloco = blocoRepository.save(Bloco.builder().condominio(condominio).numero(1).bloco("A").build());

        apt1 = apartamentoRepository.save(
                Apartamento.builder().condominio(condominio).bloco(bloco).numero("101").areaConstruida(BigDecimal.TEN).build());
        apt2 = apartamentoRepository.save(
                Apartamento.builder().condominio(condominio).bloco(bloco).numero("201").areaConstruida(BigDecimal.TEN).build());

        cobrancaRepository.save(cobranca(apt1, null, LocalDate.now().minusDays(5), false, 1L));
        cobrancaRepository.save(cobranca(apt2, StatusCobranca.PAGA, LocalDate.now(), true, 2L));
        cobrancaRepository.save(cobranca(apt1, null, LocalDate.now().plusDays(30), false, 3L));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Cobranca cobranca(Apartamento apt, StatusCobranca status, LocalDate vencimento,
                               boolean emailEnviado, Long cotaRateioId) {
        var c = Cobranca.builder()
                .apartamento(apt)
                .cotaRateioId(cotaRateioId)
                .valor(new BigDecimal("500.00"))
                .vencimento(vencimento)
                .emailEnviado(emailEnviado)
                .build();
        if (status != null) {
            c.setStatus(status);
        }
        return c;
    }

    // ── hasApartamento ────────────────────────────────────────────────────────

    @Test
    void hasApartamento_filtrarPorApartamento() {
        List<Cobranca> result = cobrancaRepository.findAll(CobrancaSpecification.hasApartamento(apt1.getId()));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.getApartamento().getId().equals(apt1.getId()));
    }

    @Test
    void hasApartamento_comNullRetornaTodasAsCobrancas() {
        List<Cobranca> result = cobrancaRepository.findAll(CobrancaSpecification.hasApartamento(null));

        assertThat(result).hasSize(3);
    }

    // ── hasStatus ─────────────────────────────────────────────────────────────

    @Test
    void hasStatus_filtrarPorStatus() {
        List<Cobranca> result = cobrancaRepository.findAll(CobrancaSpecification.hasStatus(StatusCobranca.PAGA));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusCobranca.PAGA);
    }

    @Test
    void hasStatus_comNullRetornaTodasAsCobrancas() {
        List<Cobranca> result = cobrancaRepository.findAll(CobrancaSpecification.hasStatus(null));

        assertThat(result).hasSize(3);
    }

    // ── vencimentoFrom ────────────────────────────────────────────────────────

    @Test
    void vencimentoFrom_filtrarPorDataInicio() {
        List<Cobranca> result = cobrancaRepository.findAll(CobrancaSpecification.vencimentoFrom(LocalDate.now()));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> !c.getVencimento().isBefore(LocalDate.now()));
    }

    // ── vencimentoTo ──────────────────────────────────────────────────────────

    @Test
    void vencimentoTo_filtrarPorDataFim() {
        List<Cobranca> result = cobrancaRepository.findAll(CobrancaSpecification.vencimentoTo(LocalDate.now()));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> !c.getVencimento().isAfter(LocalDate.now()));
    }

    // ── emailEnviado ──────────────────────────────────────────────────────────

    @Test
    void emailEnviado_filtrarPorEmailEnviado() {
        List<Cobranca> result = cobrancaRepository.findAll(CobrancaSpecification.emailEnviado(true));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEmailEnviado()).isTrue();
    }
}
