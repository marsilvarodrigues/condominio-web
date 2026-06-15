package com.pmrodrigues.morador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.dto.HistoricoOcupacaoDTO;
import com.pmrodrigues.morador.mapper.HistoricoOcupacaoMapper;
import com.pmrodrigues.morador.model.HistoricoOcupacao;
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.model.Pessoa;
import com.pmrodrigues.morador.repository.HistoricoOcupacaoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HistoricoOcupacaoServiceTest {

    @Mock HistoricoOcupacaoRepository historicoRepository;
    @Mock HistoricoOcupacaoMapper historicoMapper;

    HistoricoOcupacaoService service;

    @BeforeEach
    void setUp() {
        service = new HistoricoOcupacaoService(historicoRepository, historicoMapper);
        TenantContext.setCondominioId(1L);
        lenient().when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(historicoMapper.toDTO(any())).thenAnswer(inv -> {
            HistoricoOcupacao e = inv.getArgument(0);
            Long aptId = e.getApartamento() != null ? e.getApartamento().getId() : null;
            Long pId = e.getPessoa() != null ? e.getPessoa().getId() : null;
            String nome = e.getPessoa() != null ? e.getPessoa().getName() : null;
            String email = e.getPessoa() != null ? e.getPessoa().getEmail() : null;
            String cpf = e.getPessoa() instanceof Morador m ? m.getCpf() : null;
            return new HistoricoOcupacaoDTO(e.getId(), aptId, pId, nome, email, cpf,
                e.getDataEntrada(), e.getDataSaida(), e.getCreatedAt());
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Morador morador(Long id, String nome, String cpf) {
        var m = new Morador();
        m.setId(id);
        m.setName(nome);
        m.setEmail(nome.toLowerCase().replace(" ", ".") + "@test.com");
        m.setCpf(cpf);
        m.setEnabled(true);
        m.setRoles(new HashSet<>(Set.of("ROLE_USER", "ROLE_MORADOR")));
        var apt = new Apartamento();
        apt.setId(10L);
        m.setApartamento(apt);
        return m;
    }

    // ── registrar ────────────────────────────────────────────────────────────────

    @Test
    void registrar_devePersistirRegistroComRelacionamentos() {
        var m = morador(1L, "João Silva", "123.456.789-09");
        ReflectionTestUtils.setField(m, "createdAt", LocalDateTime.of(2024, 1, 15, 10, 0));

        service.registrar(m, LocalDate.of(2026, 6, 11));

        var captor = ArgumentCaptor.forClass(HistoricoOcupacao.class);
        verify(historicoRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getApartamento().getId()).isEqualTo(10L);
        assertThat(saved.getPessoa().getId()).isEqualTo(1L);
        assertThat(saved.getDataEntrada()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(saved.getDataSaida()).isEqualTo(LocalDate.of(2026, 6, 11));
    }

    @Test
    void registrar_deveUsarDataHoje_quandoCreatedAtEhNull() {
        var m = morador(3L, "Sem Data", "111.111.111-11");

        service.registrar(m, LocalDate.of(2026, 6, 11));

        var captor = ArgumentCaptor.forClass(HistoricoOcupacao.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getDataEntrada()).isEqualTo(LocalDate.now());
    }

    @Test
    void registrar_deveUsarCreatedAt_quandoDisponivel() {
        var m = morador(4L, "Com Data", "222.222.222-22");
        ReflectionTestUtils.setField(m, "createdAt", LocalDateTime.of(2024, 1, 15, 10, 0));

        service.registrar(m, LocalDate.of(2026, 6, 11));

        var captor = ArgumentCaptor.forClass(HistoricoOcupacao.class);
        verify(historicoRepository).save(captor.capture());
        assertThat(captor.getValue().getDataEntrada()).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    // ── listarPorApartamento ──────────────────────────────────────────────────────

    @Test
    void listarPorApartamento_deveRetornarListaOrdenadaPorDataSaidaDesc() {
        var apt = new Apartamento();
        apt.setId(10L);
        var p1 = new Morador(); p1.setId(1L);
        var p2 = new Morador(); p2.setId(2L);
        var p3 = new Morador(); p3.setId(3L);

        var e1 = HistoricoOcupacao.builder().id(1L).apartamento(apt).pessoa(p1)
            .dataEntrada(LocalDate.of(2023, 1, 1)).dataSaida(LocalDate.of(2024, 1, 1)).build();
        var e2 = HistoricoOcupacao.builder().id(2L).apartamento(apt).pessoa(p2)
            .dataEntrada(LocalDate.of(2021, 1, 1)).dataSaida(LocalDate.of(2022, 12, 31)).build();
        var e3 = HistoricoOcupacao.builder().id(3L).apartamento(apt).pessoa(p3)
            .dataEntrada(LocalDate.of(2019, 1, 1)).dataSaida(LocalDate.of(2020, 12, 31)).build();

        when(historicoRepository.findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc(
                eq(10L), eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(e1, e2, e3)));

        var result = service.listarPorApartamento(10L, 1L, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).pessoaId()).isEqualTo(1L);
    }

    @Test
    void listarPorApartamento_deveRetornarListaVazia_quandoNaoHaHistorico() {
        when(historicoRepository.findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc(
                eq(99L), eq(1L), any(Pageable.class)))
            .thenReturn(Page.empty());

        var result = service.listarPorApartamento(99L, 1L, Pageable.unpaged());

        assertThat(result.isEmpty()).isTrue();
    }

    // ── bloquearSeApenasResidente ─────────────────────────────────────────────────

    @Test
    void registrar_deveBloquearAcesso_quandoPessoaSoTemRoleMorador() {
        var m = morador(5L, "Morador Simples", "333.333.333-33");
        m.setRoles(new HashSet<>(Set.of("ROLE_USER", "ROLE_MORADOR")));
        m.setEnabled(true);

        service.registrar(m, LocalDate.now());

        assertThat(m.isEnabled()).isFalse();
    }

    @Test
    void registrar_naoDeveBloquear_quandoPessoaTemRoleProprietario() {
        var m = morador(6L, "Morador Proprietário", "444.444.444-44");
        m.setRoles(new HashSet<>(Set.of("ROLE_USER", "ROLE_MORADOR", "ROLE_PROPRIETARIO")));
        m.setEnabled(true);

        service.registrar(m, LocalDate.now());

        assertThat(m.isEnabled()).isTrue();
    }

    @Test
    void registrar_naoDeveBloquear_quandoPessoaTemRoleSindico() {
        var m = morador(7L, "Morador Síndico", "555.555.555-55");
        m.setRoles(new HashSet<>(Set.of("ROLE_USER", "ROLE_MORADOR", "ROLE_SINDICO")));
        m.setEnabled(true);

        service.registrar(m, LocalDate.now());

        assertThat(m.isEnabled()).isTrue();
    }

    @Test
    void registrar_naoDeveBloquear_quandoPessoaTemAmbosRolesIndependentes() {
        var m = morador(8L, "Multi Role", "666.666.666-66");
        m.setRoles(new HashSet<>(Set.of("ROLE_USER", "ROLE_MORADOR", "ROLE_PROPRIETARIO", "ROLE_SINDICO")));
        m.setEnabled(true);

        service.registrar(m, LocalDate.now());

        assertThat(m.isEnabled()).isTrue();
    }
}
