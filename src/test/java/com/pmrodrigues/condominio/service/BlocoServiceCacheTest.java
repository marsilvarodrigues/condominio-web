package com.pmrodrigues.condominio.service;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.BlocoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.mapper.BlocoMapper;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.repository.BlocoRepository;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BlocoServiceCacheTest.TestConfig.class)
class BlocoServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("blocos");
        }

        @Bean
        BlocoRepository repository() {
            return mock(BlocoRepository.class);
        }

        @Bean
        BlocoMapper mapper() {
            return mock(BlocoMapper.class);
        }

        @Bean
        BlocoService service(BlocoRepository repository, BlocoMapper mapper) {
            return new BlocoService(repository, mapper);
        }
    }

    @Autowired BlocoService service;
    @Autowired BlocoRepository repository;
    @Autowired BlocoMapper mapper;
    @Autowired CacheManager cacheManager;

    private final Bloco entity = Bloco.builder().id(1L).numero(1).bloco("A").build();
    private final BlocoDTO dto = new BlocoDTO(1L, 1, "A", null, null);
    private final BlocoFilterDTO allFilter = new BlocoFilterDTO(null);

    @BeforeEach
    void setUp() {
        reset(repository, mapper);
        cacheManager.getCache("blocos").clear();
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

        var createDto = new CreateBlocoDTO(2, "B");
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
