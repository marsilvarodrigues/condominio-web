package com.pmrodrigues.morador.repository;

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
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class ProprietarioRepositoryTest {

    @Autowired ProprietarioRepository proprietarioRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired EstadoRepository estadoRepository;

    private static final String[] VALID_CPFS = {"529.982.247-25", "111.444.777-35"};
    private int cpfIdx = 0;

    private Condominio condominio;
    private Bloco bloco;
    private Apartamento apt1;
    private Apartamento apt2;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        condominio = condominioRepository.save(Condominio.builder()
                .nome("Cond Prop Repo")
                .cnpj("22.333.444/0001-55")
                .email("proprepo@test.com")
                .endereco(new Endereco("Rua E, 5", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        bloco = blocoRepository.save(Bloco.builder()
                .condominio(condominio).numero(1).bloco("A").build());

        apt1 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("101").areaConstruida(BigDecimal.TEN).build());
        apt2 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("201").areaConstruida(BigDecimal.TEN).build());

        var prop1 = proprietario("prop1@test.com", "Proprietario Um");
        prop1.getApartamentos().add(apt1);
        proprietarioRepository.save(prop1);

        var prop2 = proprietario("prop2@test.com", "Proprietario Dois");
        prop2.getApartamentos().add(apt2);
        proprietarioRepository.save(prop2);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ProprietarioPessoaFisica proprietario(String email, String name) {
        var p = new ProprietarioPessoaFisica();
        p.setEmail(email);
        p.setName(name);
        p.setPassword("hashed-password");
        p.setCpf(VALID_CPFS[cpfIdx++]);
        return p;
    }

    // ── findByApartamentosId ──────────────────────────────────────────────────

    @Test
    void findByApartamentosId_retornaProprietariosDoApartamento() {
        var result = proprietarioRepository.findByApartamentosId(apt1.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("prop1@test.com");
    }

    @Test
    void findByApartamentosId_comApartamentoSemProprietario_retornaVazio() {
        var aptVazio = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio)
                .bloco(bloco)
                .numero("999")
                .areaConstruida(BigDecimal.ONE)
                .build());

        assertThat(proprietarioRepository.findByApartamentosId(aptVazio.getId())).isEmpty();
    }

    // ── findByUserEmail ───────────────────────────────────────────────────────

    @Test
    void findByUserEmail_whenExists_returnsProprietario() {
        var result = proprietarioRepository.findByUserEmail("prop1@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("prop1@test.com");
    }

    @Test
    void findByUserEmail_whenNotExists_returnsEmpty() {
        assertThat(proprietarioRepository.findByUserEmail("ninguem@test.com")).isEmpty();
    }
}
