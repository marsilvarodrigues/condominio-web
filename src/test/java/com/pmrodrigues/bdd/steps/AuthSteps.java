package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class AuthSteps {

    private final ScenarioContext ctx;
    private final ObjectMapper objectMapper;
    private final HttpTestHelper http;

    @When("eu faço login com {string} e senha {string}")
    @SuppressWarnings("unchecked")
    public void fazerLoginComCredenciais(String email, String senha) throws Exception {
        http.post("/auth/login",
                objectMapper.writeValueAsString(Map.of("email", email, "password", senha)));
        if (ctx.getLastStatus() == 200) {
            Map<String, Object> map = objectMapper.readValue(ctx.getLastResponseBody(), Map.class);
            ctx.setAccessToken((String) map.get("accessToken"));
            ctx.setRefreshToken((String) map.get("refreshToken"));
        }
    }

    @When("eu faço refresh do token")
    @SuppressWarnings("unchecked")
    public void fazerRefreshToken() throws Exception {
        http.post("/auth/refresh",
                objectMapper.writeValueAsString(Map.of("refreshToken", ctx.getRefreshToken())));
        if (ctx.getLastStatus() == 200) {
            Map<String, Object> map = objectMapper.readValue(ctx.getLastResponseBody(), Map.class);
            ctx.setAccessToken((String) map.get("accessToken"));
        }
    }

    @When("eu faço logout")
    public void fazerLogout() {
        http.post("/auth/logout", null);
    }

    @And("a resposta contém um accessToken")
    @SuppressWarnings("unchecked")
    public void verificarAccessTokenNaResposta() throws Exception {
        Map<String, Object> map = objectMapper.readValue(ctx.getLastResponseBody(), Map.class);
        assertThat(map).as("accessToken ausente no body: %s", ctx.getLastResponseBody())
                .containsKey("accessToken");
        assertThat((String) map.get("accessToken")).isNotBlank();
    }

    @And("a resposta contém um refreshToken")
    @SuppressWarnings("unchecked")
    public void verificarRefreshTokenNaResposta() throws Exception {
        Map<String, Object> map = objectMapper.readValue(ctx.getLastResponseBody(), Map.class);
        assertThat(map).as("refreshToken ausente no body: %s", ctx.getLastResponseBody())
                .containsKey("refreshToken");
        assertThat((String) map.get("refreshToken")).isNotBlank();
    }
}
