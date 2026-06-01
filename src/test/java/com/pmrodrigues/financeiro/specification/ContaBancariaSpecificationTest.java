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
import com.pmrodrigues.financeiro.model.ContaBancaria;
import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import com.pmrodrigues.financeiro.repository.ContaBancariaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class ContaBancariaSpecificationTest {

    @Autowired ContaBancariaRepository repository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired BancoRepository bancoRepository;

    private Condominio condominio;
    private Banco banco;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        condominio = condominioRepository.save(
                Condominio.builder()
                        .nome("Residencial Alfa")
                        .cnpj("12.345.678/0001-99")
                        .email("alfa@test.com")
                        .endereco(new Endereco("Rua A, 10", "01001000", "São Paulo", estado))
                        .build());
        banco = bancoRepository.save(Banco.builder().codigo("001").nome("Banco do Brasil").build());

        TenantContext.setCondominioId(condominio.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ContaBancaria conta(TipoContaBancaria tipo, String agencia, String conta, boolean ativa) {
        return ContaBancaria.builder()
                .banco(banco)
                .tipo(tipo)
                .agencia(agencia)
                .conta(conta)
                .ativa(ativa)
                .build();
    }

    // ── hasTipo ───────────────────────────────────────────────────────────

    @Test
    void hasTipo_withMatchingTipo_returnsOnlyMatching() {
        repository.save(conta(TipoContaBancaria.CORRENTE, "0001", "12345", true));
        repository.save(conta(TipoContaBancaria.POUPANCA, "0001", "54321", true));

        var spec = ContaBancariaSpecification.hasTipo(TipoContaBancaria.CORRENTE);
        List<ContaBancaria> result = repository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo(TipoContaBancaria.CORRENTE);
    }

    @Test
    void hasTipo_withNull_returnsNull() {
        assertThat(ContaBancariaSpecification.hasTipo(null)).isNull();
    }

    // ── isAtiva ───────────────────────────────────────────────────────────

    @Test
    void isAtiva_true_returnsOnlyActive() {
        repository.save(conta(TipoContaBancaria.CORRENTE, "0001", "11111", true));
        repository.save(conta(TipoContaBancaria.POUPANCA, "0001", "22222", false));

        List<ContaBancaria> result = repository.findAll(ContaBancariaSpecification.isAtiva(true));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAtiva()).isTrue();
    }

    @Test
    void isAtiva_false_returnsOnlyInactive() {
        repository.save(conta(TipoContaBancaria.CORRENTE, "0001", "11111", true));
        repository.save(conta(TipoContaBancaria.POUPANCA, "0001", "22222", false));

        List<ContaBancaria> result = repository.findAll(ContaBancariaSpecification.isAtiva(false));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAtiva()).isFalse();
    }

    @Test
    void isAtiva_withNull_returnsNull() {
        assertThat(ContaBancariaSpecification.isAtiva(null)).isNull();
    }

    // ── hasAgencia ────────────────────────────────────────────────────────

    @Test
    void hasAgencia_withPrefix_returnsMatching() {
        repository.save(conta(TipoContaBancaria.CORRENTE, "1234", "11111", true));
        repository.save(conta(TipoContaBancaria.POUPANCA, "5678", "22222", true));

        List<ContaBancaria> result = repository.findAll(ContaBancariaSpecification.hasAgencia("123"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAgencia()).isEqualTo("1234");
    }

    @Test
    void hasAgencia_withNull_returnsNull() {
        assertThat(ContaBancariaSpecification.hasAgencia(null)).isNull();
    }

    @Test
    void hasAgencia_withBlank_returnsNull() {
        assertThat(ContaBancariaSpecification.hasAgencia("   ")).isNull();
    }

    // ── hasConta ──────────────────────────────────────────────────────────

    @Test
    void hasConta_withPrefix_returnsMatching() {
        repository.save(conta(TipoContaBancaria.CORRENTE, "0001", "99988", true));
        repository.save(conta(TipoContaBancaria.POUPANCA, "0002", "77766", true));

        List<ContaBancaria> result = repository.findAll(ContaBancariaSpecification.hasConta("999"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConta()).isEqualTo("99988");
    }

    @Test
    void hasConta_withNull_returnsNull() {
        assertThat(ContaBancariaSpecification.hasConta(null)).isNull();
    }

    @Test
    void hasConta_withBlank_returnsNull() {
        assertThat(ContaBancariaSpecification.hasConta("")).isNull();
    }

    // ── combined ──────────────────────────────────────────────────────────

    @Test
    void combined_tipoAndAtiva_returnsIntersection() {
        repository.save(conta(TipoContaBancaria.CORRENTE, "0001", "11111", true));
        repository.save(conta(TipoContaBancaria.CORRENTE, "0002", "22222", false));
        repository.save(conta(TipoContaBancaria.POUPANCA, "0003", "33333", true));

        Specification<ContaBancaria> spec = Specification
                .where(ContaBancariaSpecification.hasTipo(TipoContaBancaria.CORRENTE))
                .and(ContaBancariaSpecification.isAtiva(true));

        List<ContaBancaria> result = repository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo(TipoContaBancaria.CORRENTE);
        assertThat(result.get(0).isAtiva()).isTrue();
    }
}
