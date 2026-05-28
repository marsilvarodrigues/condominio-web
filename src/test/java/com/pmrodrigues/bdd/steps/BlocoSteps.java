package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class BlocoSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    @When("eu busco o bloco de teste por ID")
    public void buscarBlocoPorId() throws Exception {
        http.get("/blocos/" + ctx.getTestBlocoId());
    }

    @When("eu crio um bloco com numero {int} e letra {string}")
    public void criarBloco(int numero, String letra) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("numero", numero);
        body.put("bloco", letra);
        http.post("/blocos", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um bloco sem numero")
    public void tentarCriarBlocoSemNumero() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("bloco", "SemNumero");
        // numero ausente intencionalmente
        http.post("/blocos", objectMapper.writeValueAsString(body));
    }

    @When("eu atualizo o bloco de teste com letra {string}")
    public void atualizarBloco(String novaLetra) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", ctx.getTestBlocoId());
        body.put("numero", 1);
        body.put("bloco", novaLetra);
        http.put("/blocos/" + ctx.getTestBlocoId(), objectMapper.writeValueAsString(body));
    }
}
