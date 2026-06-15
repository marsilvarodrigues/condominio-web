package com.pmrodrigues.morador.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.model.HistoricoOcupacao;
import com.pmrodrigues.morador.model.Morador;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HistoricoOcupacaoMapperImpl.class)
class HistoricoOcupacaoMapperTest {

    @Autowired HistoricoOcupacaoMapper mapper;

    private HistoricoOcupacao entity(Long id) {
        var apt = new Apartamento();
        apt.setId(10L);
        var pessoa = new Morador();
        pessoa.setId(id);
        pessoa.setName("Morador " + id);
        pessoa.setEmail("morador" + id + "@test.com");
        pessoa.setCpf("111.222.333-4" + id);
        return HistoricoOcupacao.builder()
            .id(id)
            .apartamento(apt)
            .pessoa(pessoa)
            .dataEntrada(LocalDate.of(2023, 1, 1))
            .dataSaida(LocalDate.of(2024, 6, 30))
            .build();
    }

    @Test
    void toDTO_deveMapearTodosOsCampos() {
        var e = entity(1L);

        var dto = mapper.toDTO(e);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.apartamentoId()).isEqualTo(10L);
        assertThat(dto.pessoaId()).isEqualTo(1L);
        assertThat(dto.nomeMorador()).isEqualTo("Morador 1");
        assertThat(dto.emailMorador()).isEqualTo("morador1@test.com");
        assertThat(dto.cpfMorador()).isEqualTo("111.222.333-41");
        assertThat(dto.dataEntrada()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(dto.dataSaida()).isEqualTo(LocalDate.of(2024, 6, 30));
        assertThat(dto.createdAt()).isNull();
    }

    @Test
    void toDTOList_deveMapearListaCompleta() {
        var lista = List.of(entity(1L), entity(2L), entity(3L));

        var result = mapper.toDTOList(lista);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).pessoaId()).isEqualTo(1L);
        assertThat(result.get(1).pessoaId()).isEqualTo(2L);
        assertThat(result.get(2).pessoaId()).isEqualTo(3L);
    }

    @Test
    void toDTOList_deveRetornarListaVazia_paraListaVazia() {
        var result = mapper.toDTOList(List.of());

        assertThat(result).isNotNull().isEmpty();
    }
}
