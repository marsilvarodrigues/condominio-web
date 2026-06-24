package com.pmrodrigues.morador.service;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.dto.ApartamentoResumoDTO;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.service.CondominioService;
import com.pmrodrigues.morador.dto.CreateProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioFilterDTO;
import com.pmrodrigues.morador.dto.UpdateProprietarioDTO;
import com.pmrodrigues.morador.mapper.ProprietarioMapper;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import com.pmrodrigues.morador.model.Proprietario;
import com.pmrodrigues.morador.repository.ProprietarioRepository;
import com.pmrodrigues.security.mapper.UserMapper;
import com.pmrodrigues.security.repository.PasswordHistoryRepository;
import com.pmrodrigues.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProprietarioServiceTest {

    @Mock ProprietarioRepository repository;
    @Mock ProprietarioMapper mapper;
    @Mock UserRepository userRepository;
    @Mock MailService mailService;
    @Mock UserMapper userMapper;
    @Mock PasswordHistoryRepository passwordHistoryRepository;
    @Mock CondominioService condominioService;
    @Mock PasswordEncoder passwordEncoder;

    ProprietarioService service;

    @BeforeEach
    void setUp() {
        service = new ProprietarioService(
                userRepository, mailService, userMapper,
                passwordHistoryRepository, condominioService, passwordEncoder,
                repository, mapper);

        lenient().when(mapper.toDTO(any(Proprietario.class))).thenAnswer(inv -> {
            Proprietario p = inv.getArgument(0);
            List<ApartamentoResumoDTO> apts = p.getApartamentos().stream()
                    .map(a -> new ApartamentoResumoDTO(a.getId(), a.getNumero(), null, null))
                    .toList();
            return new ProprietarioDTO(p.getId(), p.getName(), "PROP_PF",
                    null, null, null,
                    p.getEmail(), p.getTelefone(),
                    apts, p.getId(), p.getCreatedAt(), p.getUpdatedAt());
        });

        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().doNothing().when(mailService).sendEmail(any(), any());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
    }

    private Apartamento apartamento(Long id) {
        var a = new Apartamento();
        a.setId(id);
        a.setNumero("101");
        return a;
    }

    private ProprietarioPessoaFisica proprietario(Long id) {
        ProprietarioPessoaFisica p = new ProprietarioPessoaFisica();
        p.setId(id);
        p.setName("Fulano");
        p.setEmail("fulano@test.com");
        p.setCpf("123.456.789-09");
        return p;
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_deveInstanciarProprietarioPessoaFisica() {
        var dto = new CreateProprietarioDTO("Fulano", "fulano@test.com", null, "PROP_PF", "111.111.111-11", null, null);
        var entity = proprietario(null);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(userRepository.save(entity)).thenAnswer(inv -> {
            Proprietario p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        var result = service.create(dto);

        assertThat(result.id()).isEqualTo(10L);
        verify(userRepository).save(entity);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_whenExists_updatesFields() {
        var entity = proprietario(1L);
        var updateDTO = new UpdateProprietarioDTO("Novo Nome", null, null, null, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        doNothing().when(mapper).updateEntity(any(UpdateProprietarioDTO.class), any());

        service.update(1L, updateDTO);

        verify(mapper).updateEntity(eq(updateDTO), eq(entity));
        verify(repository).save(entity);
    }

    @Test
    void update_whenNotFound_throws404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateProprietarioDTO(null, null, null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_deveRetornarDTO_quandoExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(proprietario(1L)));

        var result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findById_deveLancar404_quandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── findByApartamento ─────────────────────────────────────────────────────

    @Test
    void findByApartamento_deveRetornarLista() {
        when(repository.findByApartamentosId(1L)).thenReturn(List.of(proprietario(1L)));

        var result = service.findByApartamento(1L);

        assertThat(result).hasSize(1);
    }

    // ── associarApartamento ───────────────────────────────────────────────────

    @Test
    void associarApartamento_deveAdicionarApartamento() {
        var prop = proprietario(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(prop));
        var aptRef = apartamento(5L);
        when(mapper.apartamentoFromId(5L)).thenReturn(aptRef);
        when(repository.save(prop)).thenReturn(prop);

        service.associarApartamento(1L, 5L);

        assertThat(prop.getApartamentos()).contains(aptRef);
        verify(repository).save(prop);
    }

    @Test
    void associarApartamento_deveLancar404_quandoProprietarioNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.associarApartamento(99L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── desassociarApartamento ────────────────────────────────────────────────

    @Test
    void desassociarApartamento_deveRemoverApartamento() {
        var prop = proprietario(1L);
        var apt = apartamento(5L);
        prop.getApartamentos().add(apt);
        when(repository.findById(1L)).thenReturn(Optional.of(prop));
        when(repository.save(prop)).thenReturn(prop);

        service.desassociarApartamento(1L, 5L);

        assertThat(prop.getApartamentos()).doesNotContain(apt);
        verify(repository).save(prop);
    }

    // ── filterBy ─────────────────────────────────────────────────────────────

    @Test
    void filterBy_deveRetornarPaginaFiltrada() {
        var pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(proprietario(1L))));

        var result = service.filterBy(new ProprietarioFilterDTO(null, null, null), pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_deveFazerSoftDelete() {
        var prop = proprietario(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(prop));

        service.delete(1L);

        verify(repository).delete(prop);
    }

    @Test
    void delete_deveLancar404_quandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }
}
