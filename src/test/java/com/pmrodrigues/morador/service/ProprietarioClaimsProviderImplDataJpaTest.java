package com.pmrodrigues.morador.service;

import com.pmrodrigues.commons.config.JpaAuditingConfig;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import com.pmrodrigues.condominio.repository.BlocoRepository;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link ProprietarioClaimsProviderImpl} against a real Hibernate session (not a mock),
 * so that lazy-collection access on {@code Proprietario.apartamentos} is genuinely triggered. The
 * mock-based {@link ProprietarioClaimsProviderImplTest} sets the collection directly and cannot
 * catch a missing {@code @Transactional} boundary — this test exists specifically to catch that.
 *
 * <p>{@code @DataJpaTest} wraps each test in its own transaction by default, which would keep the
 * Hibernate session open for the whole test and hide the bug just like the mock does.
 * {@code Propagation.NOT_SUPPORTED} opts out of that wrapping so {@code findClaimsByEmail} runs
 * with no ambient transaction — the same condition it runs under in production, called from
 * {@code JwtService.generateAccessToken} during login.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({JpaAuditingConfig.class, ProprietarioClaimsProviderImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProprietarioClaimsProviderImplDataJpaTest {

    @Autowired ProprietarioClaimsProviderImpl provider;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired com.pmrodrigues.morador.repository.ProprietarioRepository proprietarioRepository;
    @Autowired EstadoRepository estadoRepository;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        var condominio = condominioRepository.save(Condominio.builder()
                .nome("Cond Claims")
                .cnpj("33.444.555/0001-66")
                .email("claims@test.com")
                .endereco(new Endereco("Rua F, 6", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        var bloco = blocoRepository.save(Bloco.builder()
                .condominio(condominio).numero(1).bloco("A").build());
        var apt1 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("101").areaConstruida(BigDecimal.TEN).build());
        var apt2 = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("102").areaConstruida(BigDecimal.TEN).build());

        var proprietario = new ProprietarioPessoaFisica();
        proprietario.setEmail("dono@test.com");
        proprietario.setName("Dono Teste");
        proprietario.setPassword("hashed-password");
        proprietario.setCpf("529.982.247-25");
        proprietario.getApartamentos().add(apt1);
        proprietario.getApartamentos().add(apt2);
        proprietarioRepository.save(proprietario);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findClaimsByEmail_withRealLazyCollection_doesNotThrowLazyInitializationException() {
        var result = provider.findClaimsByEmail("dono@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().apartamentoIds()).hasSize(2);
    }
}
