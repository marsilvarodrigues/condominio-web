package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Banco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.financeiro.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class ItemExtratoRepositoryTest {

    @Autowired ItemExtratoRepository repository;
    @Autowired EntityManager em;

    private Condominio condominio;
    private ExtratoImportacao extratoImportacao;
    private ItemOrcamento itemOrcamento;

    @BeforeEach
    void setUp() {
        var estado = new Estado(null, "São Paulo", "SP");
        em.persist(estado);

        condominio = Condominio.builder()
                .nome("Cond Test").cnpj("11.111.111/0001-11")
                .email("test@test.com")
                .endereco(new Endereco("Rua A, 1", "01001000", "São Paulo", estado))
                .build();
        em.persist(condominio);

        var banco = Banco.builder().codigo("001").nome("Banco Br").build();
        em.persist(banco);

        var cb = ContaBancaria.builder()
                .condominio(condominio).banco(banco)
                .tipo(TipoContaBancaria.CORRENTE)
                .agencia("1234").conta("56789-0")
                .build();
        em.persist(cb);

        extratoImportacao = ExtratoImportacao.builder()
                .condominio(condominio).contaBancaria(cb)
                .formato(FormatoExtrato.OFX)
                .dataImportacao(LocalDateTime.now())
                .build();
        em.persist(extratoImportacao);

        var pc = PlanoContas.builder()
                .condominio(condominio)
                .codigo("1.1").descricao("Taxa condominial").tipo(TipoConta.RECEITA)
                .build();
        em.persist(pc);

        var orcamentoAnual = OrcamentoAnual.builder()
                .condominio(condominio).exercicio(2025)
                .build();
        em.persist(orcamentoAnual);

        itemOrcamento = ItemOrcamento.builder()
                .condominio(condominio).orcamentoAnual(orcamentoAnual).planoContas(pc)
                .valorPrevisto(BigDecimal.valueOf(1000))
                .build();
        em.persist(itemOrcamento);

        em.flush();
    }

    private ItemExtrato itemExtrato(BigDecimal valor, ItemOrcamento orcamento) {
        return ItemExtrato.builder()
                .condominio(condominio)
                .extratoImportacao(extratoImportacao)
                .dataLancamento(LocalDate.now())
                .valor(valor)
                .tipo(TipoLancamento.CREDITO)
                .descricao("Test")
                .itemOrcamento(orcamento)
                .build();
    }

    // ── findByItemOrcamentoId ─────────────────────────────────────────────

    @Test
    void findByItemOrcamentoId_returnsOnlyAssociatedItems() {
        var associated = repository.save(itemExtrato(BigDecimal.valueOf(500), itemOrcamento));
        repository.save(itemExtrato(BigDecimal.valueOf(100), null));
        em.flush();
        em.clear();

        var result = repository.findByItemOrcamentoId(itemOrcamento.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(associated.getId());
    }

    @Test
    void findByItemOrcamentoId_whenNoAssociations_returnsEmpty() {
        repository.save(itemExtrato(BigDecimal.valueOf(100), null));
        em.flush();
        em.clear();

        assertThat(repository.findByItemOrcamentoId(itemOrcamento.getId())).isEmpty();
    }

    @Test
    void findByItemOrcamentoId_doesNotReturnSoftDeletedItems() {
        var saved = repository.save(itemExtrato(BigDecimal.valueOf(300), itemOrcamento));
        em.flush();
        em.clear();

        repository.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(repository.findByItemOrcamentoId(itemOrcamento.getId())).isEmpty();
    }

    // ── soft delete ───────────────────────────────────────────────────────

    @Test
    void delete_softDeletesRow() {
        var saved = repository.save(itemExtrato(BigDecimal.valueOf(200), null));
        em.flush();
        em.clear();

        repository.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(repository.findById(saved.getId())).isEmpty();

        var count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM itens_extrato WHERE id = :id AND deleted = true")
                .setParameter("id", saved.getId())
                .getSingleResult();
        assertThat(count.intValue()).isEqualTo(1);
    }
}
