package com.pmrodrigues.morador.mapper;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.morador.dto.CreateProprietarioDTO;
import com.pmrodrigues.morador.dto.UpdateProprietarioDTO;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import com.pmrodrigues.morador.model.ProprietarioPessoaJuridica;
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
@ContextConfiguration(classes = ProprietarioMapperImpl.class)
class ProprietarioMapperTest {

    @Autowired ProprietarioMapper mapper;
    @MockitoBean EntityManager em;

    private Apartamento aptRef;

    @BeforeEach
    void setUp() {
        var condominio = new Condominio();
        condominio.setId(7L);
        aptRef = Apartamento.builder().id(5L).numero("202").condominio(condominio).build();
        when(em.getReference(Apartamento.class, 5L)).thenReturn(aptRef);
    }

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsAllFields_forProprietarioPF() {
        var ppf = new ProprietarioPessoaFisica();
        ppf.setId(1L);
        ppf.setName("João");
        ppf.setEmail("joao@test.com");
        ppf.setTelefone("11999990000");
        ppf.setCpf("111.111.111-11");
        ppf.getApartamentos().add(aptRef);

        var dto = mapper.toDTO(ppf);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nome()).isEqualTo("João");
        assertThat(dto.tipo()).isEqualTo("PROP_PF");
        assertThat(dto.cpf()).isEqualTo("111.111.111-11");
        assertThat(dto.cnpj()).isNull();
        assertThat(dto.apartamentos()).hasSize(1);
        assertThat(dto.apartamentos().get(0).id()).isEqualTo(5L);
        assertThat(dto.apartamentos().get(0).condominioId()).isEqualTo(7L);
        assertThat(dto.userId()).isEqualTo(1L);
    }

    @Test
    void toDTO_mapsAllFields_forProprietarioPJ() {
        var ppj = new ProprietarioPessoaJuridica();
        ppj.setId(2L);
        ppj.setName("Empresa SA");
        ppj.setEmail("empresa@test.com");
        ppj.setCnpj("12.345.678/0001-90");
        ppj.setRazaoSocial("Empresa Sociedade Anônima");

        var dto = mapper.toDTO(ppj);

        assertThat(dto.tipo()).isEqualTo("PROP_PJ");
        assertThat(dto.cnpj()).isEqualTo("12.345.678/0001-90");
        assertThat(dto.razaoSocial()).isEqualTo("Empresa Sociedade Anônima");
        assertThat(dto.cpf()).isNull();
        assertThat(dto.apartamentos()).isEmpty();
    }

    // ── toEntity ──────────────────────────────────────────────────────────

    @Test
    void toEntity_createsProprietarioPF() {
        var dto = new CreateProprietarioDTO("Maria", "maria@test.com", null, "PROP_PF", "222.222.222-22", null, null);

        var result = mapper.toEntity(dto);

        assertThat(result).isInstanceOf(ProprietarioPessoaFisica.class);
        assertThat(result.getName()).isEqualTo("Maria");
        assertThat(((ProprietarioPessoaFisica) result).getCpf()).isEqualTo("222.222.222-22");
    }

    @Test
    void toEntity_createsProprietarioPJ() {
        var dto = new CreateProprietarioDTO("Corp", "corp@test.com", null, "PROP_PJ", null, "11.222.333/0001-44", "Corp Ltda");

        var result = mapper.toEntity(dto);

        assertThat(result).isInstanceOf(ProprietarioPessoaJuridica.class);
        assertThat(result.getName()).isEqualTo("Corp");
        assertThat(((ProprietarioPessoaJuridica) result).getCnpj()).isEqualTo("11.222.333/0001-44");
        assertThat(((ProprietarioPessoaJuridica) result).getRazaoSocial()).isEqualTo("Corp Ltda");
    }

    // ── updateEntity ──────────────────────────────────────────────────────

    @Test
    void updateEntity_updatesNonNullFields_ignoresNullFields() {
        var ppf = new ProprietarioPessoaFisica();
        ppf.setName("Antigo");
        ppf.setEmail("antigo@test.com");
        ppf.setCpf("000.000.000-00");

        mapper.updateEntity(new UpdateProprietarioDTO("Novo Nome", null, null, null, null, null), ppf);

        assertThat(ppf.getName()).isEqualTo("Novo Nome");
        assertThat(ppf.getEmail()).isEqualTo("antigo@test.com");
        assertThat(ppf.getCpf()).isEqualTo("000.000.000-00");
    }

    // ── apartamentoFromId ─────────────────────────────────────────────────

    @Test
    void apartamentoFromId_returnsProxy() {
        assertThat(mapper.apartamentoFromId(5L)).isSameAs(aptRef);
    }

    @Test
    void apartamentoFromId_whenNull_returnsNull() {
        assertThat(mapper.apartamentoFromId(null)).isNull();
    }
}
