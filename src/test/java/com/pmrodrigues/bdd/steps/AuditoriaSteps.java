package com.pmrodrigues.bdd.steps;

import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

/**
 * Step definitions for the Auditoria (Hibernate Envers) BDD feature.
 */
@RequiredArgsConstructor
public class AuditoriaSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;

    /**
     * Queries the audit trail for the test apartment created in {@code @Before}.
     *
     * @param entidade path segment (e.g. "apartamentos") to pass to /auditoria/{entidade}/{id}
     */
    @When("eu consulto o histórico de auditoria de {string} do apartamento de teste")
    public void consultarHistoricoAuditoria(String entidade) {
        http.get("/auditoria/" + entidade + "/" + ctx.getTestApartamentoId());
    }
}
