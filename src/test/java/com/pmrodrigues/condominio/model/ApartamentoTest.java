package com.pmrodrigues.condominio.model;

import com.pmrodrigues.commons.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class ApartamentoTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void prePersist_whenTenantSet_wiresCondominioFromContext() throws Exception {
        TenantContext.setCondominioId(99L);
        var apartamento = new Apartamento();

        apartamento.prePersist();

        Field field = Apartamento.class.getDeclaredField("condominio");
        field.setAccessible(true);
        Condominio condominio = (Condominio) field.get(apartamento);
        assertThat(condominio).isNotNull();
        assertThat(condominio.getId()).isEqualTo(99L);
    }

    @Test
    void prePersist_whenTenantNotSet_leavesCondominioNull() throws Exception {
        var apartamento = new Apartamento();

        apartamento.prePersist();

        Field field = Apartamento.class.getDeclaredField("condominio");
        field.setAccessible(true);
        assertThat(field.get(apartamento)).isNull();
    }
}
