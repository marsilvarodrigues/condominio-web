package com.pmrodrigues.commons.service;

import com.pmrodrigues.commons.dto.BancoDTO;
import com.pmrodrigues.commons.dto.BancoFilterDTO;
import com.pmrodrigues.commons.dto.CreateBancoDTO;
import com.pmrodrigues.commons.dto.UpdateBancoDTO;
import com.pmrodrigues.commons.mapper.BancoMapper;
import com.pmrodrigues.commons.model.Banco;
import com.pmrodrigues.commons.repository.BancoRepository;
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
@ContextConfiguration(classes = BancoServiceCacheTest.TestConfig.class)
class BancoServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("bancos");
        }

        @Bean
        BancoRepository repository() {
            return mock(BancoRepository.class);
        }

        @Bean
        BancoMapper mapper() {
            return mock(BancoMapper.class);
        }

        @Bean
        BancoService service(BancoRepository repository, BancoMapper mapper) {
            return new BancoService(repository, mapper);
        }
    }

    @Autowired BancoService service;
    @Autowired BancoRepository repository;
    @Autowired BancoMapper mapper;
    @Autowired CacheManager cacheManager;

    private final Banco entity = Banco.builder().id(1L).codigo("341").nome("Itaú").ispb("60701190").build();
    private final BancoDTO dto = new BancoDTO(1L, "341", "Itaú", "60701190");
    private final BancoFilterDTO allFilter = new BancoFilterDTO(null, null);

    @BeforeEach
    void setUp() {
        reset(repository, mapper);
        cacheManager.getCache("bancos").clear();
    }

    // ── filterBy ──────────────────────────────────────────────────────────

    @Test
    void filterBy_cachedOnSecondCall_repositoryCalledOnce() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        service.filterBy(allFilter);
        service.filterBy(allFilter);

        verify(repository, times(1)).findAll(any(Specification.class));
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

    // ── cache eviction ────────────────────────────────────────────────────

    @Test
    void create_evictsCache_repositoryCalledAgainAfterCreate() {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);
        service.filterBy(allFilter);

        var createDTO = new CreateBancoDTO("341", "Itaú", "60701190");
        when(mapper.toEntity(createDTO)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        service.create(createDTO);

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
        service.update(1L, new UpdateBancoDTO("Itaú Unibanco", "60701190"));

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
