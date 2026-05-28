package com.pmrodrigues.commons.repository;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.model.Estado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(JpaAuditingConfig.class)
class EstadoRepositoryTest {

    @Autowired EstadoRepository repository;

    // ── basic persistence ─────────────────────────────────────────────────

    @Test
    void save_assignsId() {
        var estado = repository.save(new Estado(null, "Rio de Janeiro", "RJ"));

        assertThat(estado.getId()).isNotNull();
    }

    @Test
    void findById_returnsPersistedEstado() {
        var saved = repository.save(new Estado(null, "Minas Gerais", "MG"));

        var found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUf()).isEqualTo("MG");
    }
}
