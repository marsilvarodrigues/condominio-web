package com.pmrodrigues.morador.mapper;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.dto.CreatePessoaDTO;
import com.pmrodrigues.morador.dto.UpdatePessoaDTO;
import com.pmrodrigues.morador.model.Morador;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PessoaMapperImpl.class)
class PessoaMapperTest {

    @Autowired PessoaMapper mapper;
    @MockitoBean EntityManager em;

    private Apartamento aptRef;

    @BeforeEach
    void setUp() {
        aptRef = new Apartamento();
        aptRef.setId(10L);
        aptRef.setNumero("101");
        when(em.getReference(Apartamento.class, 10L)).thenReturn(aptRef);
    }

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsAllFields_forMorador() {
        var m = new Morador();
        m.setId(1L);
        m.setName("Carlos");
        m.setEmail("carlos@test.com");
        m.setTelefone("21999990000");
        m.setCpf("111.111.111-11");
        m.setApartamento(aptRef);

        var dto = mapper.toDTO(m);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nome()).isEqualTo("Carlos");
        assertThat(dto.tipo()).isEqualTo("MORADOR");
        assertThat(dto.cpf()).isEqualTo("111.111.111-11");
        assertThat(dto.apartamentoId()).isEqualTo(10L);
        assertThat(dto.apartamentoNumero()).isEqualTo("101");
        assertThat(dto.userId()).isEqualTo(1L);
    }

    // ── toEntity ──────────────────────────────────────────────────────────

    @Test
    void toEntity_createsMorador() {
        var dto = new CreatePessoaDTO("Ana", "ana@test.com", null, "222.222.222-22", null);

        var result = mapper.toEntity(dto);

        assertThat(result).isInstanceOf(Morador.class);
        assertThat(result.getName()).isEqualTo("Ana");
        assertThat(((Morador) result).getCpf()).isEqualTo("222.222.222-22");
    }

    // ── updateEntity ──────────────────────────────────────────────────────

    @Test
    void updateEntity_updatesNonNullFields_ignoresNullFields() {
        var m = new Morador();
        m.setName("Antigo");
        m.setEmail("antigo@test.com");
        m.setCpf("000.000.000-00");

        mapper.updateEntity(m, new UpdatePessoaDTO("Novo Nome", null, null, null));

        assertThat(m.getName()).isEqualTo("Novo Nome");
        assertThat(m.getEmail()).isEqualTo("antigo@test.com");
        assertThat(m.getCpf()).isEqualTo("000.000.000-00");
    }

    // ── apartamentoFromId ─────────────────────────────────────────────────

    @Test
    void apartamentoFromId_returnsProxy() {
        assertThat(mapper.apartamentoFromId(10L)).isSameAs(aptRef);
    }

    @Test
    void apartamentoFromId_whenNull_returnsNull() {
        assertThat(mapper.apartamentoFromId(null)).isNull();
    }
}
