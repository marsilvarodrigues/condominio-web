package com.pmrodrigues.morador.service;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.service.CondominioService;
import com.pmrodrigues.morador.dto.CreatePessoaDTO;
import com.pmrodrigues.morador.dto.HistoricoOcupacaoDTO;
import com.pmrodrigues.morador.dto.PessoaDTO;
import com.pmrodrigues.morador.dto.PessoaFilterDTO;
import com.pmrodrigues.morador.dto.UpdatePessoaDTO;
import com.pmrodrigues.morador.mapper.PessoaMapper;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import com.pmrodrigues.morador.repository.PessoaRepository;
import com.pmrodrigues.morador.repository.ProprietarioRepository;
import com.pmrodrigues.security.mapper.UserMapper;
import com.pmrodrigues.security.repository.PasswordHistoryRepository;
import com.pmrodrigues.security.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @Mock PessoaRepository pessoaRepository;
    @Mock PessoaMapper pessoaMapper;
    @Mock ProprietarioRepository proprietarioRepository;
    @Mock UserRepository userRepository;
    @Mock MailService mailService;
    @Mock UserMapper userMapper;
    @Mock PasswordHistoryRepository passwordHistoryRepository;
    @Mock CondominioService condominioService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock HistoricoOcupacaoService historicoOcupacaoService;

    PessoaService service;

    @BeforeEach
    void setUp() {
        service = new PessoaService(
                userRepository, mailService, userMapper,
                passwordHistoryRepository, condominioService, passwordEncoder,
                pessoaRepository, pessoaMapper, proprietarioRepository,
                historicoOcupacaoService);

        lenient().when(pessoaMapper.toEntity(any(CreatePessoaDTO.class))).thenAnswer(inv -> {
            CreatePessoaDTO d = inv.getArgument(0);
            Morador m = new Morador();
            m.setName(d.nome());
            m.setEmail(d.email());
            m.setCpf(d.cpf());
            return m;
        });
        lenient().when(pessoaMapper.toDTO(any())).thenAnswer(inv -> {
            var p = inv.getArgument(0);
            if (p instanceof Morador m) {
                return new PessoaDTO(m.getId(), m.getName(), "MORADOR", m.getCpf(),
                        m.getEmail(), m.getTelefone(), null, null, m.getId(),
                        m.getCreatedAt(), m.getUpdatedAt());
            }
            if (p instanceof com.pmrodrigues.morador.model.Pessoa pp) {
                return new PessoaDTO(pp.getId(), pp.getName(), "MORADOR", null,
                        pp.getEmail(), pp.getTelefone(), null, null, pp.getId(),
                        pp.getCreatedAt(), pp.getUpdatedAt());
            }
            return null;
        });
        lenient().doAnswer(inv -> {
            var p = inv.getArgument(0, com.pmrodrigues.morador.model.Pessoa.class);
            UpdatePessoaDTO d = inv.getArgument(1);
            if (d.nome() != null) p.setName(d.nome());
            if (d.email() != null) p.setEmail(d.email());
            return null;
        }).when(pessoaMapper).updateEntity(any(), any(UpdatePessoaDTO.class));

        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().doNothing().when(mailService).sendEmail(any(), any());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
    }

    private Morador morador(Long id, String nome) {
        Morador m = new Morador();
        m.setId(id);
        m.setName(nome);
        m.setEmail(nome.toLowerCase().replace(" ", ".") + "@test.com");
        m.setCpf("123.456.789-09");
        return m;
    }

    private Apartamento apt(Long id) {
        var a = new Apartamento();
        a.setId(id);
        return a;
    }

    // ── filterBy ─────────────────────────────────────────────────────────────

    @Test
    void filterBy_deveRetornarPaginaFiltrada() {
        var pageable = PageRequest.of(0, 20);
        when(pessoaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(morador(1L, "João Silva"))));

        var result = service.filterBy(new PessoaFilterDTO("João", null, null, null), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).nome()).isEqualTo("João Silva");
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_deveRetornarDTO_quandoExiste() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(morador(1L, "Maria Souza")));

        var result = service.findById(1L);

        assertThat(result.nome()).isEqualTo("Maria Souza");
    }

    @Test
    void findById_deveLancar404_quandoNaoExiste() {
        when(pessoaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_morador_devePersistirERetornarDTO() {
        var dto = new CreatePessoaDTO("Carlos", "carlos@test.com", null, "111.111.111-11", null);
        when(userRepository.save(any())).thenAnswer(inv -> {
            Morador m = (Morador) inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        var result = service.create(dto);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.nome()).isEqualTo("Carlos");
        verify(userRepository).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_deveAtualizarCamposNaoNulos() {
        var entity = morador(5L, "Antigo");
        when(pessoaRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(pessoaRepository.save(entity)).thenReturn(entity);

        var result = service.update(5L, new UpdatePessoaDTO("Novo Nome", "novo@email.com", null, null));

        assertThat(result.nome()).isEqualTo("Novo Nome");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_deveFazerSoftDelete_quandoSemVinculos() {
        var entity = morador(3L, "Fulano");
        when(pessoaRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(proprietarioRepository.findByApartamentosId(3L)).thenReturn(List.of());

        service.delete(3L);

        verify(pessoaRepository).delete(entity);
    }

    @Test
    void delete_deveLancar409_quandoPessoaTemApartamentoAtivo() {
        var entity = morador(3L, "Fulano");
        entity.setApartamento(apt(7L));
        when(pessoaRepository.findById(3L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void delete_deveLancar409_quandoPessoaTemPropriedadeAtiva() {
        var entity = morador(3L, "Fulano");
        when(pessoaRepository.findById(3L)).thenReturn(Optional.of(entity));
        var prop = new ProprietarioPessoaFisica();
        prop.setId(1L);
        when(proprietarioRepository.findByApartamentosId(3L)).thenReturn(List.of(prop));

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    // ── softDeleteByCondominioId ──────────────────────────────────────────────

    @Test
    void softDeleteByCondominioId_callsRepository() {
        service.softDeleteByCondominioId(42L);

        verify(pessoaRepository).softDeleteByCondominioId(42L);
    }

    // ── assignToApartamento ───────────────────────────────────────────────────

    @Test
    void assignToApartamento_deveAssociarApartamento() {
        var entity = morador(5L, "Fulano");
        when(pessoaRepository.findById(5L)).thenReturn(Optional.of(entity));
        var aptRef = apt(10L);
        when(pessoaMapper.apartamentoFromId(10L)).thenReturn(aptRef);
        when(pessoaRepository.save(entity)).thenReturn(entity);

        service.assignToApartamento(5L, 10L);

        assertThat(entity.getApartamento()).isEqualTo(aptRef);
        verify(pessoaRepository).save(entity);
    }

    // ── removeFromApartamento ─────────────────────────────────────────────────

    @Test
    void removeFromApartamento_deveRemoverApartamento() {
        var entity = morador(5L, "Fulano");
        entity.setApartamento(apt(10L));
        when(pessoaRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(pessoaRepository.save(entity)).thenReturn(entity);

        service.removeFromApartamento(5L);

        assertThat(entity.getApartamento()).isNull();
        verify(pessoaRepository).save(entity);
    }

    @Test
    void removeFromApartamento_deveRegistrarHistorico_quandoPossuiApartamento() {
        var entity = morador(5L, "Fulano");
        entity.setApartamento(apt(10L));
        when(pessoaRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(pessoaRepository.save(entity)).thenReturn(entity);
        var dto = new HistoricoOcupacaoDTO(1L, 10L, 5L, "Fulano", null, null,
            LocalDate.now(), LocalDate.now(), null);
        when(historicoOcupacaoService.registrar(eq(entity), any(LocalDate.class))).thenReturn(dto);

        service.removeFromApartamento(5L);

        verify(historicoOcupacaoService).registrar(eq(entity), any(LocalDate.class));
        assertThat(entity.getApartamento()).isNull();
    }

    @Test
    void removeFromApartamento_naoDeveRegistrarHistorico_quandoSemApartamento() {
        var entity = morador(5L, "Fulano");
        // apartamento já é null
        when(pessoaRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(pessoaRepository.save(entity)).thenReturn(entity);

        service.removeFromApartamento(5L);

        verify(historicoOcupacaoService, never()).registrar(any(), any());
    }

    // ── assignToApartamento com histórico ─────────────────────────────────────

    @Test
    void assignToApartamento_deveRegistrarHistoricoDosAnteriores_quandoApartamentoOcupado() {
        var novaPessoa = morador(10L, "Nova Pessoa");
        var anterior1  = morador(1L, "Anterior Um");
        anterior1.setApartamento(apt(20L));
        var anterior2  = morador(2L, "Anterior Dois");
        anterior2.setApartamento(apt(20L));

        when(pessoaRepository.findById(10L)).thenReturn(Optional.of(novaPessoa));
        when(pessoaRepository.findByApartamentoId(20L)).thenReturn(List.of(anterior1, anterior2));
        when(pessoaMapper.apartamentoFromId(20L)).thenReturn(apt(20L));
        when(pessoaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignToApartamento(10L, 20L);

        verify(historicoOcupacaoService).registrar(eq(anterior1), any(LocalDate.class));
        verify(historicoOcupacaoService).registrar(eq(anterior2), any(LocalDate.class));
        assertThat(anterior1.getApartamento()).isNull();
        assertThat(anterior2.getApartamento()).isNull();
    }

    @Test
    void assignToApartamento_naoDeveRegistrarHistorico_quandoApartamentoVazio() {
        var novaPessoa = morador(10L, "Nova Pessoa");
        when(pessoaRepository.findById(10L)).thenReturn(Optional.of(novaPessoa));
        when(pessoaRepository.findByApartamentoId(20L)).thenReturn(List.of());
        when(pessoaMapper.apartamentoFromId(20L)).thenReturn(apt(20L));
        when(pessoaRepository.save(novaPessoa)).thenReturn(novaPessoa);

        service.assignToApartamento(10L, 20L);

        verify(historicoOcupacaoService, never()).registrar(any(), any());
    }

    // ── findByIds ──────────────────────────────────────────────────────────────

    @Test
    void findByIds_deveRetornarMapaDTOs_quandoExistemPessoas() {
        var m1 = morador(1L, "Ana");
        var m2 = morador(2L, "Bia");
        when(pessoaRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(m1, m2));

        Map<Long, PessoaDTO> result = service.findByIds(Set.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L).nome()).isEqualTo("Ana");
        assertThat(result.get(2L).nome()).isEqualTo("Bia");
    }

    @Test
    void findByIds_deveRetornarMapaVazio_quandoIdsVazios() {
        Map<Long, PessoaDTO> result = service.findByIds(Set.of());

        assertThat(result).isEmpty();
        verify(pessoaRepository, never()).findAllById(any());
    }

    @Test
    void assignToApartamento_naoDeveRegistrarHistoricoDaPropriaPessoa_quandoJaEstaNoApartamento() {
        var pessoa = morador(10L, "Fulano");
        pessoa.setApartamento(apt(20L));
        when(pessoaRepository.findById(10L)).thenReturn(Optional.of(pessoa));
        when(pessoaRepository.findByApartamentoId(20L)).thenReturn(List.of(pessoa));
        when(pessoaMapper.apartamentoFromId(20L)).thenReturn(apt(20L));
        when(pessoaRepository.save(pessoa)).thenReturn(pessoa);

        service.assignToApartamento(10L, 20L);

        verify(historicoOcupacaoService, never()).registrar(any(), any());
    }
}
