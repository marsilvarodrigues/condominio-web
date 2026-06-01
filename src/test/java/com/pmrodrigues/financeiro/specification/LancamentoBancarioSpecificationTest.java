package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Banco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.BancoRepository;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.financeiro.model.*;
import com.pmrodrigues.financeiro.repository.ContaBancariaRepository;
import com.pmrodrigues.financeiro.repository.LancamentoBancarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class LancamentoBancarioSpecificationTest {

    @Autowired LancamentoBancarioRepository repository;
    @Autowired ContaBancariaRepository contaBancariaRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired BancoRepository bancoRepository;

    private Condominio condominio;
    private ContaBancaria contaBancaria;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "Rio de Janeiro", "RJ"));
        condominio = condominioRepository.save(
                Condominio.builder()
                        .nome("Condominio Beta")
                        .cnpj("98.765.432/0001-10")
                        .email("beta@test.com")
                        .endereco(new Endereco("Rua B, 20", "20000000", "Rio de Janeiro", estado))
                        .build());
        var banco = bancoRepository.save(Banco.builder().codigo("341").nome("Itaú").build());

        TenantContext.setCondominioId(condominio.getId());

        contaBancaria = contaBancariaRepository.save(
                ContaBancaria.builder()
                        .banco(banco)
                        .tipo(TipoContaBancaria.CORRENTE)
                        .agencia("1234")
                        .conta("56789")
                        .ativa(true)
                        .build());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private LancamentoBancario lancamento(LocalDate data, BigDecimal valor,
                                          TipoLancamento tipo, OrigemLancamento origem,
                                          StatusLancamento status) {
        return LancamentoBancario.builder()
                .contaBancaria(contaBancaria)
                .dataLancamento(data)
                .valor(valor)
                .tipo(tipo)
                .descricao("Lancamento teste")
                .origem(origem)
                .status(status)
                .build();
    }

    // ── hasContaBancaria ──────────────────────────────────────────────────

    @Test
    void hasContaBancaria_withMatchingId_returnsMatching() {
        repository.save(lancamento(LocalDate.now(), BigDecimal.TEN,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));

        var spec = LancamentoBancarioSpecification.hasContaBancaria(contaBancaria.getId());
        List<LancamentoBancario> result = repository.findAll(spec);

        assertThat(result).hasSize(1);
    }

    @Test
    void hasContaBancaria_withNull_returnsNull() {
        assertThat(LancamentoBancarioSpecification.hasContaBancaria(null)).isNull();
    }

    // ── hasTipo ───────────────────────────────────────────────────────────

    @Test
    void hasTipo_credito_returnsOnlyCreditos() {
        repository.save(lancamento(LocalDate.now(), BigDecimal.TEN,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(LocalDate.now(), BigDecimal.ONE,
                TipoLancamento.DEBITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));

        List<LancamentoBancario> result = repository.findAll(
                LancamentoBancarioSpecification.hasTipo(TipoLancamento.CREDITO));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo(TipoLancamento.CREDITO);
    }

    @Test
    void hasTipo_withNull_returnsNull() {
        assertThat(LancamentoBancarioSpecification.hasTipo((TipoLancamento) null)).isNull();
    }

    // ── hasOrigem ─────────────────────────────────────────────────────────

    @Test
    void hasOrigem_manual_returnsOnlyManual() {
        repository.save(lancamento(LocalDate.now(), BigDecimal.TEN,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(LocalDate.now(), BigDecimal.ONE,
                TipoLancamento.DEBITO, OrigemLancamento.IMPORTACAO, StatusLancamento.PENDENTE));

        List<LancamentoBancario> result = repository.findAll(
                LancamentoBancarioSpecification.hasOrigem(OrigemLancamento.MANUAL));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrigem()).isEqualTo(OrigemLancamento.MANUAL);
    }

    @Test
    void hasOrigem_withNull_returnsNull() {
        assertThat(LancamentoBancarioSpecification.hasOrigem(null)).isNull();
    }

    // ── hasStatus ─────────────────────────────────────────────────────────

    @Test
    void hasStatus_confirmado_returnsOnlyConfirmados() {
        repository.save(lancamento(LocalDate.now(), BigDecimal.TEN,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(LocalDate.now(), BigDecimal.ONE,
                TipoLancamento.DEBITO, OrigemLancamento.MANUAL, StatusLancamento.CONCILIADO));

        List<LancamentoBancario> result = repository.findAll(
                LancamentoBancarioSpecification.hasStatus(StatusLancamento.CONCILIADO));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusLancamento.CONCILIADO);
    }

    @Test
    void hasStatus_withNull_returnsNull() {
        assertThat(LancamentoBancarioSpecification.hasStatus(null)).isNull();
    }

    // ── dataInicio ────────────────────────────────────────────────────────

    @Test
    void dataInicio_returnsOnOrAfter() {
        var yesterday = LocalDate.now().minusDays(1);
        var today = LocalDate.now();
        var tomorrow = LocalDate.now().plusDays(1);

        repository.save(lancamento(yesterday, BigDecimal.ONE,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(today, BigDecimal.TEN,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(tomorrow, BigDecimal.ONE,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));

        List<LancamentoBancario> result = repository.findAll(
                LancamentoBancarioSpecification.dataInicio(today));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(l -> !l.getDataLancamento().isBefore(today));
    }

    @Test
    void dataInicio_withNull_returnsNull() {
        assertThat(LancamentoBancarioSpecification.dataInicio(null)).isNull();
    }

    // ── dataFim ───────────────────────────────────────────────────────────

    @Test
    void dataFim_returnsOnOrBefore() {
        var yesterday = LocalDate.now().minusDays(1);
        var today = LocalDate.now();
        var tomorrow = LocalDate.now().plusDays(1);

        repository.save(lancamento(yesterday, BigDecimal.ONE,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(today, BigDecimal.TEN,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(tomorrow, BigDecimal.ONE,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));

        List<LancamentoBancario> result = repository.findAll(
                LancamentoBancarioSpecification.dataFim(today));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(l -> !l.getDataLancamento().isAfter(today));
    }

    @Test
    void dataFim_withNull_returnsNull() {
        assertThat(LancamentoBancarioSpecification.dataFim(null)).isNull();
    }

    // ── combined ──────────────────────────────────────────────────────────

    @Test
    void combined_tipoAndDateRange_returnsIntersection() {
        var yesterday = LocalDate.now().minusDays(1);
        var today = LocalDate.now();
        var tomorrow = LocalDate.now().plusDays(1);

        repository.save(lancamento(yesterday, BigDecimal.ONE,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(today, BigDecimal.TEN,
                TipoLancamento.DEBITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));
        repository.save(lancamento(tomorrow, BigDecimal.ONE,
                TipoLancamento.CREDITO, OrigemLancamento.MANUAL, StatusLancamento.PENDENTE));

        Specification<LancamentoBancario> spec = Specification
                .where(LancamentoBancarioSpecification.hasTipo(TipoLancamento.CREDITO))
                .and(LancamentoBancarioSpecification.dataFim(today));

        List<LancamentoBancario> result = repository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo(TipoLancamento.CREDITO);
        assertThat(result.get(0).getDataLancamento()).isEqualTo(yesterday);
    }
}
