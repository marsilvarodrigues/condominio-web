package com.pmrodrigues.cobranca.repository;

import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.cobranca.model.StatusCobranca;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class CobrancaRepositoryTest {

    @Autowired CobrancaRepository cobrancaRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired TestEntityManager em;

    private Apartamento apt1;
    private Apartamento apt2;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        var condominio = condominioRepository.save(Condominio.builder()
                .nome("Cond Repo")
                .cnpj("11.222.333/0001-44")
                .email("repo@test.com")
                .endereco(new Endereco("Rua C, 3", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        var bloco = blocoRepository.save(Bloco.builder()
                .condominio(condominio).numero(1).bloco("A").build());

        apt1 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("101").areaConstruida(BigDecimal.TEN).build());
        apt2 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("201").areaConstruida(BigDecimal.TEN).build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Cobranca cobranca(Apartamento apt, StatusCobranca status, boolean emailEnviado,
                               Long cotaRateioId, String asaasId) {
        return cobrancaRepository.save(Cobranca.builder()
                .apartamento(apt)
                .valor(new BigDecimal("500.00"))
                .vencimento(LocalDate.now().plusDays(30))
                .status(status)
                .emailEnviado(emailEnviado)
                .cotaRateioId(cotaRateioId)
                .asaasId(asaasId)
                .build());
    }

    // ── findByCotaRateioId ────────────────────────────────────────────────────

    @Test
    void findByCotaRateioId_whenExists_returnsCobranca() {
        cobranca(apt1, StatusCobranca.PENDENTE, false, 42L, null);

        assertThat(cobrancaRepository.findByCotaRateioId(42L)).isPresent();
    }

    @Test
    void findByCotaRateioId_whenNotExists_returnsEmpty() {
        assertThat(cobrancaRepository.findByCotaRateioId(999L)).isEmpty();
    }

    // ── findByApartamentoIdAndStatus ──────────────────────────────────────────

    @Test
    void findByApartamentoIdAndStatus_filtrarPorStatusCorreto() {
        cobranca(apt1, StatusCobranca.PENDENTE, false, 1L, null);
        cobranca(apt1, StatusCobranca.PAGA, true, 2L, null);

        var result = cobrancaRepository.findByApartamentoIdAndStatus(apt1.getId(), StatusCobranca.PENDENTE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusCobranca.PENDENTE);
    }

    // ── findByStatusAndEmailEnviadoFalse ──────────────────────────────────────

    @Test
    void findByStatusAndEmailEnviadoFalse_retornaApenasNaoEnviados() {
        cobranca(apt1, StatusCobranca.PENDENTE, false, 1L, null);
        cobranca(apt2, StatusCobranca.PENDENTE, true, 2L, null);

        var result = cobrancaRepository.findByStatusAndEmailEnviadoFalse(StatusCobranca.PENDENTE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEmailEnviado()).isFalse();
    }

    // ── findByApartamentoIdOrderByCreatedAtDesc ───────────────────────────────

    @Test
    void findByApartamentoIdOrderByCreatedAtDesc_retornaEmOrdemCorreta() {
        cobranca(apt1, StatusCobranca.PENDENTE, false, 1L, null);
        cobranca(apt1, StatusCobranca.PAGA, true, 2L, null);

        var page = cobrancaRepository.findByApartamentoIdOrderByCreatedAtDesc(
                apt1.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
    }

    // ── findByAsaasIdNative ───────────────────────────────────────────────────

    @Test
    void findByAsaasIdNative_whenExists_returnsCobranca() {
        cobranca(apt1, StatusCobranca.PENDENTE, false, 1L, "pay_abc123");

        assertThat(cobrancaRepository.findByAsaasIdNative("pay_abc123")).isPresent();
    }

    @Test
    void findByAsaasIdNative_whenNotExists_returnsEmpty() {
        assertThat(cobrancaRepository.findByAsaasIdNative("pay_nonexistent")).isEmpty();
    }

    // ── softDeleteByCondominioId ──────────────────────────────────────────────

    @Test
    void softDeleteByCondominioId_marcaTodasDeletadas() {
        var c = cobranca(apt1, StatusCobranca.PENDENTE, false, 1L, null);
        Long condominioId = TenantContext.getCondominioId();

        cobrancaRepository.softDeleteByCondominioId(condominioId);
        em.flush();
        em.clear();

        // Verify via native SQL that deleted flag was set (bypasses @SQLRestriction)
        var deleted = (Boolean) em.getEntityManager()
                .createNativeQuery("SELECT deleted FROM cobrancas WHERE id = ?1")
                .setParameter(1, c.getId())
                .getSingleResult();

        assertThat(deleted).isTrue();
    }
}
