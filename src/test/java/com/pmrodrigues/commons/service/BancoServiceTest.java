package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.dto.BancoDTO;
import com.pmrodrigues.commons.dto.BancoFilterDTO;
import com.pmrodrigues.commons.dto.CreateBancoDTO;
import com.pmrodrigues.commons.dto.UpdateBancoDTO;
import com.pmrodrigues.commons.mapper.BancoMapper;
import com.pmrodrigues.commons.model.Banco;
import com.pmrodrigues.commons.repository.BancoRepository;
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
class BancoServiceTest {

    @Mock BancoRepository repository;
    @Mock BancoMapper mapper;

    BancoService service;

    private Banco banco(Long id) {
        return Banco.builder().id(id).codigo("341").nome("Itaú").ispb("60701190").build();
    }

    private BancoDTO dto(Long id) {
        return new BancoDTO(id, "341", "Itaú", "60701190");
    }

    @BeforeEach
    void setUp() {
        service = new BancoService(repository, mapper);
        lenient().when(mapper.toDTO(any(Banco.class))).thenAnswer(inv -> {
            Banco b = inv.getArgument(0);
            return new BancoDTO(b.getId(), b.getCodigo(), b.getNome(), b.getIspb());
        });
        lenient().when(mapper.toEntity(any(CreateBancoDTO.class))).thenAnswer(inv -> {
            CreateBancoDTO d = inv.getArgument(0);
            return Banco.builder().codigo(d.codigo()).nome(d.nome()).ispb(d.ispb()).build();
        });
    }

    // ── filterBy ──────────────────────────────────────────────────────────

    @Test
    void filterBy_noFilter_returnsAll() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(banco(1L), banco(2L)));

        var result = service.filterBy(new BancoFilterDTO(null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void filterBy_withNome_returnsMatch() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(banco(1L)));

        var result = service.filterBy(new BancoFilterDTO(null, "Itaú"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nome()).isEqualTo("Itaú");
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(banco(1L)));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().codigo()).isEqualTo("341");
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsDTO() {
        var createDTO = new CreateBancoDTO("341", "Itaú", "60701190");
        var entity = banco(null);
        when(mapper.toEntity(createDTO)).thenReturn(entity);
        when(repository.save(entity)).thenAnswer(inv -> {
            Banco b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        var result = service.create(createDTO);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.codigo()).isEqualTo("341");
        verify(repository).save(entity);
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesAndReturnsDTO() {
        var entity = banco(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        var result = service.update(1L, new UpdateBancoDTO("Itaú Unibanco", "60701190"));

        verify(mapper).updateEntity(eq(entity), any(UpdateBancoDTO.class));
        verify(repository).save(entity);
        assertThat(result).isNotNull();
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateBancoDTO("X", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_softDeletesEntity() {
        var entity = banco(1L);
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
