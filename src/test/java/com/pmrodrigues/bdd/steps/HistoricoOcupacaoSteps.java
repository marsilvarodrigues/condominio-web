package com.pmrodrigues.bdd.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import com.pmrodrigues.bdd.hooks.DatabaseSetupHooks;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cucumber step definitions for the occupancy history domain. Reuses helpers from
 * {@link MoradorSteps} and {@link CommonSteps} (DRY) and adds history-specific assertions.
 */
@RequiredArgsConstructor
public class HistoricoOcupacaoSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;
    private final CommonSteps commonSteps;

    // ── Given ─────────────────────────────────────────────────────────────────

    @Given("existe um apartamento de teste com um morador ativo")
    public void existeApartamentoComMorador() throws Exception {
        criarMoradorNoApartamento("morador.ativo@bdd.com", "Morador Ativo BDD", "100.200.300-09");
    }

    @Given("o apartamento de teste já tem um morador ativo")
    public void apartamentoJaTemMorador() {
        // handled by Background step — morador already exists
    }

    @Given("existem registros no histórico do apartamento de teste")
    public void existemRegistrosNoHistorico() throws Exception {
        criarMoradorNoApartamento("hist.bdd@bdd.com", "Hist BDD", "111.222.333-44");
        http.delete("/pessoas/" + ctx.getTestPessoaId() + "/apartamento");
    }

    @Given("o apartamento de teste não tem histórico de ocupação")
    public void apartamentoSemHistorico() {
        // nothing to do — no moradores have been removed from this apartment
    }

    @Given("existe um morador com somente ROLE_MORADOR no apartamento de teste")
    public void moradorSomenteRoleMorador() throws Exception {
        criarMoradorNoApartamento("somente.morador@bdd.com", "Somente Morador", "999.888.777-66");
    }

    @Given("existe um morador que também é proprietário no apartamento de teste")
    public void moradorTambemProprietario() throws Exception {
        criarMoradorNoApartamento("prop.morador@bdd.com", "Prop Morador", "555.444.333-22");
        jdbc.update(
            "INSERT INTO user_roles (user_id, role) VALUES (?, 'ROLE_PROPRIETARIO')",
            ctx.getTestPessoaId());
    }

    // ── When ──────────────────────────────────────────────────────────────────

    @When("eu removo o morador do apartamento de teste")
    public void removerMoradorDoApartamento() {
        http.delete("/pessoas/" + ctx.getTestPessoaId() + "/apartamento");
    }

    @When("eu associo um segundo morador ao apartamento de teste")
    public void associarSegundoMorador() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Segundo Morador BDD");
        body.put("email", "segundo.bdd@bdd.com");
        body.put("cpf", "777.888.999-00");
        http.post("/pessoas", objectMapper.writeValueAsString(body));
        Long segundoId = ctx.getLastCreatedId();
        http.post(
            "/pessoas/" + segundoId + "/apartamento?apartamentoId=" + ctx.getTestApartamentoId(),
            "{}");
    }

    @When("eu faço GET para o histórico de ocupação do apartamento de teste")
    public void getHistoricoOcupacao() {
        http.get("/pessoas/apartamentos/" + ctx.getTestApartamentoId() + "/historico-ocupacao");
    }

    // ── Then ──────────────────────────────────────────────────────────────────

    @Then("o histórico do apartamento de teste contém {int} registro")
    public void historicoContem(int count) throws Exception {
        // re-authenticate as admin to ensure valid token for the GET query
        commonSteps.fazerLogin(DatabaseSetupHooks.ADMIN_EMAIL, DatabaseSetupHooks.ADMIN_PASSWORD);
        http.get("/pessoas/apartamentos/" + ctx.getTestApartamentoId() + "/historico-ocupacao");
        assertThat(ctx.getLastStatus()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(ctx.getLastResponseBody()).get("data");
        assertThat(data.size()).isEqualTo(count);
    }

    @Then("o morador anterior aparece no histórico com data_saida preenchida")
    public void moradorAnteriorNoHistorico() throws Exception {
        http.get("/pessoas/apartamentos/" + ctx.getTestApartamentoId() + "/historico-ocupacao");
        assertThat(ctx.getLastStatus()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(ctx.getLastResponseBody()).get("data");
        assertThat(data.size()).isGreaterThan(0);
        assertThat(data.get(0).get("dataSaida").asText()).isNotBlank();
    }

    @Then("a resposta contém itens em {string}")
    public void respostaContemItens(String jsonPath) throws Exception {
        JsonNode data = objectMapper.readTree(ctx.getLastResponseBody()).get("data");
        assertThat(data).isNotNull();
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThan(0);
    }

    @Then("a resposta tem lista vazia em {string}")
    public void respostaListaVazia(String jsonPath) throws Exception {
        JsonNode data = objectMapper.readTree(ctx.getLastResponseBody()).get("data");
        assertThat(data).isNotNull();
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isEqualTo(0);
    }

    @And("o morador está no histórico de ocupação do apartamento de teste")
    public void moradorNoHistorico() throws Exception {
        http.get("/pessoas/apartamentos/" + ctx.getTestApartamentoId() + "/historico-ocupacao");
        assertThat(ctx.getLastStatus()).isEqualTo(200);
        JsonNode data = objectMapper.readTree(ctx.getLastResponseBody()).get("data");
        assertThat(data.size()).isGreaterThan(0);
    }

    @Then("o morador removido tem acesso bloqueado no sistema")
    public void moradorBloqueado() {
        Boolean enabled = jdbc.queryForObject(
            "SELECT enabled FROM users WHERE id = ?", Boolean.class, ctx.getTestPessoaId());
        assertThat(enabled).isFalse();
    }

    @Then("o morador removido mantém acesso ao sistema")
    public void moradorNaoBloqueado() {
        Boolean enabled = jdbc.queryForObject(
            "SELECT enabled FROM users WHERE id = ?", Boolean.class, ctx.getTestPessoaId());
        assertThat(enabled).isTrue();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void criarMoradorNoApartamento(String email, String nome, String cpf) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", nome);
        body.put("email", email);
        body.put("cpf", cpf);
        http.post("/pessoas", objectMapper.writeValueAsString(body));
        ctx.setTestPessoaId(ctx.getLastCreatedId());
        http.post(
            "/pessoas/" + ctx.getTestPessoaId()
                + "/apartamento?apartamentoId=" + ctx.getTestApartamentoId(),
            "{}");
    }
}
