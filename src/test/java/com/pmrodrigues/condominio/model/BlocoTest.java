package com.pmrodrigues.condominio.model;

import com.pmrodrigues.commons.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class BlocoTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void prePersist_whenTenantSet_wiresCondominioFromContext() throws Exception {
        TenantContext.setCondominioId(42L);
        var bloco = new Bloco();

        bloco.prePersist();

        Field field = Bloco.class.getDeclaredField("condominio");
        field.setAccessible(true);
        Condominio condominio = (Condominio) field.get(bloco);
        assertThat(condominio).isNotNull();
        assertThat(condominio.getId()).isEqualTo(42L);
    }

    @Test
    void prePersist_whenTenantNotSet_leavesCondominioNull() throws Exception {
        var bloco = new Bloco();

        bloco.prePersist();

        Field field = Bloco.class.getDeclaredField("condominio");
        field.setAccessible(true);
        assertThat(field.get(bloco)).isNull();
    }
}
