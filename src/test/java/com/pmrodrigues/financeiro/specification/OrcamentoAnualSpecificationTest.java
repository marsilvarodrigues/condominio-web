package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import com.pmrodrigues.financeiro.repository.OrcamentoAnualRepository;
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
class OrcamentoAnualSpecificationTest {

    @Autowired OrcamentoAnualRepository repository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;

    private Condominio condominio;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "Bahia", "BA"));
        condominio = condominioRepository.save(
                Condominio.builder()
                        .nome("Condominio Delta")
                        .cnpj("55.666.777/0001-88")
                        .email("delta@test.com")
                        .endereco(new Endereco("Rua D, 40", "40000000", "Salvador", estado))
                        .build());

        TenantContext.setCondominioId(condominio.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private OrcamentoAnual orcamento(int exercicio, StatusOrcamento status) {
        return OrcamentoAnual.builder()
                .exercicio(exercicio)
                .status(status)
                .build();
    }

    // ── hasExercicio ──────────────────────────────────────────────────────

    @Test
    void hasExercicio_withMatchingYear_returnsMatching() {
        repository.save(orcamento(2025, StatusOrcamento.RASCUNHO));
        repository.save(orcamento(2026, StatusOrcamento.RASCUNHO));

        List<OrcamentoAnual> result = repository.findAll(
                OrcamentoAnualSpecification.hasExercicio(2025));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExercicio()).isEqualTo(2025);
    }

    @Test
    void hasExercicio_withNonMatchingYear_returnsEmpty() {
        repository.save(orcamento(2025, StatusOrcamento.RASCUNHO));

        List<OrcamentoAnual> result = repository.findAll(
                OrcamentoAnualSpecification.hasExercicio(2099));

        assertThat(result).isEmpty();
    }

    @Test
    void hasExercicio_withNull_returnsNull() {
        assertThat(OrcamentoAnualSpecification.hasExercicio(null)).isNull();
    }

    // ── hasStatus ─────────────────────────────────────────────────────────

    @Test
    void hasStatus_aprovado_returnsOnlyAprovados() {
        repository.save(orcamento(2024, StatusOrcamento.RASCUNHO));
        repository.save(orcamento(2025, StatusOrcamento.APROVADO));
        repository.save(orcamento(2026, StatusOrcamento.ENCERRADO));

        List<OrcamentoAnual> result = repository.findAll(
                OrcamentoAnualSpecification.hasStatus(StatusOrcamento.APROVADO));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    void hasStatus_rascunho_returnsOnlyRascunhos() {
        repository.save(orcamento(2024, StatusOrcamento.RASCUNHO));
        repository.save(orcamento(2025, StatusOrcamento.APROVADO));

        List<OrcamentoAnual> result = repository.findAll(
                OrcamentoAnualSpecification.hasStatus(StatusOrcamento.RASCUNHO));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusOrcamento.RASCUNHO);
    }

    @Test
    void hasStatus_withNull_returnsNull() {
        assertThat(OrcamentoAnualSpecification.hasStatus(null)).isNull();
    }

    // ── combined ──────────────────────────────────────────────────────────

    @Test
    void combined_exercicioAndStatus_returnsIntersection() {
        repository.save(orcamento(2025, StatusOrcamento.RASCUNHO));
        repository.save(orcamento(2025, StatusOrcamento.APROVADO));
        repository.save(orcamento(2026, StatusOrcamento.APROVADO));

        Specification<OrcamentoAnual> spec = Specification
                .where(OrcamentoAnualSpecification.hasExercicio(2025))
                .and(OrcamentoAnualSpecification.hasStatus(StatusOrcamento.APROVADO));

        List<OrcamentoAnual> result = repository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExercicio()).isEqualTo(2025);
        assertThat(result.get(0).getStatus()).isEqualTo(StatusOrcamento.APROVADO);
    }
}
