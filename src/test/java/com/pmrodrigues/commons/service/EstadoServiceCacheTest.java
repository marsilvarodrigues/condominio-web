package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.dto.EstadoFilterDTO;
import com.pmrodrigues.commons.mapper.EstadoMapper;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EstadoServiceCacheTest.TestConfig.class)
class EstadoServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("estados");
        }

        @Bean
        EstadoRepository repository() {
            return mock(EstadoRepository.class);
        }

        @Bean
        EstadoMapper mapper() {
            return mock(EstadoMapper.class);
        }

        @Bean
        EstadoService service(EstadoRepository repository, EstadoMapper mapper) {
            return new EstadoService(repository, mapper);
        }
    }

    @Autowired EstadoService service;
    @Autowired EstadoRepository repository;
    @Autowired EstadoMapper mapper;
    @Autowired CacheManager cacheManager;

    private final Estado entity = new Estado(1L, "São Paulo", "SP");
    private final EstadoDTO dto = new EstadoDTO(1L, "São Paulo", "SP");
    private final EstadoFilterDTO allFilter = new EstadoFilterDTO(null, null);

    @BeforeEach
    void setUp() {
        reset(repository, mapper);
        cacheManager.getCache("estados").clear();
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

    // ── filterBy ─────────────────────────────────────────────────────────

    @Test
    void filterBy_cachedOnSecondCall_repositoryCalledOnce() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        service.filterBy(allFilter);
        service.filterBy(allFilter);

        verify(repository, times(1)).findAll(any(Specification.class));
    }

    // ── cache eviction ────────────────────────────────────────────────────

    @Test
    void create_evictsCache_repositoryCalledAgainAfterCreate() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);
        service.filterBy(allFilter);

        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        service.create(dto);

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
