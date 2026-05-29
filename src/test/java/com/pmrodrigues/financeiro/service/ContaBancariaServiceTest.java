package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.commons.dto.BancoDTO;
import com.pmrodrigues.commons.service.BancoService;
import com.pmrodrigues.financeiro.dto.ContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.ContaBancariaFilterDTO;
import com.pmrodrigues.financeiro.dto.CreateContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.UpdateContaBancariaDTO;
import com.pmrodrigues.financeiro.mapper.ContaBancariaMapper;
import com.pmrodrigues.financeiro.model.ContaBancaria;
import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import com.pmrodrigues.financeiro.repository.ContaBancariaRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaBancariaServiceTest {

    @Mock ContaBancariaRepository repository;
    @Mock BancoService bancoService;
    @Mock ContaBancariaMapper mapper;

    ContaBancariaService service;

    private ContaBancaria entity(Long id, TipoContaBancaria tipo) {
        return ContaBancaria.builder()
                .id(id).tipo(tipo).agencia("0001").conta("12345").build();
    }

    private ContaBancariaDTO dto(Long id, TipoContaBancaria tipo) {
        return new ContaBancariaDTO(id, 1L, "Itaú", "341",
                tipo, "0001", "12345", null, null, null,
                BigDecimal.ZERO, true, LocalDateTime.now(), LocalDateTime.now());
    }

    @BeforeEach
    void setUp() {
        service = new ContaBancariaService(repository, bancoService, mapper);
        lenient().when(mapper.toDTO(any(ContaBancaria.class))).thenAnswer(inv -> {
            ContaBancaria c = inv.getArgument(0);
            return dto(c.getId(), c.getTipo());
        });
    }

    // ── filterBy ──────────────────────────────────────────────────────────

    @Test
    void filterBy_returnsPage() {
        var pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity(1L, TipoContaBancaria.CORRENTE))));

        var result = service.filterBy(new ContaBancariaFilterDTO(null, null, null, null), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity(1L, TipoContaBancaria.CORRENTE)));

        var result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.tipo()).isEqualTo(TipoContaBancaria.CORRENTE);
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
    void create_corrente_saves() {
        var createDTO = new CreateContaBancariaDTO(1L, TipoContaBancaria.CORRENTE, "0001", "12345", null, null, null);
        var bancoDTO = new BancoDTO(1L, "341", "Itaú", "60701190");
        var newEntity = entity(null, TipoContaBancaria.CORRENTE);
        when(mapper.toEntity(createDTO)).thenReturn(newEntity);
        when(bancoService.findById(1L)).thenReturn(Optional.of(bancoDTO));
        when(repository.save(newEntity)).thenAnswer(inv -> {
            ContaBancaria c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        var result = service.create(createDTO);

        assertThat(result.id()).isEqualTo(1L);
        verify(repository).save(newEntity);
    }

    @Test
    void create_fundoReserva_whenAlreadyExists_throwsConflict() {
        var createDTO = new CreateContaBancariaDTO(1L, TipoContaBancaria.FUNDO_RESERVA, "0001", "12345", null, null, null);
        when(repository.existsByTipoAndDeletedFalse(TipoContaBancaria.FUNDO_RESERVA)).thenReturn(true);

        assertThatThrownBy(() -> service.create(createDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void create_whenBancoNotFound_throwsNotFound() {
        var createDTO = new CreateContaBancariaDTO(99L, TipoContaBancaria.CORRENTE, "0001", "12345", null, null, null);
        when(bancoService.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(createDTO))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesFields() {
        var existing = entity(1L, TipoContaBancaria.CORRENTE);
        var updateDTO = new UpdateContaBancariaDTO("0002", "99999", null, "Conta principal", null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.update(1L, updateDTO);

        verify(mapper).updateEntity(eq(existing), eq(updateDTO));
        verify(repository).save(existing);
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateContaBancariaDTO("0001", "1", null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── setAtiva ──────────────────────────────────────────────────────────

    @Test
    void setAtiva_true_activatesAccount() {
        var existing = entity(1L, TipoContaBancaria.CORRENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.setAtiva(1L, true);

        assertThat(existing.isAtiva()).isTrue();
        verify(repository).save(existing);
    }

    @Test
    void setAtiva_false_deactivatesAccount() {
        var existing = entity(1L, TipoContaBancaria.CORRENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.setAtiva(1L, false);

        assertThat(existing.isAtiva()).isFalse();
        verify(repository).save(existing);
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_softDeletes() {
        var existing = entity(1L, TipoContaBancaria.CORRENTE);
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
