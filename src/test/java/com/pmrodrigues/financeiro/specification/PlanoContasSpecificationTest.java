package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.financeiro.model.PlanoContas;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.repository.PlanoContasRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class PlanoContasSpecificationTest {

    @Autowired PlanoContasRepository repository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;

    private Condominio condominio;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "Minas Gerais", "MG"));
        condominio = condominioRepository.save(
                Condominio.builder()
                        .nome("Condominio Gama")
                        .cnpj("11.222.333/0001-44")
                        .email("gama@test.com")
                        .endereco(new Endereco("Rua C, 30", "30000000", "Belo Horizonte", estado))
                        .build());

        TenantContext.setCondominioId(condominio.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private PlanoContas plano(String codigo, String descricao, TipoConta tipo, PlanoContas pai) {
        return PlanoContas.builder()
                .codigo(codigo)
                .descricao(descricao)
                .tipo(tipo)
                .pai(pai)
                .build();
    }

    // ── hasTipo ───────────────────────────────────────────────────────────

    @Test
    void hasTipo_receita_returnsOnlyReceitas() {
        repository.save(plano("1", "Receitas", TipoConta.RECEITA, null));
        repository.save(plano("2", "Despesas", TipoConta.DESPESA, null));

        List<PlanoContas> result = repository.findAll(PlanoContasSpecification.hasTipo(TipoConta.RECEITA));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo(TipoConta.RECEITA);
    }

    @Test
    void hasTipo_despesa_returnsOnlyDespesas() {
        repository.save(plano("1", "Receitas", TipoConta.RECEITA, null));
        repository.save(plano("2", "Despesas", TipoConta.DESPESA, null));

        List<PlanoContas> result = repository.findAll(PlanoContasSpecification.hasTipo(TipoConta.DESPESA));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTipo()).isEqualTo(TipoConta.DESPESA);
    }

    @Test
    void hasTipo_withNull_returnsNull() {
        assertThat(PlanoContasSpecification.hasTipo(null)).isNull();
    }

    // ── hasPai ────────────────────────────────────────────────────────────

    @Test
    void hasPai_withMatchingPaiId_returnsChildren() {
        var pai = repository.save(plano("1", "Receitas", TipoConta.RECEITA, null));
        repository.save(plano("1.1", "Taxa Condominial", TipoConta.RECEITA, pai));
        repository.save(plano("1.2", "Taxa Extra", TipoConta.RECEITA, pai));
        repository.save(plano("2", "Despesas", TipoConta.DESPESA, null));

        List<PlanoContas> result = repository.findAll(PlanoContasSpecification.hasPai(pai.getId()));

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> p.getPai() != null && p.getPai().getId().equals(pai.getId()));
    }

    @Test
    void hasPai_withNullPaiId_returnsNull() {
        assertThat(PlanoContasSpecification.hasPai(null)).isNull();
    }

    @Test
    void hasPai_withNonExistentPaiId_returnsEmpty() {
        repository.save(plano("1", "Receitas", TipoConta.RECEITA, null));

        List<PlanoContas> result = repository.findAll(PlanoContasSpecification.hasPai(9999L));

        assertThat(result).isEmpty();
    }
}
