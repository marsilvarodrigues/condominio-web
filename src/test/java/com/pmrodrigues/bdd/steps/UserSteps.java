package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class UserSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    @When("eu crio um usuário com email {string} no condomínio de teste")
    public void criarUsuario(String email) throws Exception {
        ctx.setLastCreatedEmail(email);
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("name", "Test User BDD");
        body.put("enabled", true);
        body.put("roles", Set.of("ROLE_USER"));
        body.put("condominioId", ctx.getTestCondominioId());
        http.post("/users", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um usuário sem condominioId com email {string}")
    public void tentarCriarUsuarioSemCondominioId(String email) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("name", "Sem Cond BDD");
        body.put("roles", Set.of("ROLE_USER"));
        // condominioId ausente intencionalmente
        http.post("/users", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um usuário com condominioId inválido com email {string}")
    public void tentarCriarUsuarioComCondominioInvalido(String email) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("name", "Cond Invalido BDD");
        body.put("roles", Set.of("ROLE_USER"));
        body.put("condominioId", 999999L);
        http.post("/users", objectMapper.writeValueAsString(body));
    }

    @When("eu atualizo o último usuário criado com nome {string}")
    public void atualizarUltimoUsuario(String novoNome) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", ctx.getLastCreatedId());
        body.put("email", ctx.getLastCreatedEmail());
        body.put("name", novoNome);
        body.put("enabled", true);
        body.put("roles", Set.of("ROLE_USER"));
        body.put("condominioId", ctx.getTestCondominioId());
        http.put("/users/" + ctx.getLastCreatedId(), objectMapper.writeValueAsString(body));
    }

    @When("eu atualizo meus próprios dados com nome {string}")
    public void atualizarPropriosDados(String novoNome) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", ctx.getRegularUserId());
        body.put("email", "user@bdd.com");
        body.put("name", novoNome);
        body.put("enabled", true);
        body.put("roles", Set.of("ROLE_USER"));
        body.put("condominioId", ctx.getTestCondominioId());
        http.put("/users/" + ctx.getRegularUserId(), objectMapper.writeValueAsString(body));
    }

    @When("eu tento atualizar o usuário master com nome {string}")
    public void tentarAtualizarUsuarioMaster(String nome) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", ctx.getMasterUserId());
        body.put("email", "master@bdd.com");
        body.put("name", nome);
        body.put("enabled", true);
        body.put("roles", Set.of("ROLE_ADMIN"));
        // condominioId required by UserDTO @NotNull; pass testCondominioId so validation passes
        // before the service's ownership check throws 403.
        body.put("condominioId", ctx.getTestCondominioId());
        http.put("/users/" + ctx.getMasterUserId(), objectMapper.writeValueAsString(body));
    }

    @When("eu altero minha senha de {string} para {string} confirmando {string}")
    public void alterarSenha(String senhaAtual, String novaSenha, String confirmacao) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("currentPassword", senhaAtual);
        body.put("newPassword", novaSenha);
        body.put("confirmPassword", confirmacao);
        http.patch("/users/" + ctx.getMasterUserId() + "/password", objectMapper.writeValueAsString(body));
    }
}
