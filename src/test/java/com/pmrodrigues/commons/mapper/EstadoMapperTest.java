package com.pmrodrigues.commons.mapper;

import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.model.Estado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EstadoMapperImpl.class)
class EstadoMapperTest {

    @Autowired EstadoMapper mapper;

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsAllFields() {
        var estado = new Estado(1L, "São Paulo", "SP");

        var dto = mapper.toDTO(estado);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nome()).isEqualTo("São Paulo");
        assertThat(dto.uf()).isEqualTo("SP");
    }

    @Test
    void toDTO_whenNull_returnsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    // ── toEntity ──────────────────────────────────────────────────────────

    @Test
    void toEntity_mapsAllFields() {
        var dto = new EstadoDTO(2L, "Rio de Janeiro", "RJ");

        var estado = mapper.toEntity(dto);

        assertThat(estado.getId()).isEqualTo(2L);
        assertThat(estado.getNome()).isEqualTo("Rio de Janeiro");
        assertThat(estado.getUf()).isEqualTo("RJ");
    }

    @Test
    void toEntity_whenNull_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}