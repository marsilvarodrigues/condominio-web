package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class TenantFilterTest {

    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired EntityManager em;

    private Condominio cond1;
    private Condominio cond2;
    private Bloco bloco1;
    private Bloco bloco2;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        cond1 = condominioRepository.save(
                Condominio.builder()
                        .nome("Cond Alpha")
                        .cnpj("11.111.111/0001-11")
                        .email("alpha@test.com")
                        .endereco(new Endereco("Rua A", "01001000", "SP", estado))
                        .build());
        cond2 = condominioRepository.save(
                Condominio.builder()
                        .nome("Cond Beta")
                        .cnpj("22.222.222/0001-22")
                        .email("beta@test.com")
                        .endereco(new Endereco("Rua B", "01002000", "SP", estado))
                        .build());

        bloco1 = blocoRepository.save(Bloco.builder().condominio(cond1).numero(1).bloco("A").build());
        bloco2 = blocoRepository.save(Bloco.builder().condominio(cond2).numero(1).bloco("A").build());

        apartamentoRepository.save(Apartamento.builder().condominio(cond1).bloco(bloco1).numero("101").build());
        apartamentoRepository.save(Apartamento.builder().condominio(cond1).bloco(bloco1).numero("102").build());
        apartamentoRepository.save(Apartamento.builder().condominio(cond2).bloco(bloco2).numero("201").build());

        em.flush();
        em.clear();
    }

    private void enableFilter(Long condominioId) {
        em.unwrap(Session.class)
                .enableFilter("condominioFilter")
                .setParameter("condominioId", condominioId);
    }

    // ── Bloco filter ──────────────────────────────────────────────────────

    @Test
    void blocoFilter_restrictsToCond1() {
        enableFilter(cond1.getId());

        var result = blocoRepository.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(bloco1.getId());
    }

    @Test
    void blocoFilter_restrictsToCond2() {
        enableFilter(cond2.getId());

        var result = blocoRepository.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(bloco2.getId());
    }

    @Test
    void blocoFilter_whenNotEnabled_returnsAll() {
        var result = blocoRepository.findAll();

        assertThat(result).hasSize(2);
    }

    // ── Apartamento filter ────────────────────────────────────────────────

    @Test
    void apartamentoFilter_restrictsToCond1() {
        enableFilter(cond1.getId());

        var result = apartamentoRepository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Apartamento::getNumero).containsExactlyInAnyOrder("101", "102");
    }

    @Test
    void apartamentoFilter_restrictsToCond2() {
        enableFilter(cond2.getId());

        var result = apartamentoRepository.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumero()).isEqualTo("201");
    }

    @Test
    void apartamentoFilter_whenNotEnabled_returnsAll() {
        var result = apartamentoRepository.findAll();

        assertThat(result).hasSize(3);
    }

}
