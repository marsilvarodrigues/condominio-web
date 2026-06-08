package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Cucumber step definitions for the morador domain (Pessoa, Proprietario).
 */
@RequiredArgsConstructor
public class MoradorSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    // ── Pessoa (Morador) ──────────────────────────────────────────────────────

    /**
     * Creates a Morador test fixture and stores its id in the scenario context.
     */
    @Given("eu criei uma pessoa de teste")
    public void criarPessoaDeTeste() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Pessoa Teste BDD");
        body.put("email", "pessoa.bdd@test.com");
        body.put("cpf", "123.456.789-09");
        http.post("/pessoas", objectMapper.writeValueAsString(body));
        ctx.setTestPessoaId(ctx.getLastCreatedId());
    }

    /**
     * Creates a second Morador test fixture and stores its id in the scenario context.
     */
    @Given("eu criei uma segunda pessoa de teste")
    public void criarSegundaPessoaDeTeste() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Segunda Pessoa BDD");
        body.put("email", "segunda.bdd@test.com");
        body.put("cpf", "987.654.321-00");
        http.post("/pessoas", objectMapper.writeValueAsString(body));
        ctx.setSecondTestPessoaId(ctx.getLastCreatedId());
    }

    @When("eu crio uma pessoa física com nome {string} e cpf {string}")
    public void criarPessoaFisica(String nome, String cpf) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", nome);
        body.put("email", nome.toLowerCase().replace(" ", ".") + "@bdd.com");
        body.put("cpf", cpf);
        http.post("/pessoas", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar uma pessoa física sem CPF")
    public void criarPessoaFisicaSemCpf() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Teste Sem CPF");
        body.put("email", "sem.cpf@bdd.com");
        // cpf ausente intencionalmente
        http.post("/pessoas", objectMapper.writeValueAsString(body));
    }

    // ── Proprietario ──────────────────────────────────────────────────────────

    /**
     * Creates a ProprietarioPessoaFisica test fixture and stores its id in the scenario context.
     */
    @Given("eu criei um proprietário de teste")
    public void criarProprietarioDeTeste() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Proprietario Teste BDD");
        body.put("email", "prop.bdd@test.com");
        body.put("tipo", "PROP_PF");
        body.put("cpf", "935.411.347-80");
        http.post("/proprietarios", objectMapper.writeValueAsString(body));
        ctx.setTestProprietarioId(ctx.getLastCreatedId());
    }

    @When("eu crio um proprietário pessoa física com nome {string} e cpf {string}")
    public void criarProprietarioPessoaFisica(String nome, String cpf) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", nome);
        body.put("email", nome.toLowerCase().replace(" ", ".") + "@bdd.com");
        body.put("tipo", "PROP_PF");
        body.put("cpf", cpf);
        http.post("/proprietarios", objectMapper.writeValueAsString(body));
    }

    @When("eu associo o proprietário de teste ao apartamento de teste")
    public void associarProprietarioAoApartamento() {
        http.post("/proprietarios/" + ctx.getTestProprietarioId()
                + "/apartamentos?apartamentoId=" + ctx.getTestApartamentoId(), "{}");
    }

    @When("eu faço GET dos proprietários do apartamento de teste")
    public void getProprietariosDoApartamento() {
        http.get("/apartamentos/" + ctx.getTestApartamentoId() + "/proprietarios");
    }

    @When("eu registro a pessoa de teste como proprietária do apartamento de teste")
    public void registrarProprietario() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Proprietario Via Step");
        body.put("email", "prop.via.step@test.com");
        body.put("tipo", "PROP_PF");
        body.put("cpf", "111.444.777-35");
        http.post("/proprietarios", objectMapper.writeValueAsString(body));
        Long propId = ctx.getLastCreatedId();
        ctx.setTestProprietarioId(propId);
        http.post("/proprietarios/" + propId + "/apartamentos?apartamentoId=" + ctx.getTestApartamentoId(), "{}");
    }

    @When("eu faço GET para o proprietário do apartamento de teste")
    public void getProprietarioDoApartamento() {
        http.get("/apartamentos/" + ctx.getTestApartamentoId() + "/proprietarios");
    }

    @When("eu faço GET para o histórico de proprietários do apartamento de teste")
    public void getHistoricoProprietarios() {
        http.get("/apartamentos/" + ctx.getTestApartamentoId() + "/proprietarios");
    }

    // ── Morador assignment ────────────────────────────────────────────────────

    @And("eu atribuo a pessoa de teste ao apartamento de teste como morador")
    public void atribuirPessoaAoApartamento() {
        http.post("/pessoas/" + ctx.getTestPessoaId()
                + "/apartamento?apartamentoId=" + ctx.getTestApartamentoId(), "{}");
    }
}
