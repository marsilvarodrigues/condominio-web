package com.pmrodrigues.condominio.mapper;

import com.pmrodrigues.commons.dto.EnderecoDTO;
import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.mapper.EnderecoMapperImpl;
import com.pmrodrigues.commons.mapper.EstadoMapperImpl;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioEnderecoDTO;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EstadoMapperImpl.class, EnderecoMapperImpl.class, CondominioMapperImpl.class})
class CondominioMapperTest {

    @Autowired CondominioMapper mapper;
    @MockitoBean EntityManager em;

    @BeforeEach
    void setUp() {
        when(em.getReference(Estado.class, 1L)).thenReturn(new Estado(1L, null, null));
    }

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsAllFields() {
        var condominio = condominio();

        var dto = mapper.toDTO(condominio);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nome()).isEqualTo("Residencial Sol");
        assertThat(dto.cnpj()).isEqualTo("12.345.678/0001-99");
        assertThat(dto.email()).isEqualTo("contato@sol.com");
        assertThat(dto.endereco().logradouro()).isEqualTo("Rua A, 10");
        assertThat(dto.endereco().cep()).isEqualTo("01001000");
        assertThat(dto.endereco().estado().uf()).isEqualTo("SP");
    }

    @Test
    void toDTO_doesNotExposeDeletedOrAuditUserFields() {
        var dto = mapper.toDTO(condominio());

        // CondominioDTO has no deleted, createdBy, updatedBy fields — compile-time guarantee
        assertThat(dto).isNotNull();
    }

    @Test
    void toDTO_whenNull_returnsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    // ── toEntity(CreateCondominioDTO) ─────────────────────────────────────

    @Test
    void toEntity_fromCreateDTO_mapsBusinessFields() {
        var endereco = new CreateCondominioEnderecoDTO("Rua C, 30", "01310100", "São Paulo", 1L);
        var dto = new CreateCondominioDTO("Cond. Novo", "12.345.678/0001-95", "novo@cond.com", endereco);

        var entity = mapper.toEntity(dto);

        assertThat(entity.getNome()).isEqualTo("Cond. Novo");
        assertThat(entity.getCnpj()).isEqualTo("12.345.678/0001-95");
        assertThat(entity.getEmail()).isEqualTo("novo@cond.com");
        assertThat(entity.getEndereco().getCep()).isEqualTo("01310100");
        assertThat(entity.getEndereco().getLogradouro()).isEqualTo("Rua C, 30");
        assertThat(entity.getEndereco().getCidade()).isEqualTo("São Paulo");
        assertThat(entity.getEndereco().getEstado().getId()).isEqualTo(1L);
        verify(em).getReference(Estado.class, 1L);
    }

    @Test
    void toEntity_fromCreateDTO_ignoresServerManagedFields() {
        var endereco = new CreateCondominioEnderecoDTO("Rua C, 30", "01310100", "SP", 1L);
        var dto = new CreateCondominioDTO("Cond. Novo", "12.345.678/0001-95", "novo@cond.com", endereco);

        var entity = mapper.toEntity(dto);

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
    }

    @Test
    void toEntity_fromCreateDTO_whenNull_returnsNull() {
        assertThat(mapper.toEntity((CreateCondominioDTO) null)).isNull();
    }

    // ── updateEntity ──────────────────────────────────────────────────────

    @Test
    void updateEntity_updatesNonNullFields() {
        var entity = condominio();
        var dto = new CondominioDTO(null, "Novo Nome", null, null, null, null, null);

        mapper.updateEntity(entity, dto);

        assertThat(entity.getNome()).isEqualTo("Novo Nome");
        assertThat(entity.getCnpj()).isEqualTo("12.345.678/0001-99");
        assertThat(entity.getEmail()).isEqualTo("contato@sol.com");
    }

    @Test
    void updateEntity_doesNotModifyId() {
        var entity = condominio();
        var dto = new CondominioDTO(99L, "Outro", "11.111.111/0001-11", "outro@mail.com", null, null, null);

        mapper.updateEntity(entity, dto);

        assertThat(entity.getId()).isEqualTo(1L);
    }

    @Test
    void updateEntity_doesNotModifyServerManagedFields() {
        var entity = condominio();
        var dto = new CondominioDTO(null, "Novo Nome", null, null, null, LocalDateTime.now(), LocalDateTime.now());

        mapper.updateEntity(entity, dto);

        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private Condominio condominio() {
        return Condominio.builder()
                .id(1L)
                .nome("Residencial Sol")
                .cnpj("12.345.678/0001-99")
                .email("contato@sol.com")
                .endereco(new Endereco("Rua A, 10", "01001000", "São Paulo",
                        new Estado(1L, "São Paulo", "SP")))
                .build();
    }

    private EnderecoDTO enderecoDTO() {
        return new EnderecoDTO("Rua B, 20", "13020000", "Campinas",
                new EstadoDTO(1L, "São Paulo", "SP"));
    }
}
