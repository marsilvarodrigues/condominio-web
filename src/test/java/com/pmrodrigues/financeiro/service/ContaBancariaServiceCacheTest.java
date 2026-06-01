package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.commons.dto.BancoDTO;
import com.pmrodrigues.commons.service.BancoService;
import com.pmrodrigues.financeiro.dto.ContaBancariaDTO;
import com.pmrodrigues.financeiro.dto.CreateContaBancariaDTO;
import com.pmrodrigues.financeiro.mapper.ContaBancariaMapper;
import com.pmrodrigues.financeiro.model.ContaBancaria;
import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import com.pmrodrigues.financeiro.repository.ContaBancariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ContaBancariaServiceCacheTest.TestConfig.class)
class ContaBancariaServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("contas-bancarias", "bancos");
        }

        @Bean
        ContaBancariaRepository repository() {
            return mock(ContaBancariaRepository.class);
        }

        @Bean
        BancoService bancoService() {
            return mock(BancoService.class);
        }

        @Bean
        ContaBancariaMapper mapper() {
            return mock(ContaBancariaMapper.class);
        }

        @Bean
        ContaBancariaService service(ContaBancariaRepository repository, BancoService bancoService,
                                     ContaBancariaMapper mapper) {
            return new ContaBancariaService(repository, bancoService, mapper);
        }
    }

    @Autowired ContaBancariaService service;
    @Autowired ContaBancariaRepository repository;
    @Autowired BancoService bancoService;
    @Autowired ContaBancariaMapper mapper;
    @Autowired CacheManager cacheManager;

    private final ContaBancaria entity = ContaBancaria.builder()
            .id(1L).tipo(TipoContaBancaria.CORRENTE).agencia("0001").conta("12345").build();
    private final ContaBancariaDTO dto = new ContaBancariaDTO(1L, 1L, "Itaú", "341",
            TipoContaBancaria.CORRENTE, "0001", "12345", null, null, null,
            BigDecimal.ZERO, true, LocalDateTime.now(), LocalDateTime.now());

    @BeforeEach
    void setUp() {
        reset(repository, bancoService, mapper);
        cacheManager.getCache("contas-bancarias").clear();
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_cachedOnSecondCall_repositoryCalledOnce() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        service.findById(1L);
        service.findById(1L);

        verify(repository, times(1)).findById(1L);
    }

    @Test
    void findById_throwsNotFound_notCached_repositoryCalledEachTime() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResponseStatusException.class);

        verify(repository, times(2)).findById(99L);
    }

    // ── cache eviction ────────────────────────────────────────────────────

    @Test
    void create_evictsCache_repositoryCalledAgainAfterCreate() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);
        service.findById(1L);

        var createDTO = new CreateContaBancariaDTO(1L, TipoContaBancaria.CORRENTE, "0001", "12345", null, null, null);
        var bancoDto = new BancoDTO(1L, "341", "Itaú", "60701190");
        when(bancoService.findById(1L)).thenReturn(bancoDto);
        when(mapper.toEntity(createDTO)).thenReturn(entity);
        when(repository.save(entity)).thenAnswer(inv -> {
            ContaBancaria c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        service.create(createDTO);

        service.findById(1L);
        verify(repository, times(2)).findById(1L);
    }
}
