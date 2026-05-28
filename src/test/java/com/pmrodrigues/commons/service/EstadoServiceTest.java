package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.dto.EstadoFilterDTO;
import com.pmrodrigues.commons.mapper.EstadoMapper;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class EstadoServiceTest {

    @Mock EstadoRepository repository;
    @Mock EstadoMapper mapper;

    EstadoService service;

    @BeforeEach
    void setUp() {
        service = new EstadoService(repository, mapper);

        lenient().when(mapper.toEntity(any(EstadoDTO.class))).thenAnswer(inv -> {
            EstadoDTO d = inv.getArgument(0);
            return new Estado(d.id(), d.nome(), d.uf());
        });
        lenient().when(mapper.toDTO(any(Estado.class))).thenAnswer(inv -> {
            Estado e = inv.getArgument(0);
            return new EstadoDTO(e.getId(), e.getNome(), e.getUf());
        });
        lenient().doAnswer(inv -> {
            Estado e = inv.getArgument(0);
            EstadoDTO d = inv.getArgument(1);
            if (d.nome() != null) e.setNome(d.nome());
            if (d.uf() != null) e.setUf(d.uf());
            return null;
        }).when(mapper).updateEntity(any(Estado.class), any(EstadoDTO.class));
    }

    // ── filterBy ──────────────────────────────────────────────────────────

    @Test
    void filterBy_noFilter_returnsAll() {
        when(repository.findAll(any(Specification.class))).thenReturn(
                List.of(new Estado(1L, "São Paulo", "SP"), new Estado(2L, "Rio de Janeiro", "RJ")));

        var result = service.filterBy(new EstadoFilterDTO(null, null));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EstadoDTO::uf).containsExactlyInAnyOrder("SP", "RJ");
    }

    @Test
    void filterBy_withNome_returnsMatchingEstados() {
        when(repository.findAll(any(Specification.class))).thenReturn(
                List.of(new Estado(1L, "São Paulo", "SP")));

        var result = service.filterBy(new EstadoFilterDTO("Paulo", null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nome()).isEqualTo("São Paulo");
        verify(repository).findAll(any(Specification.class));
    }

    @Test
    void filterBy_withUf_returnsMatchingEstado() {
        when(repository.findAll(any(Specification.class))).thenReturn(
                List.of(new Estado(1L, "São Paulo", "SP")));

        var result = service.filterBy(new EstadoFilterDTO(null, "SP"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).uf()).isEqualTo("SP");
        verify(repository).findAll(any(Specification.class));
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Estado(1L, "São Paulo", "SP")));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().uf()).isEqualTo("SP");
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsDTO() {
        var dto = new EstadoDTO(null, "Minas Gerais", "MG");
        when(repository.save(any(Estado.class))).thenAnswer(inv -> {
            Estado e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });

        var result = service.create(dto);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.uf()).isEqualTo("MG");
        verify(repository).save(any(Estado.class));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesAndReturnsDTO() {
        var entity = new Estado(1L, "São Paulo", "SP");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        var result = service.update(new EstadoDTO(1L, "São Paulo - atualizado", "SP"));

        assertThat(result.nome()).isEqualTo("São Paulo - atualizado");
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new EstadoDTO(99L, "X", "XX")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_deletesEntity() {
        var entity = new Estado(1L, "São Paulo", "SP");
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
