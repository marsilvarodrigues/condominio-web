package com.pmrodrigues.condominio.service;

import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CondominioFilterDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioEnderecoDTO;
import com.pmrodrigues.condominio.mapper.CondominioMapper;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import com.pmrodrigues.condominio.repository.BlocoRepository;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.financeiro.service.FundoReservaService;
import com.pmrodrigues.financeiro.service.OrcamentoAnualService;
import com.pmrodrigues.financeiro.service.PlanoContasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CondominioServiceCacheTest.TestConfig.class)
class CondominioServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("condominios", "blocos", "apartamentos",
                    "plano-contas", "fundo-reserva", "orcamentos");
        }

        @Bean
        CondominioRepository repository() {
            return mock(CondominioRepository.class);
        }

        @Bean
        CondominioMapper mapper() {
            return mock(CondominioMapper.class);
        }

        @Bean
        BlocoRepository blocoRepository() {
            return mock(BlocoRepository.class);
        }

        @Bean
        ApartamentoRepository apartamentoRepository() {
            return mock(ApartamentoRepository.class);
        }

        @Bean
        PlanoContasService planoContasService() {
            return mock(PlanoContasService.class);
        }

        @Bean
        FundoReservaService fundoReservaService() {
            return mock(FundoReservaService.class);
        }

        @Bean
        OrcamentoAnualService orcamentoAnualService() {
            return mock(OrcamentoAnualService.class);
        }

        @Bean
        CondominioService service(CondominioRepository repository, CondominioMapper mapper,
                                   BlocoRepository blocoRepository, ApartamentoRepository apartamentoRepository,
                                   PlanoContasService planoContasService, FundoReservaService fundoReservaService,
                                   OrcamentoAnualService orcamentoAnualService) {
            return new CondominioService(repository, mapper, blocoRepository, apartamentoRepository,
                    planoContasService, fundoReservaService, orcamentoAnualService);
        }
    }

    @Autowired CondominioService service;
    @Autowired CondominioRepository repository;
    @Autowired CondominioMapper mapper;
    @Autowired CacheManager cacheManager;

    private final Condominio entity = Condominio.builder().id(1L).nome("Test").cnpj("12.345.678/0001-99").build();
    private final CondominioDTO dto = new CondominioDTO(1L, "Test", "12.345.678/0001-99", "t@t.com", null, null, null);
    private final CondominioFilterDTO allFilter = new CondominioFilterDTO(null, null);

    @BeforeEach
    void setUp() {
        reset(repository, mapper);
        cacheManager.getCache("condominios").clear();
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
    void findById_emptyResult_notCached_repositoryCalledEachTime() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        service.findById(99L);
        service.findById(99L);

        verify(repository, times(2)).findById(99L);
    }

    // ── cache eviction ────────────────────────────────────────────────────

    @Test
    void create_evictsCache_repositoryCalledAgainAfterCreate() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);
        service.filterBy(allFilter);

        var createDto = new CreateCondominioDTO("Test", "12.345.678/0001-95", "t@t.com",
                new CreateCondominioEnderecoDTO("Rua A", "01001000", "SP", 1L));
        when(mapper.toEntity(createDto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        service.create(createDto);

        service.filterBy(allFilter);
        verify(repository, times(2)).findAll(any(Specification.class));
    }

    @Test
    void update_evictsCache_repositoryCalledAgainAfterUpdate() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);
        service.filterBy(allFilter);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        service.update(dto);

        service.filterBy(allFilter);
        verify(repository, times(2)).findAll(any(Specification.class));
    }

    @Test
    void delete_evictsCache_repositoryCalledAgainAfterDelete() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);
        service.filterBy(allFilter);

        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        service.delete(1L);

        service.filterBy(allFilter);
        verify(repository, times(2)).findAll(any(Specification.class));
    }
}
