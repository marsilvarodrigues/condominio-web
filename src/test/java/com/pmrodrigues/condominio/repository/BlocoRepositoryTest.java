package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.model.Bloco;
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
class BlocoRepositoryTest {

    @Autowired BlocoRepository repository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired EntityManager em;

    private Condominio condominio;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        condominio = condominioRepository.save(
                Condominio.builder()
                        .nome("Residencial Sol")
                        .cnpj("12.345.678/0001-99")
                        .email("contato@sol.com")
                        .endereco(new Endereco("Rua A, 10", "01001000", "São Paulo", estado))
                        .build());
    }

    private Bloco bloco(String nome, int numero) {
        return Bloco.builder()
                .condominio(condominio)
                .numero(numero)
                .bloco(nome)
                .build();
    }

    // ── soft delete ───────────────────────────────────────────────────────

    @Test
    void delete_softDeletesRow() {
        var saved = repository.save(bloco("A", 1));

        repository.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(repository.findById(saved.getId())).isEmpty();

        var count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM blocos WHERE id = :id AND deleted = true")
                .setParameter("id", saved.getId())
                .getSingleResult();
        assertThat(count.intValue()).isEqualTo(1);
    }

    @Test
    void softDeleteByCondominioId_marksAllBlocosDeleted() {
        repository.save(bloco("A", 1));
        repository.save(bloco("B", 2));
        em.flush();
        em.clear();

        repository.softDeleteByCondominioId(condominio.getId());
        em.flush();
        em.clear();

        assertThat(repository.findAll()).isEmpty();

        var count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM blocos WHERE condominio_id = :cid AND deleted = true")
                .setParameter("cid", condominio.getId())
                .getSingleResult();
        assertThat(count.intValue()).isEqualTo(2);
    }
}
