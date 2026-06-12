package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApartamentoMapperImpl.class)
class ApartamentoMapperTest {

    @Autowired ApartamentoMapper mapper;
    @MockitoBean EntityManager em;

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsNumero() {
        var bloco = new Bloco();
        bloco.setId(5L);
        var apartamento = Apartamento.builder().id(1L).bloco(bloco).numero("101").build();

        var dto = mapper.toDTO(apartamento);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.numero()).isEqualTo("101");
        assertThat(dto.quantidadeMoradores()).isZero();
    }

    @Test
    void toDTO_whenNull_returnsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    // ── toEntity (CreateApartamentoDTO) ───────────────────────────────────

    @Test
    void toEntity_mapsNumeroAndCreatesBlocoProxy() {
        var bloco = new Bloco();
        bloco.setId(5L);
        when(em.getReference(Bloco.class, 5L)).thenReturn(bloco);

        var dto = new CreateApartamentoDTO(5L, "202", BigDecimal.TEN, null, null);
        var apartamento = mapper.toEntity(dto);

        assertThat(apartamento.getNumero()).isEqualTo("202");
        assertThat(apartamento.getBloco()).isNotNull();
        assertThat(apartamento.getBloco().getId()).isEqualTo(5L);
        assertThat(apartamento.getAreaConstruida()).isEqualTo(BigDecimal.TEN);
        verify(em).getReference(Bloco.class, 5L);
    }

    @Test
    void toEntity_whenBlocoIdIsNull_blocoIsNull() {
        var dto = new CreateApartamentoDTO(null, "101", BigDecimal.TEN, null, null);

        var apartamento = mapper.toEntity(dto);

        assertThat(apartamento.getBloco()).isNull();
    }

    @Test
    void toEntity_ignoresServerManagedFields() {
        var bloco = new Bloco();
        bloco.setId(5L);
        when(em.getReference(Bloco.class, 5L)).thenReturn(bloco);

        var dto = new CreateApartamentoDTO(5L, "101", BigDecimal.TEN, null, null);
        var apartamento = mapper.toEntity(dto);

        assertThat(apartamento.isDeleted()).isFalse();
        assertThat(apartamento.getCreatedAt()).isNull();
        assertThat(apartamento.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_whenNull_returnsNull() {
        assertThat(mapper.toEntity((CreateApartamentoDTO) null)).isNull();
    }

    // ── updateEntity ──────────────────────────────────────────────────────

    @Test
    void updateEntity_updatesNumero() {
        var apartamento = Apartamento.builder().id(1L).numero("101").build();
        var dto = new ApartamentoDTO(null, null, null, "202", null, null, null, null, null, 0);

        mapper.updateEntity(apartamento, dto);

        assertThat(apartamento.getNumero()).isEqualTo("202");
    }

    @Test
    void updateEntity_doesNotModifyBloco() {
        var bloco = new Bloco();
        bloco.setId(5L);
        var apartamento = Apartamento.builder().id(1L).bloco(bloco).numero("101").build();
        var dto = new ApartamentoDTO(null, null, null, "202", null, null, null, null, null, 0);

        mapper.updateEntity(apartamento, dto);

        assertThat(apartamento.getBloco().getId()).isEqualTo(5L);
    }

    @Test
    void updateEntity_skipsNullNumero() {
        var apartamento = Apartamento.builder().id(1L).numero("101").build();
        var dto = new ApartamentoDTO(null, null, null, null, null, null, null, null, null, 0);

        mapper.updateEntity(apartamento, dto);

        assertThat(apartamento.getNumero()).isEqualTo("101");
    }

    @Test
    void updateEntity_mapsFracaoIdealAndAndar() {
        var apartamento = Apartamento.builder().id(1L).numero("101").build();
        var dto = new ApartamentoDTO(
            null, null, null, "101", null, null,
            BigDecimal.TEN, new BigDecimal("0.125"), 3, 0);

        mapper.updateEntity(apartamento, dto);

        assertThat(apartamento.getAreaConstruida()).isEqualTo(BigDecimal.TEN);
        assertThat(apartamento.getFracaoIdeal()).isEqualByComparingTo("0.125");
        assertThat(apartamento.getAndar()).isEqualTo(3);
    }
}
