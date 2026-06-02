package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for Grupos de Despesa and Rateio BDD scenarios.
 */
@RequiredArgsConstructor
public class RateioSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    @When("eu crio um grupo de despesa com nome {string} e tipoRateio {string}")
    public void criarGrupoDespesa(String nome, String tipoRateio) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", nome);
        body.put("tipoRateio", tipoRateio);
        body.put("escopo", "TODOS");
        http.post("/grupos-despesa", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um grupo de despesa sem nome")
    public void tentarCriarGrupoDespesaSemNome() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "");
        body.put("tipoRateio", "IGUALITARIO");
        body.put("escopo", "TODOS");
        http.post("/grupos-despesa", objectMapper.writeValueAsString(body));
    }

    @When("eu envio recalcular rateio com confirmar false")
    public void enviarRecalcularSemConfirmar() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("confirmar", false);
        http.post("/rateio/recalcular", objectMapper.writeValueAsString(body));
    }
}
