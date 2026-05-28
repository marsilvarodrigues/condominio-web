package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.model.Bloco;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BlocoMapperImpl.class)
class BlocoMapperTest {

    @Autowired BlocoMapper mapper;

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsNumeroBlocoFields() {
        var bloco = Bloco.builder().id(1L).numero(2).bloco("A").build();

        var dto = mapper.toDTO(bloco);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.numero()).isEqualTo(2);
        assertThat(dto.bloco()).isEqualTo("A");
    }

    @Test
    void toDTO_whenNull_returnsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    // ── toEntity (CreateBlocoDTO) ─────────────────────────────────────────

    @Test
    void toEntity_mapsNumeroAndBloco() {
        var dto = new CreateBlocoDTO(2, "B");

        var bloco = mapper.toEntity(dto);

        assertThat(bloco.getNumero()).isEqualTo(2);
        assertThat(bloco.getBloco()).isEqualTo("B");
    }

    @Test
    void toEntity_ignoresServerManagedFields() {
        var dto = new CreateBlocoDTO(1, "A");

        var bloco = mapper.toEntity(dto);

        assertThat(bloco.isDeleted()).isFalse();
        assertThat(bloco.getCreatedAt()).isNull();
        assertThat(bloco.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_whenNull_returnsNull() {
        assertThat(mapper.toEntity((CreateBlocoDTO) null)).isNull();
    }

    // ── updateEntity ──────────────────────────────────────────────────────

    @Test
    void updateEntity_updatesNumeroBlocoFields() {
        var bloco = Bloco.builder().id(1L).numero(1).bloco("A").build();
        var dto = new BlocoDTO(null, 3, "C", null, null);

        mapper.updateEntity(bloco, dto);

        assertThat(bloco.getNumero()).isEqualTo(3);
        assertThat(bloco.getBloco()).isEqualTo("C");
    }

    @Test
    void updateEntity_skipsNullFields() {
        var bloco = Bloco.builder().id(1L).numero(1).bloco("A").build();
        var dto = new BlocoDTO(null, null, "B", null, null);

        mapper.updateEntity(bloco, dto);

        assertThat(bloco.getNumero()).isEqualTo(1);
        assertThat(bloco.getBloco()).isEqualTo("B");
    }
}
