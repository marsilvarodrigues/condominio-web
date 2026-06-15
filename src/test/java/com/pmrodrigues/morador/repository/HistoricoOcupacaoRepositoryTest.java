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
import com.pmrodrigues.morador.model.HistoricoOcupacao;
import com.pmrodrigues.morador.model.Morador;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class HistoricoOcupacaoRepositoryTest {

    @Autowired HistoricoOcupacaoRepository historicoRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired PessoaRepository pessoaRepository;

    private Condominio condominio;
    private Apartamento apt1;
    private Apartamento apt2;
    private Morador morador;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        condominio = condominioRepository.save(Condominio.builder()
                .nome("Cond Historico Repo")
                .cnpj("99.888.777/0001-66")
                .email("historico.repo@test.com")
                .endereco(new Endereco("Rua H, 1", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        var bloco = blocoRepository.save(Bloco.builder()
                .condominio(condominio).numero(1).bloco("A").build());

        apt1 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("101").areaConstruida(BigDecimal.TEN).build());
        apt2 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("201").areaConstruida(BigDecimal.TEN).build());

        morador = new Morador();
        morador.setEmail("morador.historico@test.com");
        morador.setName("Morador Historico");
        morador.setPassword("hashed");
        pessoaRepository.save(morador);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private HistoricoOcupacao historico(Apartamento apt, LocalDate dataSaida) {
        return historicoRepository.save(HistoricoOcupacao.builder()
                .apartamento(apt)
                .pessoa(morador)
                .dataEntrada(dataSaida.minusYears(1))
                .dataSaida(dataSaida)
                .build());
    }

    // ── findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc ──────────────

    @Test
    void findByApartamento_IdAndCondominio_Id_retornaOrdenadoPorDataSaidaDesc() {
        historico(apt1, LocalDate.of(2023, 1, 1));
        historico(apt1, LocalDate.of(2025, 6, 30));
        historico(apt1, LocalDate.of(2024, 3, 15));

        var page = historicoRepository.findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc(
                apt1.getId(), condominio.getId(), Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getDataSaida()).isEqualTo(LocalDate.of(2025, 6, 30));
        assertThat(page.getContent().get(1).getDataSaida()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(page.getContent().get(2).getDataSaida()).isEqualTo(LocalDate.of(2023, 1, 1));
    }

    @Test
    void findByApartamento_IdAndCondominio_Id_filtraPorApartamento() {
        historico(apt1, LocalDate.of(2024, 1, 1));
        historico(apt1, LocalDate.of(2024, 6, 1));
        historico(apt2, LocalDate.of(2024, 3, 1));

        var pageApt1 = historicoRepository.findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc(
                apt1.getId(), condominio.getId(), Pageable.unpaged());
        var pageApt2 = historicoRepository.findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc(
                apt2.getId(), condominio.getId(), Pageable.unpaged());

        assertThat(pageApt1.getTotalElements()).isEqualTo(2);
        assertThat(pageApt2.getTotalElements()).isEqualTo(1);
    }
}
