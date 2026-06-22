package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import com.pmrodrigues.bdd.hooks.DatabaseSetupHooks;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps genéricos reutilizados por todos os domínios.
 *
 * <p>Contém:
 * <ul>
 *   <li>Given: autenticação como master/admin/regular/sem token</li>
 *   <li>When: requisições HTTP genéricas (GET, DELETE) e login</li>
 *   <li>Then: assertions de status e campos de resposta</li>
 * </ul>
 */
@RequiredArgsConstructor
public class CommonSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    // ── Given: estado de autenticação ─────────────────────────────────────────

    @Given("estou autenticado como master")
    public void autenticarComoMaster() throws Exception {
        fazerLogin(DatabaseSetupHooks.MASTER_EMAIL, DatabaseSetupHooks.MASTER_PASSWORD);
    }

    @Given("estou autenticado como admin do condomínio")
    public void autenticarComoAdmin() throws Exception {
        fazerLogin(DatabaseSetupHooks.ADMIN_EMAIL, DatabaseSetupHooks.ADMIN_PASSWORD);
    }

    @Given("estou autenticado como síndico")
    public void autenticarComoSindico() throws Exception {
        fazerLogin(DatabaseSetupHooks.SINDICO_EMAIL, DatabaseSetupHooks.SINDICO_PASSWORD);
    }

    @Given("estou autenticado como usuário regular")
    public void autenticarComoUsuarioRegular() throws Exception {
        fazerLogin(DatabaseSetupHooks.USER_EMAIL, DatabaseSetupHooks.USER_PASSWORD);
    }

    @Given("não estou autenticado")
    public void semAutenticacao() {
        ctx.setAccessToken(null);
        ctx.setRefreshToken(null);
    }

    // ── When: requisições HTTP genéricas ──────────────────────────────────────

    @When("eu faço GET para {string}")
    public void fazerGet(String path) {
        http.get(path);
    }

    @When("eu faço DELETE para {string}")
    public void fazerDelete(String path) {
        http.delete(path);
    }

    @When("eu busco o último recurso criado em {string}")
    public void buscarUltimoRecursoById(String baseUrl) {
        http.get(baseUrl + "/" + ctx.getLastCreatedId());
    }

    @When("eu deleto o último recurso criado em {string}")
    public void deletarUltimoRecurso(String baseUrl) {
        http.delete(baseUrl + "/" + ctx.getLastCreatedId());
    }

    // ── Then: assertions ──────────────────────────────────────────────────────

    @Then("o status da resposta é {int}")
    public void verificarStatus(int expectedStatus) {
        assertThat(ctx.getLastStatus())
                .as("HTTP status esperado %d mas foi %d. Body: %s",
                        expectedStatus, ctx.getLastStatus(), ctx.getLastResponseBody())
                .isEqualTo(expectedStatus);
    }

    @Then("a resposta tem o campo {string} com valor {string}")
    public void verificarCampo(String jsonPath, String expectedValue) throws Exception {
        String body = ctx.getLastResponseBody();
        assertThat(body).as("Response body vazio").isNotBlank();
        JsonNode root = objectMapper.readTree(body);
        JsonNode node = navigatePath(root, jsonPath);
        assertThat(node.asText())
                .as("Campo %s no body: %s", jsonPath, body)
                .isEqualTo(expectedValue);
    }

    @And("a resposta contém uma lista em {string}")
    public void verificarListaNoBody(String jsonPath) throws Exception {
        JsonNode node = navigatePath(objectMapper.readTree(ctx.getLastResponseBody()), jsonPath);
        assertThat(node.isArray()).as("Campo %s deve ser array. Body: %s",
                jsonPath, ctx.getLastResponseBody()).isTrue();
    }

    @And("a resposta contém uma lista com pelo menos {int} item em {string}")
    public void verificarListaComItens(int minItems, String jsonPath) throws Exception {
        JsonNode node = navigatePath(objectMapper.readTree(ctx.getLastResponseBody()), jsonPath);
        assertThat(node.isArray()).as("Deve ser array").isTrue();
        assertThat(node.size()).as("Deve ter ao menos %d items. Body: %s", minItems, ctx.getLastResponseBody())
                .isGreaterThanOrEqualTo(minItems);
    }

    @And("a resposta contém erro de validação em {string}")
    public void verificarErroValidacao(String campo) throws Exception {
        JsonNode root = objectMapper.readTree(ctx.getLastResponseBody());
        JsonNode fields = root.path("data").path("fields");
        assertThat(fields.has(campo))
                .as("Campo '%s' deve ter erro de validação. Fields: %s", campo, fields)
                .isTrue();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    void fazerLogin(String email, String password) throws Exception {
        http.post("/auth/login",
                objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
        if (ctx.getLastStatus() == 200) {
            Map<String, Object> map = objectMapper.readValue(ctx.getLastResponseBody(), Map.class);
            ctx.setAccessToken((String) map.get("accessToken"));
            ctx.setRefreshToken((String) map.get("refreshToken"));
        }
    }

    /** Navega um caminho JSONPath simples como "$.data[0].uf" ou "$.data.id". */
    private JsonNode navigatePath(JsonNode root, String path) {
        String stripped = path.startsWith("$.") ? path.substring(2) : path;
        JsonNode current = root;
        for (String token : stripped.split("\\.")) {
            if (token.contains("[")) {
                String key = token.substring(0, token.indexOf('['));
                int index = Integer.parseInt(token.replaceAll(".*\\[(\\d+)\\].*", "$1"));
                if (!key.isBlank()) current = current.path(key);
                current = current.get(index);
            } else {
                current = current.path(token);
            }
            if (current == null || current.isMissingNode()) break;
        }
        return current;
    }
}
