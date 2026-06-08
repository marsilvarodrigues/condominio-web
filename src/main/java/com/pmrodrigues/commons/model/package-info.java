@FilterDef(
    name = TenantFilterAspect.CONDOMINIO_FILTER,
    parameters = @ParamDef(name = "condominioId", type = Long.class))
package com.pmrodrigues.commons.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
