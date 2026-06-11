package com.pmrodrigues.morador.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.pmrodrigues.morador.model.HistoricoOcupacao;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private HistoricoOcupacao entity(Long id, String nome) {
        return HistoricoOcupacao.builder()
            .id(id)
            .condominioId(1L)
            .apartamentoId(10L)
            .pessoaId(id)
            .nomeMorador(nome)
            .emailMorador(nome.toLowerCase() + "@test.com")
            .cpfMorador("123.456.789-09")
            .dataEntrada(LocalDate.of(2023, 1, 1))
            .dataSaida(LocalDate.of(2024, 6, 30))
            .criadoEm(LocalDateTime.of(2024, 6, 30, 12, 0))
            .build();
    }

    @Test
    void toDTO_deveMapearTodosOsCampos() {
        var e = entity(1L, "Ana");

        var dto = mapper.toDTO(e);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.apartamentoId()).isEqualTo(10L);
        assertThat(dto.pessoaId()).isEqualTo(1L);
        assertThat(dto.nomeMorador()).isEqualTo("Ana");
        assertThat(dto.emailMorador()).isEqualTo("ana@test.com");
        assertThat(dto.cpfMorador()).isEqualTo("123.456.789-09");
        assertThat(dto.dataEntrada()).isEqualTo(LocalDate.of(2023, 1, 1));
        assertThat(dto.dataSaida()).isEqualTo(LocalDate.of(2024, 6, 30));
        assertThat(dto.criadoEm()).isEqualTo(LocalDateTime.of(2024, 6, 30, 12, 0));
    }

    @Test
    void toDTOList_deveMapearListaCompleta() {
        var lista = List.of(entity(1L, "Ana"), entity(2L, "Bruno"), entity(3L, "Carla"));

        var result = mapper.toDTOList(lista);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).nomeMorador()).isEqualTo("Ana");
        assertThat(result.get(1).nomeMorador()).isEqualTo("Bruno");
        assertThat(result.get(2).nomeMorador()).isEqualTo("Carla");
    }

    @Test
    void toDTOList_deveRetornarListaVazia_paraListaVazia() {
        var result = mapper.toDTOList(List.of());

        assertThat(result).isNotNull().isEmpty();
    }
}
