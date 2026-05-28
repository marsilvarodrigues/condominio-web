package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.model.Apartamento;
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
class ApartamentoRepositoryTest {

    @Autowired ApartamentoRepository repository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired EntityManager em;

    private Condominio condominio;
    private Bloco bloco;

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
        bloco = blocoRepository.save(
                Bloco.builder().condominio(condominio).numero(1).bloco("A").build());
    }

    private Apartamento apartamento(String numero) {
        return Apartamento.builder().condominio(condominio).bloco(bloco).numero(numero).build();
    }

    // ── soft delete ───────────────────────────────────────────────────────

    @Test
    void delete_softDeletesRow() {
        var saved = repository.save(apartamento("101"));

        repository.deleteById(saved.getId());
        em.flush();
        em.clear();

        assertThat(repository.findById(saved.getId())).isEmpty();

        var count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM apartamentos WHERE id = :id AND deleted = true")
                .setParameter("id", saved.getId())
                .getSingleResult();
        assertThat(count.intValue()).isEqualTo(1);
    }

    @Test
    void softDeleteByCondominioId_marksAllApartamentosDeleted() {
        repository.save(apartamento("101"));
        repository.save(apartamento("102"));
        em.flush();
        em.clear();

        repository.softDeleteByCondominioId(condominio.getId());
        em.flush();
        em.clear();

        assertThat(repository.findAll()).isEmpty();

        var count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM apartamentos WHERE condominio_id = :cid AND deleted = true")
                .setParameter("cid", condominio.getId())
                .getSingleResult();
        assertThat(count.intValue()).isEqualTo(2);
    }
}
