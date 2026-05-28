package com.pmrodrigues.condominio.config;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

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
}
