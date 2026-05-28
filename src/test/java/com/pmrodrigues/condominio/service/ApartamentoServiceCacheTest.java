package com.pmrodrigues.condominio.service;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.ApartamentoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.mapper.ApartamentoMapper;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApartamentoServiceCacheTest.TestConfig.class)
class ApartamentoServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("apartamentos");
        }

        @Bean
        ApartamentoRepository repository() {
            return mock(ApartamentoRepository.class);
        }

        @Bean
        ApartamentoMapper mapper() {
            return mock(ApartamentoMapper.class);
        }

        @Bean
        ApartamentoService service(ApartamentoRepository repository, ApartamentoMapper mapper) {
            return new ApartamentoService(repository, mapper);
        }
    }

    @Autowired ApartamentoService service;
    @Autowired ApartamentoRepository repository;
    @Autowired ApartamentoMapper mapper;
    @Autowired CacheManager cacheManager;

    private final Apartamento entity = Apartamento.builder().id(1L).numero("101").build();
    private final ApartamentoDTO dto = new ApartamentoDTO(1L, null, "101", null, null, BigDecimal.TEN);
    private final ApartamentoFilterDTO allFilter = new ApartamentoFilterDTO(null, null);

    @BeforeEach
    void setUp() {
        reset(repository, mapper);
        cacheManager.getCache("apartamentos").clear();
        TenantContext.setCondominioId(10L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
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
    void findById_emptyResult_notCached() {
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

        var createDto = new CreateApartamentoDTO(1L, "102", BigDecimal.TEN);
        when(mapper.toEntity(createDto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        service.create(createDto);

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
