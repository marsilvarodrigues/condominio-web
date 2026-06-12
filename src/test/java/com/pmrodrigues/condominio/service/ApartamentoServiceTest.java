package com.pmrodrigues.condominio.service;

import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.ApartamentoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.mapper.ApartamentoMapper;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApartamentoServiceTest {

    @Mock ApartamentoRepository repository;
    @Mock ApartamentoMapper mapper;

    ApartamentoService service;

    @BeforeEach
    void setUp() {
        service = new ApartamentoService(repository, mapper);

        lenient().when(mapper.toEntity(any(CreateApartamentoDTO.class))).thenAnswer(inv -> {
            CreateApartamentoDTO d = inv.getArgument(0);
            var ap = new Apartamento();
            ap.setNumero(d.numero());
            if (d.blocoId() != null) {
                var b = new Bloco(); b.setId(d.blocoId()); ap.setBloco(b);
            }
            ap.setAreaConstruida(d.areaConstruida());
            return ap;
        });
        lenient().when(mapper.toDTO(any(Apartamento.class))).thenAnswer(inv -> {
            Apartamento a = inv.getArgument(0);
            Long blocoId = a.getBloco() != null ? a.getBloco().getId() : null;
            return new ApartamentoDTO(a.getId(), blocoId, null, a.getNumero(), a.getCreatedAt(), a.getUpdatedAt(), a.getAreaConstruida(), null, null, 0);
        });
        lenient().doAnswer(inv -> {
            Apartamento a = inv.getArgument(0);
            ApartamentoDTO d = inv.getArgument(1);
            if (d.numero() != null) a.setNumero(d.numero());
            return null;
        }).when(mapper).updateEntity(any(Apartamento.class), any(ApartamentoDTO.class));
    }

    private Apartamento apartamento(Long id, Long blocoId, BigDecimal areaConstruida) {
        var b = new Bloco(); b.setId(blocoId);
        return Apartamento.builder().id(id).bloco(b).numero("101").build();
    }

    // ── filterBy ─────────────────────────────────────────────────────────

    @Test
    void filterBy_withBlocoId_returnsFilteredList() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(apartamento(1L, 5L, BigDecimal.TEN)));

        var result = service.filterBy(new ApartamentoFilterDTO(5L, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).blocoId()).isEqualTo(5L);
        verify(repository).findAll(any(Specification.class));
    }

    @Test
    void filterBy_withNumero_returnsFilteredList() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(apartamento(1L, 5L, BigDecimal.TEN)));

        var result = service.filterBy(new ApartamentoFilterDTO(null, "101"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).numero()).isEqualTo("101");
    }

    @Test
    void filterBy_withBothParams_returnsIntersection() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(apartamento(1L, 5L, BigDecimal.TEN)));

        var result = service.filterBy(new ApartamentoFilterDTO(5L, "101"));

        assertThat(result).hasSize(1);
    }

    @Test
    void filterBy_withBothNull_returnsAll() {
        when(repository.findAll(any(Specification.class))).thenReturn(
                List.of(apartamento(1L, 5L, BigDecimal.TEN), apartamento(2L, 5L, BigDecimal.TEN)));

        var result = service.filterBy(new ApartamentoFilterDTO(null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void filterBy_whenNone_returnsEmpty() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(service.filterBy(new ApartamentoFilterDTO(5L, "999"))).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(apartamento(1L, 5L, BigDecimal.TEN)));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().numero()).isEqualTo("101");
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsDTO() {
        var dto = new CreateApartamentoDTO(5L, "202", BigDecimal.TEN, null, null);
        when(repository.save(any(Apartamento.class))).thenAnswer(inv -> {
            Apartamento a = inv.getArgument(0);
            a.setId(7L);
            return a;
        });

        var result = service.create(dto);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.numero()).isEqualTo("202");
        verify(repository).save(any(Apartamento.class));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesAndReturnsDTO() {
        var entity = apartamento(1L, 5L, BigDecimal.TEN);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        var result = service.update(new ApartamentoDTO(1L, null, null, "303", null, null, BigDecimal.TEN, null, null, 0));

        assertThat(result.numero()).isEqualTo("303");
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new ApartamentoDTO(99L, null, null, "101", null, null, BigDecimal.TEN, null, null, 0)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_deletesEntity() {
        var entity = apartamento(1L, 5L, BigDecimal.TEN);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void delete_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }
}
