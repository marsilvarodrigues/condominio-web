package com.pmrodrigues.cobranca.mapper;

import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.cobranca.model.StatusCobranca;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CobrancaMapperImpl.class)
class CobrancaMapperTest {

    @Autowired CobrancaMapper mapper;

    // ── toDTO ─────────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsApartamentoFields() {
        var cobranca = cobranca();

        var dto = mapper.toDTO(cobranca);

        assertThat(dto.apartamentoId()).isEqualTo(10L);
        assertThat(dto.apartamentoNumero()).isEqualTo("101");
        assertThat(dto.blocoNome()).isEqualTo("A");
    }

    @Test
    void toDTO_moradorFieldsAreNull() {
        var dto = mapper.toDTO(cobranca());

        assertThat(dto.moradorNome()).isNull();
        assertThat(dto.moradorEmail()).isNull();
    }

    @Test
    void toDTO_mapsValorStatusVencimento() {
        var dto = mapper.toDTO(cobranca());

        assertThat(dto.valor()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(dto.status()).isEqualTo(StatusCobranca.PENDENTE);
        assertThat(dto.vencimento()).isEqualTo(LocalDate.of(2026, 7, 10));
    }

    @Test
    void toDTO_mapsCriadaEmFromCreatedAt() {
        var cobranca = cobranca();
        var now = LocalDateTime.now();
        cobranca.setCreatedAt(now);

        var dto = mapper.toDTO(cobranca);

        assertThat(dto.criadaEm()).isEqualTo(now);
    }

    // ── toResumoDTO ───────────────────────────────────────────────────────────

    @Test
    void toResumoDTO_mapsCriadaEmFromCreatedAt() {
        var cobranca = cobranca();
        var now = LocalDateTime.now();
        cobranca.setCreatedAt(now);

        var dto = mapper.toResumoDTO(cobranca);

        assertThat(dto.criadaEm()).isEqualTo(now);
        assertThat(dto.valor()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(dto.status()).isEqualTo(StatusCobranca.PENDENTE);
    }

    private Cobranca cobranca() {
        var bloco = new Bloco();
        bloco.setBloco("A");

        var apt = new Apartamento();
        apt.setId(10L);
        apt.setNumero("101");
        apt.setBloco(bloco);

        return Cobranca.builder()
                .apartamento(apt)
                .valor(new BigDecimal("750.00"))
                .vencimento(LocalDate.of(2026, 7, 10))
                .status(StatusCobranca.PENDENTE)
                .build();
    }
}
