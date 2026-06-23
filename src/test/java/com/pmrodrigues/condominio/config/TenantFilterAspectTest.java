package com.pmrodrigues.condominio.config;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private TenantFilterAspect aspect;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void enableCondominioFilter_whenTenantNotSet_doesNotInteractWithEntityManager() {
        aspect.enableCondominioFilter();

        verifyNoInteractions(entityManager);
    }

    @Test
    void enableCondominioFilter_whenTenantSet_enablesFilterWithCondominioId() {
        TenantContext.setCondominioId(42L);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("condominioFilter")).thenReturn(filter);
        when(filter.setParameter("condominioId", 42L)).thenReturn(filter);

        aspect.enableCondominioFilter();

        verify(session).enableFilter("condominioFilter");
        verify(filter).setParameter("condominioId", 42L);
    }

    // ── enforceTenantOnFindById ─────────────────────────────────────────────

    private Apartamento apartamentoOf(Long condominioId) {
        var condominio = new Condominio();
        condominio.setId(condominioId);
        return Apartamento.builder().condominio(condominio).build();
    }

    @Test
    void enforceTenantOnFindById_whenNoActiveTenant_returnsResultUnchanged() throws Throwable {
        var result = Optional.of(apartamentoOf(2L));
        when(proceedingJoinPoint.proceed()).thenReturn(result);

        var returned = aspect.enforceTenantOnFindById(proceedingJoinPoint);

        assertThat(returned).isSameAs(result);
    }

    @Test
    void enforceTenantOnFindById_whenEntityMatchesActiveTenant_returnsResultUnchanged() throws Throwable {
        TenantContext.setCondominioId(1L);
        var result = Optional.of(apartamentoOf(1L));
        when(proceedingJoinPoint.proceed()).thenReturn(result);

        var returned = aspect.enforceTenantOnFindById(proceedingJoinPoint);

        assertThat(returned).isSameAs(result);
    }

    @Test
    void enforceTenantOnFindById_whenEntityFromOtherTenant_returnsEmpty() throws Throwable {
        TenantContext.setCondominioId(1L);
        when(proceedingJoinPoint.proceed()).thenReturn(Optional.of(apartamentoOf(2L)));

        var returned = aspect.enforceTenantOnFindById(proceedingJoinPoint);

        assertThat(returned).isEqualTo(Optional.empty());
    }

    @Test
    void enforceTenantOnFindById_whenResultIsEmpty_returnsEmpty() throws Throwable {
        TenantContext.setCondominioId(1L);
        when(proceedingJoinPoint.proceed()).thenReturn(Optional.empty());

        var returned = aspect.enforceTenantOnFindById(proceedingJoinPoint);

        assertThat(returned).isEqualTo(Optional.empty());
    }

    @Test
    void enforceTenantOnFindById_whenResultIsNotOptional_returnsResultUnchanged() throws Throwable {
        TenantContext.setCondominioId(1L);
        var result = apartamentoOf(2L);
        when(proceedingJoinPoint.proceed()).thenReturn(result);

        var returned = aspect.enforceTenantOnFindById(proceedingJoinPoint);

        assertThat(returned).isSameAs(result);
    }
}
