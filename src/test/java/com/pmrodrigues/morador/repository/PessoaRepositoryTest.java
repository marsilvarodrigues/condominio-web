package com.pmrodrigues.morador.repository;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.model.Condominio;
import java.math.BigDecimal;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import com.pmrodrigues.condominio.repository.BlocoRepository;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.morador.model.Morador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class PessoaRepositoryTest {

    @Autowired PessoaRepository pessoaRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired TestEntityManager em;

    private Condominio condominio;
    private Bloco bloco;
    private Apartamento apt;
    private Morador moradorComApt;
    private Morador moradorSemApt;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        condominio = condominioRepository.save(Condominio.builder()
                .nome("Cond Pessoa Repo")
                .cnpj("55.666.777/0001-88")
                .email("pessoarepo@test.com")
                .endereco(new Endereco("Rua D, 4", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        bloco = blocoRepository.save(Bloco.builder()
                .condominio(condominio).numero(1).bloco("A").build());

        apt = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("101").areaConstruida(BigDecimal.TEN).build());

        moradorComApt = new Morador();
        moradorComApt.setEmail("morador-apt@test.com");
        moradorComApt.setName("Morador Com Apt");
        moradorComApt.setPassword("hashed");
        moradorComApt.setApartamento(apt);
        pessoaRepository.save(moradorComApt);

        moradorSemApt = new Morador();
        moradorSemApt.setEmail("morador-semapt@test.com");
        moradorSemApt.setName("Morador Sem Apt");
        moradorSemApt.setPassword("hashed");
        pessoaRepository.save(moradorSemApt);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── findByApartamentoId ───────────────────────────────────────────────────

    @Test
    void findByApartamentoId_retornaMoradoresDoApartamento() {
        var result = pessoaRepository.findByApartamentoId(apt.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("morador-apt@test.com");
    }

    @Test
    void findByApartamentoId_comApartamentoSemMoradores_retornaVazio() {
        var aptVazio = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio)
                .bloco(bloco)
                .numero("999")
                .areaConstruida(BigDecimal.ONE)
                .build());

        assertThat(pessoaRepository.findByApartamentoId(aptVazio.getId())).isEmpty();
    }

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    void findByEmail_retornaPessoaComApartamento() {
        var result = pessoaRepository.findByEmail("morador-apt@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getApartamento().getId()).isEqualTo(apt.getId());
    }

    @Test
    void findByEmail_quandoNaoExiste_retornaVazio() {
        assertThat(pessoaRepository.findByEmail("ninguem@test.com")).isEmpty();
    }

    // softDeleteByCondominioId is not testable in H2: Hibernate 6 generates a
    // PostgreSQL-specific MATERIALIZED CTE + RETURNING clause for bulk updates
    // across JOINED inheritance (deleted column is in the users table, not pessoas).
    // Covered by BDD integration tests with real PostgreSQL.
}
