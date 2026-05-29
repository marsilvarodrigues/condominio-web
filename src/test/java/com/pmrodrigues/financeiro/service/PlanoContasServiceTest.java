package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.CreatePlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasFilterDTO;
import com.pmrodrigues.financeiro.mapper.PlanoContasMapper;
import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.PlanoContas;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.model.TipoRateio;
import com.pmrodrigues.financeiro.repository.PlanoContasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanoContasServiceTest {

    @Mock PlanoContasRepository repository;
    @Mock PlanoContasMapper mapper;

    PlanoContasService service;

    @BeforeEach
    void setUp() {
        service = new PlanoContasService(repository, mapper);

        lenient().when(mapper.toDTO(any(PlanoContas.class))).thenAnswer(inv -> {
            PlanoContas pc = inv.getArgument(0);
            return new PlanoContasDTO(pc.getId(), pc.getCodigo(), pc.getDescricao(),
                    pc.getTipo(), pc.getTipoRateio(), pc.getEscopoRateio(), null, null, null);
        });
        lenient().when(mapper.toEntity(any(CreatePlanoContasDTO.class))).thenAnswer(inv -> {
            CreatePlanoContasDTO dto = inv.getArgument(0);
            return PlanoContas.builder().codigo(dto.codigo()).descricao(dto.descricao())
                    .tipo(dto.tipo()).tipoRateio(dto.tipoRateio()).escopoRateio(dto.escopoRateio()).build();
        });
    }

    private PlanoContas conta(Long id, String codigo, TipoConta tipo) {
        return PlanoContas.builder().id(id).codigo(codigo).descricao("Desc " + codigo).tipo(tipo)
                .filhos(new ArrayList<>()).build();
    }

    private PlanoContas contaComRateio(Long id, String codigo, TipoConta tipo,
                                       TipoRateio tipoRateio, EscopoRateio escopoRateio) {
        return PlanoContas.builder().id(id).codigo(codigo).descricao("Desc " + codigo).tipo(tipo)
                .tipoRateio(tipoRateio).escopoRateio(escopoRateio).filhos(new ArrayList<>()).build();
    }

    // ── filterBy ─────────────────────────────────────────────────────────

    @Test
    void filterBy_withTipo_returnsFiltered() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(conta(1L, "1", TipoConta.RECEITA)));

        var result = service.filterBy(new PlanoContasFilterDTO(TipoConta.RECEITA, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tipo()).isEqualTo(TipoConta.RECEITA);
    }

    @Test
    void filterBy_withNoFilter_returnsAll() {
        when(repository.findAll(any(Specification.class))).thenReturn(
                List.of(conta(1L, "1", TipoConta.RECEITA), conta(2L, "2", TipoConta.DESPESA)));

        var result = service.filterBy(new PlanoContasFilterDTO(null, null));

        assertThat(result).hasSize(2);
    }

    // ── getArvore ─────────────────────────────────────────────────────────

    @Test
    void getArvore_returnsRootsWithChildren() {
        var filho = conta(2L, "1.1", TipoConta.RECEITA);
        var raiz = PlanoContas.builder().id(1L).codigo("1").descricao("Raiz").tipo(TipoConta.RECEITA)
                .filhos(List.of(filho)).build();
        when(repository.findAllByPaiIsNull()).thenReturn(List.of(raiz));

        var tree = service.getArvore();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).filhos()).hasSize(1);
        assertThat(tree.get(0).filhos().get(0).codigo()).isEqualTo("1.1");
    }

    @Test
    void getArvore_whenEmpty_returnsEmptyList() {
        when(repository.findAllByPaiIsNull()).thenReturn(List.of());

        assertThat(service.getArvore()).isEmpty();
    }

    @Test
    void getArvore_propagatesTipoRateioAndEscopo() {
        var raiz = contaComRateio(1L, "2", TipoConta.DESPESA, TipoRateio.FRACAO_IDEAL, EscopoRateio.POR_BLOCO);
        when(repository.findAllByPaiIsNull()).thenReturn(List.of(raiz));

        var tree = service.getArvore();

        assertThat(tree.get(0).tipoRateio()).isEqualTo(TipoRateio.FRACAO_IDEAL);
        assertThat(tree.get(0).escopoRateio()).isEqualTo(EscopoRateio.POR_BLOCO);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(conta(1L, "1", TipoConta.RECEITA)));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().codigo()).isEqualTo("1");
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsDTO() {
        var dto = new CreatePlanoContasDTO("2", "Manutenção", TipoConta.DESPESA, null, null, null);
        when(repository.save(any(PlanoContas.class))).thenAnswer(inv -> {
            PlanoContas pc = inv.getArgument(0);
            pc.setId(5L);
            return pc;
        });

        var result = service.create(dto);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.codigo()).isEqualTo("2");
        verify(repository).save(any(PlanoContas.class));
    }

    @Test
    void create_withRateioAndEscopo_mapsFields() {
        var dto = new CreatePlanoContasDTO("3", "Taxa", TipoConta.RECEITA, null,
                TipoRateio.IGUALITARIO, EscopoRateio.TODOS);
        when(repository.save(any(PlanoContas.class))).thenAnswer(inv -> {
            PlanoContas pc = inv.getArgument(0);
            pc.setId(7L);
            return pc;
        });

        var result = service.create(dto);

        assertThat(result.tipoRateio()).isEqualTo(TipoRateio.IGUALITARIO);
        assertThat(result.escopoRateio()).isEqualTo(EscopoRateio.TODOS);
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesAndReturns() {
        var entity = conta(1L, "1", TipoConta.RECEITA);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        var dto = new PlanoContasDTO(1L, "1-updated", "Nova desc", TipoConta.RECEITA,
                TipoRateio.IGUALITARIO, EscopoRateio.TODOS, null, null, null);
        var result = service.update(dto);

        assertThat(result).isNotNull();
        verify(mapper).updateEntity(entity, dto);
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new PlanoContasDTO(99L, "x", "y", TipoConta.RECEITA, null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFoundAndNoChildren_deletes() {
        var entity = conta(1L, "1", TipoConta.RECEITA);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByPaiId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void delete_whenHasChildren_throwsConflict() {
        var entity = conta(1L, "1", TipoConta.RECEITA);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByPaiId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
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
