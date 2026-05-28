package com.pmrodrigues.condominio.service;

import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.BlocoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.mapper.BlocoMapper;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.repository.BlocoRepository;
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

@ExtendWith(MockitoExtension.class)
class BlocoServiceTest {

    @Mock BlocoRepository repository;
    @Mock BlocoMapper mapper;

    BlocoService service;

    @BeforeEach
    void setUp() {
        service = new BlocoService(repository, mapper);

        lenient().when(mapper.toEntity(any(CreateBlocoDTO.class))).thenAnswer(inv -> {
            CreateBlocoDTO d = inv.getArgument(0);
            var bloco = new Bloco();
            bloco.setNumero(d.numero());
            bloco.setBloco(d.bloco());
            return bloco;
        });
        lenient().when(mapper.toDTO(any(Bloco.class))).thenAnswer(inv -> {
            Bloco b = inv.getArgument(0);
            return new BlocoDTO(b.getId(), b.getNumero(), b.getBloco(),
                    b.getCreatedAt(), b.getUpdatedAt());
        });
        lenient().doAnswer(inv -> {
            Bloco b = inv.getArgument(0);
            BlocoDTO d = inv.getArgument(1);
            if (d.numero() != null) b.setNumero(d.numero());
            if (d.bloco() != null) b.setBloco(d.bloco());
            return null;
        }).when(mapper).updateEntity(any(Bloco.class), any(BlocoDTO.class));
    }

    private Bloco bloco(Long id) {
        return Bloco.builder().id(id).numero(1).bloco("A").build();
    }

    // ── filterBy ─────────────────────────────────────────────────────────

    @Test
    void filterBy_withBloco_returnsFilteredList() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(bloco(1L)));

        var result = service.filterBy(new BlocoFilterDTO("A"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bloco()).isEqualTo("A");
        verify(repository).findAll(any(Specification.class));
    }

    @Test
    void filterBy_withNullBloco_returnsAll() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(bloco(1L), bloco(2L)));

        var result = service.filterBy(new BlocoFilterDTO(null));

        assertThat(result).hasSize(2);
    }

    @Test
    void filterBy_withEmptyBloco_returnsAll() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(bloco(1L), bloco(2L)));

        var result = service.filterBy(new BlocoFilterDTO(""));

        assertThat(result).hasSize(2);
    }

    @Test
    void filterBy_whenNone_returnsEmpty() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(service.filterBy(new BlocoFilterDTO("Z"))).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(bloco(1L)));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().bloco()).isEqualTo("A");
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsDTO() {
        var dto = new CreateBlocoDTO(2, "B");
        when(repository.save(any(Bloco.class))).thenAnswer(inv -> {
            Bloco b = inv.getArgument(0);
            b.setId(3L);
            return b;
        });

        var result = service.create(dto);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.bloco()).isEqualTo("B");
        verify(repository).save(any(Bloco.class));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesAndReturnsDTO() {
        var entity = bloco(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        var result = service.update(new BlocoDTO(1L, 3, "C", null, null));

        assertThat(result.numero()).isEqualTo(3);
        assertThat(result.bloco()).isEqualTo("C");
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new BlocoDTO(99L, 1, "A", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_deletesEntity() {
        var entity = bloco(1L);
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
