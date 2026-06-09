package com.pmrodrigues.morador.service;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
import com.pmrodrigues.morador.repository.ProprietarioRepository;
import com.pmrodrigues.security.dto.ProprietarioClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProprietarioClaimsProviderImplTest {

    @Mock ProprietarioRepository proprietarioRepository;

    ProprietarioClaimsProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new ProprietarioClaimsProviderImpl(proprietarioRepository);
    }

    @Test
    void findClaimsByEmail_whenProprietarioExists_returnsClaims() {
        var apt1 = new Apartamento(); apt1.setId(10L);
        var apt2 = new Apartamento(); apt2.setId(20L);
        var proprietario = new ProprietarioPessoaFisica();
        proprietario.setId(42L);
        proprietario.setApartamentos(Set.of(apt1, apt2));

        when(proprietarioRepository.findByUserEmail("owner@test.com"))
                .thenReturn(Optional.of(proprietario));

        Optional<ProprietarioClaims> result = provider.findClaimsByEmail("owner@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(42L);
        assertThat(result.get().apartamentoIds()).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void findClaimsByEmail_whenNotProprietario_returnsEmpty() {
        when(proprietarioRepository.findByUserEmail("user@test.com"))
                .thenReturn(Optional.empty());

        Optional<ProprietarioClaims> result = provider.findClaimsByEmail("user@test.com");

        assertThat(result).isEmpty();
    }
}
