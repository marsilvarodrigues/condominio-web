package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.CreateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.LancamentoBancarioDTO;
import com.pmrodrigues.financeiro.dto.LancamentoBancarioFilterDTO;
import com.pmrodrigues.financeiro.dto.UpdateLancamentoBancarioDTO;
import com.pmrodrigues.financeiro.mapper.LancamentoBancarioMapper;
import com.pmrodrigues.financeiro.model.LancamentoBancario;
import com.pmrodrigues.financeiro.model.OrigemLancamento;
import com.pmrodrigues.financeiro.model.StatusLancamento;
import com.pmrodrigues.financeiro.model.TipoLancamento;
import com.pmrodrigues.financeiro.repository.LancamentoBancarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LancamentoBancarioServiceTest {

    @Mock LancamentoBancarioRepository repository;
    @Mock ContaBancariaService contaBancariaService;
    @Mock LancamentoBancarioMapper mapper;

    LancamentoBancarioService service;

    private LancamentoBancario entity(Long id) {
        return LancamentoBancario.builder()
                .id(id)
                .dataLancamento(LocalDate.now())
                .valor(BigDecimal.valueOf(100))
                .tipo(TipoLancamento.CREDITO)
                .descricao("Teste")
                .origem(OrigemLancamento.MANUAL)
                .build();
    }

    private LancamentoBancarioDTO dto(Long id) {
        return new LancamentoBancarioDTO(id, 1L, "Conta Corrente",
                LocalDate.now(), BigDecimal.valueOf(100),
                TipoLancamento.CREDITO, "Teste", OrigemLancamento.MANUAL,
                null, StatusLancamento.PENDENTE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @BeforeEach
    void setUp() {
        service = new LancamentoBancarioService(repository, contaBancariaService, mapper);
        lenient().when(mapper.toDTO(any(LancamentoBancario.class))).thenAnswer(inv -> {
            LancamentoBancario l = inv.getArgument(0);
            return dto(l.getId());
        });
    }

    // ── filterBy ──────────────────────────────────────────────────────────

    @Test
    void filterBy_returnsPage() {
        var pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity(1L))));

        var result = service.filterBy(
                new LancamentoBancarioFilterDTO(null, null, null, null, null, null), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity(1L)));

        var result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.tipo()).isEqualTo(TipoLancamento.CREDITO);
    }

    @Test
    void findById_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_saves() {
        var createDTO = new CreateLancamentoBancarioDTO(
                1L, LocalDate.now(), BigDecimal.valueOf(100),
                TipoLancamento.CREDITO, "Teste", OrigemLancamento.MANUAL, null);
        var newEntity = entity(null);
        when(mapper.toEntity(createDTO)).thenReturn(newEntity);
        when(repository.save(newEntity)).thenAnswer(inv -> {
            LancamentoBancario l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        var result = service.create(createDTO);

        assertThat(result.id()).isEqualTo(1L);
        verify(repository).save(newEntity);
        verify(contaBancariaService).findById(1L);
    }

    @Test
    void create_whenContaBancariaNotFound_throwsNotFound() {
        var createDTO = new CreateLancamentoBancarioDTO(
                99L, LocalDate.now(), BigDecimal.valueOf(100),
                TipoLancamento.DEBITO, "Teste", OrigemLancamento.MANUAL, null);
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND))
                .when(contaBancariaService).findById(99L);

        assertThatThrownBy(() -> service.create(createDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesFields() {
        var existing = entity(1L);
        var updateDTO = new UpdateLancamentoBancarioDTO(LocalDate.now(), BigDecimal.valueOf(200), "Atualizado", null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.update(1L, updateDTO);

        verify(mapper).updateEntity(eq(existing), eq(updateDTO));
        verify(repository).save(existing);
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L,
                new UpdateLancamentoBancarioDTO(LocalDate.now(), BigDecimal.ONE, "x", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_softDeletes() {
        var existing = entity(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).delete(existing);
    }

    @Test
    void delete_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── softDeleteByCondominioId ──────────────────────────────────────────

    @Test
    void softDeleteByCondominioId_callsRepository() {
        service.softDeleteByCondominioId(42L);

        verify(repository).softDeleteByCondominioId(42L);
    }
}
