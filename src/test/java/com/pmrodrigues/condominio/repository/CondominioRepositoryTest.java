package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class CondominioRepositoryTest {

    @Autowired CondominioRepository repository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired EntityManager em;

    private Estado estado;

    @BeforeEach
    void setUp() {
        estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
    }

    private Condominio condominio(String cnpj) {
        return Condominio.builder()
                .nome("Residencial Sol")
                .cnpj(cnpj)
                .email("contato@sol.com")
                .endereco(new Endereco("Rua A, 10", "01001000", "São Paulo", estado))
                .build();
    }

    // ── findByCnpj ────────────────────────────────────────────────────────

    @Test
    void findByCnpj_returnsMatchingCondominio() {
        repository.save(condominio("12.345.678/0001-99"));

        var result = repository.findByCnpj("12.345.678/0001-99");

        assertThat(result).isPresent();
        assertThat(result.get().getNome()).isEqualTo("Residencial Sol");
    }

    @Test
    void findByCnpj_whenNotFound_returnsEmpty() {
        var result = repository.findByCnpj("00.000.000/0000-00");

        assertThat(result).isEmpty();
    }

    // ── soft delete ───────────────────────────────────────────────────────

    @Test
    void delete_softDeletesRow() {
        var saved = repository.save(condominio("12.345.678/0001-99"));

        repository.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(repository.findById(saved.getId())).isEmpty();

        var count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM condominios WHERE id = :id AND deleted = true")
                .setParameter("id", saved.getId())
                .getSingleResult();
        assertThat(count.intValue()).isEqualTo(1);
    }

    @Test
    void findByCnpj_doesNotReturnSoftDeletedCondominio() {
        var saved = repository.save(condominio("12.345.678/0001-99"));
        repository.deleteById(saved.getId());
        em.flush();
        em.clear();

        var result = repository.findByCnpj("12.345.678/0001-99");

        assertThat(result).isEmpty();
    }

    // ── audit fields ──────────────────────────────────────────────────────

    @Test
    void save_populatesTimestampFields() {
        var saved = repository.save(condominio("12.345.678/0001-99"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}