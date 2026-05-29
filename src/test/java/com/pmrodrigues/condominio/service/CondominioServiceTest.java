package com.pmrodrigues.condominio.service;

import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CondominioFilterDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioEnderecoDTO;
import com.pmrodrigues.condominio.mapper.CondominioMapper;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import com.pmrodrigues.condominio.repository.BlocoRepository;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.financeiro.service.FundoReservaService;
import com.pmrodrigues.financeiro.service.OrcamentoAnualService;
import com.pmrodrigues.financeiro.service.PlanoContasService;
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
class CondominioServiceTest {

    @Mock CondominioRepository repository;
    @Mock CondominioMapper mapper;
    @Mock BlocoRepository blocoRepository;
    @Mock ApartamentoRepository apartamentoRepository;
    @Mock PlanoContasService planoContasService;
    @Mock FundoReservaService fundoReservaService;
    @Mock OrcamentoAnualService orcamentoAnualService;

    CondominioService service;

    @BeforeEach
    void setUp() {
        service = new CondominioService(repository, mapper, blocoRepository, apartamentoRepository,
                planoContasService, fundoReservaService, orcamentoAnualService);

        lenient().when(mapper.toEntity(any(CreateCondominioDTO.class))).thenAnswer(inv -> {
            CreateCondominioDTO d = inv.getArgument(0);
            return Condominio.builder().nome(d.nome()).cnpj(d.cnpj()).email(d.email()).build();
        });
        lenient().when(mapper.toDTO(any(Condominio.class))).thenAnswer(inv -> {
            Condominio c = inv.getArgument(0);
            return new CondominioDTO(c.getId(), c.getNome(), c.getCnpj(), c.getEmail(),
                    null, c.getCreatedAt(), c.getUpdatedAt());
        });
        lenient().doAnswer(inv -> {
            Condominio c = inv.getArgument(0);
            CondominioDTO d = inv.getArgument(1);
            if (d.nome() != null) c.setNome(d.nome());
            if (d.cnpj() != null) c.setCnpj(d.cnpj());
            if (d.email() != null) c.setEmail(d.email());
            return null;
        }).when(mapper).updateEntity(any(Condominio.class), any(CondominioDTO.class));
    }

    private Condominio condominio(Long id) {
        return Condominio.builder().id(id)
                .nome("Residencial Sol").cnpj("12.345.678/0001-99").email("sol@test.com").build();
    }

    // ── filterBy ─────────────────────────────────────────────────────────

    @Test
    void filterBy_withNome_returnsFilteredList() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(condominio(1L)));

        var result = service.filterBy(new CondominioFilterDTO("Residencial", null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nome()).isEqualTo("Residencial Sol");
        verify(repository).findAll(any(Specification.class));
    }

    @Test
    void filterBy_withCnpj_returnsFilteredList() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(condominio(1L)));

        var result = service.filterBy(new CondominioFilterDTO(null, "12.345.678/0001-99"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cnpj()).isEqualTo("12.345.678/0001-99");
    }

    @Test
    void filterBy_withBothParams_returnsFilteredList() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(condominio(1L)));

        var result = service.filterBy(new CondominioFilterDTO("Residencial", "12.345.678/0001-99"));

        assertThat(result).hasSize(1);
    }

    @Test
    void filterBy_withBothNull_returnsAll() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(condominio(1L), condominio(2L)));

        var result = service.filterBy(new CondominioFilterDTO(null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void filterBy_whenNone_returnsEmpty() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(service.filterBy(new CondominioFilterDTO("Inexistente", null))).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(condominio(1L)));

        var result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().cnpj()).isEqualTo("12.345.678/0001-99");
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    // ── findByCnpj ────────────────────────────────────────────────────────

    @Test
    void findByCnpj_whenFound_returnsDTO() {
        when(repository.findByCnpj("12.345.678/0001-99")).thenReturn(Optional.of(condominio(1L)));

        var result = service.findByCnpj("12.345.678/0001-99");

        assertThat(result).isPresent();
    }

    @Test
    void findByCnpj_whenNotFound_returnsEmpty() {
        when(repository.findByCnpj("00.000.000/0000-00")).thenReturn(Optional.empty());

        assertThat(service.findByCnpj("00.000.000/0000-00")).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturnsDTO() {
        var endereco = new CreateCondominioEnderecoDTO("Rua A, 1", "01001000", "São Paulo", 1L);
        var dto = new CreateCondominioDTO("Novo Cond", "12.345.678/0001-95", "novo@test.com", endereco);
        when(repository.save(any(Condominio.class))).thenAnswer(inv -> {
            Condominio c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });

        var result = service.create(dto);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.nome()).isEqualTo("Novo Cond");
        assertThat(result.cnpj()).isEqualTo("12.345.678/0001-95");
        verify(repository).save(any(Condominio.class));
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesAndReturnsDTO() {
        var entity = condominio(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        var result = service.update(
                new CondominioDTO(1L, "Nome Novo", null, null, null, null, null));

        assertThat(result.nome()).isEqualTo("Nome Novo");
        assertThat(result.cnpj()).isEqualTo("12.345.678/0001-99");
    }

    @Test
    void update_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new CondominioDTO(99L, "X", null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_deletesEntity() {
        var entity = condominio(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void delete_cascadesSoftDeleteToBlocosAndApartamentos() {
        var entity = condominio(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(apartamentoRepository).softDeleteByCondominioId(1L);
        verify(blocoRepository).softDeleteByCondominioId(1L);
    }

    @Test
    void delete_cascadesSoftDeleteToFinanceiroServices() {
        var entity = condominio(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(planoContasService).softDeleteByCondominioId(1L);
        verify(fundoReservaService).softDeleteByCondominioId(1L);
        verify(orcamentoAnualService).softDeleteByCondominioId(1L);
    }

    @Test
    void delete_whenNotFound_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }
}
