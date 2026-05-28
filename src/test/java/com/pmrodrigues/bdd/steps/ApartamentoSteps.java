package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class ApartamentoSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    @When("eu crio um apartamento com numero {string} no bloco de teste")
    public void criarApartamento(String numero) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("blocoId", ctx.getTestBlocoId());
        body.put("numero", numero);
        http.post("/apartamentos", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um apartamento sem numero")
    public void tentarCriarApartamentoSemNumero() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("blocoId", ctx.getTestBlocoId());
        // numero ausente intencionalmente
        http.post("/apartamentos", objectMapper.writeValueAsString(body));
    }

    @When("eu atualizo o último apartamento criado com numero {string}")
    public void atualizarApartamento(String novoNumero) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", ctx.getLastCreatedId());
        body.put("numero", novoNumero);
        body.put("blocoId", ctx.getTestBlocoId());
        http.put("/apartamentos/" + ctx.getLastCreatedId(), objectMapper.writeValueAsString(body));
    }
}
