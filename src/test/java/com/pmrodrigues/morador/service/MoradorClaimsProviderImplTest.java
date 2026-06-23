package com.pmrodrigues.morador.service;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.repository.PessoaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoradorClaimsProviderImplTest {

    @Mock PessoaRepository pessoaRepository;

    MoradorClaimsProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new MoradorClaimsProviderImpl(pessoaRepository);
    }

    @Test
    void findApartamentoIdByEmail_whenMoradorHasApartamento_returnsId() {
        var apt = new Apartamento();
        apt.setId(17L);
        var morador = new Morador();
        morador.setApartamento(apt);

        when(pessoaRepository.findByEmail("carlos@test.com")).thenReturn(Optional.of(morador));

        var result = provider.findApartamentoIdByEmail("carlos@test.com");

        assertThat(result).contains(17L);
    }

    @Test
    void findApartamentoIdByEmail_whenMoradorHasNoApartamento_returnsEmpty() {
        var morador = new Morador();

        when(pessoaRepository.findByEmail("sememapt@test.com")).thenReturn(Optional.of(morador));

        var result = provider.findApartamentoIdByEmail("sememapt@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findApartamentoIdByEmail_whenNotPessoa_returnsEmpty() {
        when(pessoaRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        var result = provider.findApartamentoIdByEmail("admin@test.com");

        assertThat(result).isEmpty();
    }
}
