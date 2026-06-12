package com.pmrodrigues.morador.specification;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.repository.PessoaRepository;
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
class PessoaSpecificationTest {

    @Autowired PessoaRepository pessoaRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        var condominio = condominioRepository.save(Condominio.builder()
                .nome("Residencial Spec")
                .cnpj("12.345.678/0001-55")
                .email("spec@test.com")
                .endereco(new Endereco("Rua A, 1", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        pessoaRepository.save(morador("joao@test.com", "João Silva", "111.111.111-11"));
        pessoaRepository.save(morador("maria@test.com", "Maria Santos", "222.222.222-22"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Morador morador(String email, String name, String cpf) {
        var m = new Morador();
        m.setEmail(email);
        m.setName(name);
        m.setPassword("hashed-password");
        m.setCpf(cpf);
        return m;
    }

    // ── hasNome ───────────────────────────────────────────────────────────────

    @Test
    void hasNome_filtrarPorSubstring() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasNome("João"));

        assertThat(result).hasSize(1);
    }

    @Test
    void hasNome_caseInsensitive() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasNome("silva"));

        assertThat(result).hasSize(1);
    }

    @Test
    void hasNome_comNullRetornaTodos() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasNome(null));

        assertThat(result).hasSize(2);
    }

    @Test
    void hasNome_comBlankRetornaTodos() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasNome("  "));

        assertThat(result).hasSize(2);
    }

    // ── hasTipo ───────────────────────────────────────────────────────────────

    @Test
    void hasTipo_filtrarPorMorador() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasTipo("MORADOR"));

        assertThat(result).hasSize(2);
    }

    @Test
    void hasTipo_comNullRetornaTodos() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasTipo(null));

        assertThat(result).hasSize(2);
    }

    // ── hasCpf ────────────────────────────────────────────────────────────────

    @Test
    void hasCpf_filtrarPorCpfExato() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasCpf("111.111.111-11"));

        assertThat(result).hasSize(1);
    }

    @Test
    void hasCpf_comNullRetornaTodos() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasCpf(null));

        assertThat(result).hasSize(2);
    }

    // ── hasEmail ──────────────────────────────────────────────────────────────

    @Test
    void hasEmail_filtrarPorEmailExato() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasEmail("joao@test.com"));

        assertThat(result).hasSize(1);
    }

    @Test
    void hasEmail_comNullRetornaTodos() {
        List<?> result = pessoaRepository.findAll(PessoaSpecification.hasEmail(null));

        assertThat(result).hasSize(2);
    }
}
