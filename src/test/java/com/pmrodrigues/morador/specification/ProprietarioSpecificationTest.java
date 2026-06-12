package com.pmrodrigues.morador.specification;

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
import com.pmrodrigues.morador.repository.ProprietarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class ProprietarioSpecificationTest {

    @Autowired ProprietarioRepository proprietarioRepository;
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
                .nome("Residencial Prop Spec")
                .cnpj("99.888.777/0001-11")
                .email("propspec@test.com")
                .endereco(new Endereco("Rua B, 2", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        var bloco = blocoRepository.save(Bloco.builder()
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

    private static final String[] VALID_CPFS = {"529.982.247-25", "111.444.777-35"};
    private int cpfIdx = 0;

    private ProprietarioPessoaFisica proprietario(String email, String name) {
        var p = new ProprietarioPessoaFisica();
        p.setEmail(email);
        p.setName(name);
        p.setPassword("hashed-password");
        p.setCpf(VALID_CPFS[cpfIdx++]);
        return p;
    }

    // ── hasApartamentoId ──────────────────────────────────────────────────────

    @Test
    void hasApartamentoId_filtrarPorApartamento() {
        var result = proprietarioRepository.findAll(ProprietarioSpecification.hasApartamentoId(apt1.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApartamentos()).contains(apt1);
    }

    @Test
    void hasApartamentoId_comNullRetornaTodos() {
        var result = proprietarioRepository.findAll(ProprietarioSpecification.hasApartamentoId(null));

        assertThat(result).hasSize(2);
    }

    @Test
    void hasApartamentoId_comApartamentoSemProprietario_retornaVazio() {
        var estado = estadoRepository.findAll().get(0);
        var condominio = condominioRepository.findAll().get(0);
        var bloco = blocoRepository.findAll().get(0);
        var aptSemProprietario = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("301").areaConstruida(BigDecimal.ONE).build());

        var result = proprietarioRepository.findAll(
                ProprietarioSpecification.hasApartamentoId(aptSemProprietario.getId()));

        assertThat(result).isEmpty();
    }
}
