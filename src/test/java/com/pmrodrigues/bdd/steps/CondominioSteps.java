package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class CondominioSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    @When("eu busco o condomínio de teste por ID")
    public void buscarCondominioPorId() throws Exception {
        http.get("/condominios/" + ctx.getTestCondominioId());
    }

    @When("eu crio um condomínio com nome {string} e cnpj {string}")
    public void criarCondominio(String nome, String cnpj) throws Exception {
        http.post("/condominios", objectMapper.writeValueAsString(buildBody(nome, cnpj)));
    }

    @When("eu tento criar um condomínio sem nome")
    public void tentarCriarCondominioSemNome() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("cnpj", "12.345.678/0001-95");
        body.put("email", "valido@test.com");
        // nome ausente intencionalmente
        http.post("/condominios", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um condomínio com cnpj inválido")
    public void tentarCriarCondominioComCnpjInvalido() throws Exception {
        Map<String, Object> body = buildBody("Cond Invalido", "11.111.111/0001-11");
        http.post("/condominios", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um condomínio com cep inválido")
    public void tentarCriarCondominioComCepInvalido() throws Exception {
        Map<String, Object> body = buildBody("Cond Cep Invalido", "12.345.678/0001-95");
        // sobrescreve o endereco com cep inválido (menos de 8 chars)
        Map<String, Object> enderecoInvalido = new HashMap<>();
        enderecoInvalido.put("logradouro", "Av. BDD, 100");
        enderecoInvalido.put("cep", "0100");
        enderecoInvalido.put("cidade", "São Paulo");
        enderecoInvalido.put("estado", ctx.getTestEstadoId());
        body.put("endereco", enderecoInvalido);
        http.post("/condominios", objectMapper.writeValueAsString(body));
    }

    @When("eu atualizo o condomínio de teste com nome {string}")
    public void atualizarCondominio(String novoNome) throws Exception {
        // PUT /condominios uses CondominioDTO whose endereco.estado is EstadoDTO (object, not Long).
        Map<String, Object> estadoObj = new HashMap<>();
        estadoObj.put("id", ctx.getTestEstadoId());
        Map<String, Object> endereco = new HashMap<>();
        endereco.put("logradouro", "Av. BDD, 100");
        endereco.put("cep", "01001000");
        endereco.put("cidade", "São Paulo");
        endereco.put("estado", estadoObj);
        Map<String, Object> body = new HashMap<>();
        body.put("id", ctx.getTestCondominioId());
        body.put("nome", novoNome);
        body.put("cnpj", "00.000.000/0001-00");
        body.put("email", "bdd@condominio.com");
        body.put("endereco", endereco);
        http.put("/condominios/" + ctx.getTestCondominioId(), objectMapper.writeValueAsString(body));
    }

    private Map<String, Object> buildBody(String nome, String cnpj) {
        Map<String, Object> endereco = new HashMap<>();
        endereco.put("logradouro", "Av. BDD, 100");
        endereco.put("cep", "01001000");
        endereco.put("cidade", "São Paulo");
        endereco.put("estado", ctx.getTestEstadoId());

        Map<String, Object> body = new HashMap<>();
        body.put("nome", nome);
        body.put("cnpj", cnpj);
        body.put("email", "bdd@condominio.com");
        body.put("endereco", endereco);
        return body;
    }
}
