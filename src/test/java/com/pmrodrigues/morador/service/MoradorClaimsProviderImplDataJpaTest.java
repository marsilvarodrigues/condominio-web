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
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.repository.PessoaRepository;
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
 * Exercises {@link MoradorClaimsProviderImpl} against a real Hibernate session (not a mock), with
 * no ambient transaction — the same condition it runs under in production, called from {@code
 * JwtService.generateAccessToken} during login. See {@code ProprietarioClaimsProviderImplDataJpaTest}
 * for why {@code @DataJpaTest}'s default per-test transaction has to be disabled here.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({JpaAuditingConfig.class, MoradorClaimsProviderImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MoradorClaimsProviderImplDataJpaTest {

    @Autowired MoradorClaimsProviderImpl provider;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired PessoaRepository pessoaRepository;
    @Autowired EstadoRepository estadoRepository;

    @BeforeEach
    void setUp() {
        var estado = estadoRepository.save(new Estado(null, "São Paulo", "SP"));
        var condominio = condominioRepository.save(Condominio.builder()
                .nome("Cond Morador Claims")
                .cnpj("44.555.666/0001-77")
                .email("moradorclaims@test.com")
                .endereco(new Endereco("Rua G, 7", "01001000", "São Paulo", estado))
                .build());

        TenantContext.setCondominioId(condominio.getId());

        var bloco = blocoRepository.save(Bloco.builder()
                .condominio(condominio).numero(1).bloco("A").build());
        var apt = apartamentoRepository.save(Apartamento.builder()
                .condominio(condominio).bloco(bloco).numero("101").areaConstruida(BigDecimal.TEN).build());

        var morador = new Morador();
        morador.setEmail("morador-claims@test.com");
        morador.setName("Morador Claims");
        morador.setPassword("hashed-password");
        morador.setApartamento(apt);
        pessoaRepository.save(morador);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findApartamentoIdByEmail_withRealLazyAssociation_doesNotThrowLazyInitializationException() {
        var result = provider.findApartamentoIdByEmail("morador-claims@test.com");

        assertThat(result).isPresent();
    }
}
