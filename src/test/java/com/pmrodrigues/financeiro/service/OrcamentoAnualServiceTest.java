package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.mapper.OrcamentoAnualMapper;
import com.pmrodrigues.financeiro.model.ItemOrcamento;
import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.repository.ItemOrcamentoRepository;
import com.pmrodrigues.financeiro.repository.OrcamentoAnualRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoAnualServiceTest {

    @Mock OrcamentoAnualRepository repository;
    @Mock ItemOrcamentoRepository itemRepository;
    @Mock OrcamentoAnualMapper mapper;
    @Mock EntityManager entityManager;

    OrcamentoAnualService service;

    private OrcamentoAnual rascunho() {
        return OrcamentoAnual.builder().id(1L).exercicio(2026).status(StatusOrcamento.RASCUNHO).itens(List.of()).build();
    }

    private OrcamentoAnualDTO dto(Long id, StatusOrcamento status) {
        return new OrcamentoAnualDTO(id, 2026, status, null, List.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    @BeforeEach
    void setUp() {
        service = new OrcamentoAnualService(repository, itemRepository, mapper, entityManager);
        lenient().when(mapper.toDTO(any(OrcamentoAnual.class))).thenAnswer(inv -> {
            OrcamentoAnual o = inv.getArgument(0);
            return dto(o.getId(), o.getStatus());
        });
        lenient().when(mapper.toEntity(any(CreateOrcamentoAnualDTO.class))).thenReturn(rascunho());
    }

    // ── filterBy ─────────────────────────────────────────────────────────

    @Test
    void filterBy_returnsMatchingBudgets() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(rascunho()));

        var result = service.filterBy(new OrcamentoAnualFilterDTO(2026, null));

        assertThat(result).hasSize(1);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(rascunho()));

        assertThat(service.findById(1L)).isPresent();
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesRascunho() {
        var entity = rascunho();
        when(repository.save(any())).thenReturn(entity);

        var result = service.create(new CreateOrcamentoAnualDTO(2026));

        assertThat(result.status()).isEqualTo(StatusOrcamento.RASCUNHO);
        verify(repository).save(any(OrcamentoAnual.class));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_rascunho_updatesExercicio() {
        var entity = rascunho();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        service.update(1L, new CreateOrcamentoAnualDTO(2027));

        assertThat(entity.getExercicio()).isEqualTo(2027);
    }

    @Test
    void update_aprovado_throwsConflict() {
        var aprovado = OrcamentoAnual.builder().id(1L).exercicio(2026)
                .status(StatusOrcamento.APROVADO).build();
        when(repository.findById(1L)).thenReturn(Optional.of(aprovado));

        assertThatThrownBy(() -> service.update(1L, new CreateOrcamentoAnualDTO(2026)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    // ── aprovar ───────────────────────────────────────────────────────────

    @Test
    void aprovar_computesTaxaAndSetsAprovado() {
        var entity = rascunho();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByExercicioAndStatus(2026, StatusOrcamento.APROVADO)).thenReturn(false);
        var item = ItemOrcamento.builder().valorPrevisto(new BigDecimal("12000.00")).build();
        when(itemRepository.findByOrcamentoAnualId(1L)).thenReturn(List.of(item));
        when(repository.save(entity)).thenReturn(entity);

        service.aprovar(1L, new AprovarOrcamentoDTO(10));

        assertThat(entity.getStatus()).isEqualTo(StatusOrcamento.APROVADO);
        // taxa = 12000 / (12 * 10) = 100.00
        assertThat(entity.getTaxaEstimadaUnidade()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void aprovar_whenAlreadyApprovedForYear_throwsConflict() {
        when(repository.findById(1L)).thenReturn(Optional.of(rascunho()));
        when(repository.existsByExercicioAndStatus(2026, StatusOrcamento.APROVADO)).thenReturn(true);

        assertThatThrownBy(() -> service.aprovar(1L, new AprovarOrcamentoDTO(10)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    // ── encerrar ──────────────────────────────────────────────────────────

    @Test
    void encerrar_aprovado_setsEncerrado() {
        var entity = OrcamentoAnual.builder().id(1L).exercicio(2026)
                .status(StatusOrcamento.APROVADO).itens(List.of()).build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        service.encerrar(1L);

        assertThat(entity.getStatus()).isEqualTo(StatusOrcamento.ENCERRADO);
    }

    @Test
    void encerrar_rascunho_throwsConflict() {
        when(repository.findById(1L)).thenReturn(Optional.of(rascunho()));

        assertThatThrownBy(() -> service.encerrar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_rascunho_softDeletesItemsAndBudget() {
        when(repository.findById(1L)).thenReturn(Optional.of(rascunho()));

        service.delete(1L);

        verify(itemRepository).softDeleteByOrcamentoAnualId(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void delete_aprovado_throwsConflict() {
        var aprovado = OrcamentoAnual.builder().id(1L).exercicio(2026)
                .status(StatusOrcamento.APROVADO).build();
        when(repository.findById(1L)).thenReturn(Optional.of(aprovado));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }

    // ── addItem ───────────────────────────────────────────────────────────

    @Test
    void addItem_rascunho_savesItem() {
        var entity = rascunho();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        var savedItem = ItemOrcamento.builder().id(10L).valorPrevisto(new BigDecimal("500")).build();
        when(itemRepository.save(any())).thenReturn(savedItem);
        when(mapper.toItemDTO(savedItem)).thenReturn(
                new ItemOrcamentoDTO(10L, 5L, "Limpeza", TipoConta.DESPESA, new BigDecimal("500"), BigDecimal.ZERO));

        var result = service.addItem(1L, new CreateItemOrcamentoDTO(5L, new BigDecimal("500")));

        assertThat(result.id()).isEqualTo(10L);
        verify(itemRepository).save(any(ItemOrcamento.class));
    }

    // ── deleteItem ────────────────────────────────────────────────────────

    @Test
    void deleteItem_rascunho_softDeletesItem() {
        when(repository.findById(1L)).thenReturn(Optional.of(rascunho()));
        var item = ItemOrcamento.builder().id(10L).valorPrevisto(BigDecimal.ONE).build();
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

        service.deleteItem(1L, 10L);

        verify(itemRepository).delete(item);
    }
}
