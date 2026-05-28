package com.pmrodrigues.commons.mapper;

import com.pmrodrigues.commons.dto.EnderecoDTO;
import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EstadoMapperImpl.class, EnderecoMapperImpl.class})
class EnderecoMapperTest {

    @Autowired EnderecoMapper mapper;

    private final Estado estado = new Estado(1L, "São Paulo", "SP");
    private final Endereco endereco = new Endereco("Rua das Flores, 123", "01310100", "São Paulo", estado);

    private final EstadoDTO estadoDTO = new EstadoDTO(1L, "São Paulo", "SP");
    private final EnderecoDTO enderecoDTO = new EnderecoDTO("Rua das Flores, 123", "01310100", "São Paulo", estadoDTO);

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsAllFieldsIncludingEstado() {
        var dto = mapper.toDTO(endereco);

        assertThat(dto.logradouro()).isEqualTo("Rua das Flores, 123");
        assertThat(dto.cep()).isEqualTo("01310100");
        assertThat(dto.cidade()).isEqualTo("São Paulo");
        assertThat(dto.estado().uf()).isEqualTo("SP");
        assertThat(dto.estado().nome()).isEqualTo("São Paulo");
    }

    @Test
    void toDTO_whenNull_returnsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    // ── toEntity ──────────────────────────────────────────────────────────

    @Test
    void toEntity_mapsAllFields() {
        var entity = mapper.toEntity(enderecoDTO);

        assertThat(entity.getLogradouro()).isEqualTo("Rua das Flores, 123");
        assertThat(entity.getCep()).isEqualTo("01310100");
        assertThat(entity.getCidade()).isEqualTo("São Paulo");
        assertThat(entity.getEstado().getUf()).isEqualTo("SP");
    }

    @Test
    void toEntity_whenNull_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}