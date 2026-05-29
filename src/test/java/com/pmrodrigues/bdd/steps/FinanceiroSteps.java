package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for Plano de Contas, Fundo de Reserva, and Orçamento Anual BDD scenarios.
 */
@RequiredArgsConstructor
public class FinanceiroSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;

    // ── Plano de Contas ───────────────────────────────────────────────────

    @When("eu crio um plano de contas com codigo {string} e descricao {string} do tipo {string}")
    public void criarPlanoContas(String codigo, String descricao, String tipo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", codigo);
        body.put("descricao", descricao);
        body.put("tipo", tipo);
        http.post("/plano-contas", objectMapper.writeValueAsString(body));
        if (ctx.getLastStatus() == 201) {
            ctx.setTestPlanoContasId(ctx.getLastCreatedId());
        }
    }

    @When("eu crio um plano de contas com codigo {string} descricao {string} tipo {string} tipoRateio {string} e escopo {string}")
    public void criarPlanoContasComRateio(String codigo, String descricao, String tipo,
                                          String tipoRateio, String escopo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", codigo);
        body.put("descricao", descricao);
        body.put("tipo", tipo);
        body.put("tipoRateio", tipoRateio);
        body.put("escopoRateio", escopo);
        http.post("/plano-contas", objectMapper.writeValueAsString(body));
        if (ctx.getLastStatus() == 201) {
            ctx.setTestPlanoContasId(ctx.getLastCreatedId());
        }
    }

    @When("eu tento criar um plano de contas sem codigo")
    public void tentarCriarPlanoContasSemCodigo() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", "");
        body.put("descricao", "Teste");
        body.put("tipo", "RECEITA");
        http.post("/plano-contas", objectMapper.writeValueAsString(body));
    }

    @And("eu atualizo o último plano de contas com descricao {string}")
    public void atualizarUltimoPlanoContas(String novaDescricao) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", ctx.getLastCreatedId());
        body.put("codigo", "UPD");
        body.put("descricao", novaDescricao);
        body.put("tipo", "RECEITA");
        http.put("/plano-contas/" + ctx.getLastCreatedId(), objectMapper.writeValueAsString(body));
    }

    // ── Fundo de Reserva ──────────────────────────────────────────────────

    @When("eu crio um fundo de reserva com percentual {string} e conta {string}")
    public void criarFundoReserva(String percentual, String conta) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("percentualArrecadacao", new java.math.BigDecimal(percentual));
        // contaBancariaId is optional (Long); BDD tests omit it
        http.post("/fundo-reserva", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um fundo de reserva com percentual inválido")
    public void tentarCriarFundoReservaInvalido() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("percentualArrecadacao", new java.math.BigDecimal("150.00"));
        // contaBancariaId is optional (Long); omit in invalid-percentual test
        http.post("/fundo-reserva", objectMapper.writeValueAsString(body));
    }

    @And("eu credito {string} no fundo de reserva")
    public void creditarFundoReserva(String valor) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("valor", new java.math.BigDecimal(valor));
        body.put("justificativa", "Crédito BDD");
        http.post("/fundo-reserva/creditar", objectMapper.writeValueAsString(body));
    }

    @And("eu debito {string} do fundo de reserva com justificativa {string}")
    public void debitarFundoReserva(String valor, String justificativa) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("valor", new java.math.BigDecimal(valor));
        body.put("justificativa", justificativa);
        http.post("/fundo-reserva/debitar", objectMapper.writeValueAsString(body));
    }

    @And("eu atualizo o fundo de reserva com percentual {string} e conta {string}")
    public void atualizarFundoReserva(String percentual, String conta) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("percentualArrecadacao", new java.math.BigDecimal(percentual));
        // contaBancariaId is optional (Long); BDD tests omit it
        http.put("/fundo-reserva", objectMapper.writeValueAsString(body));
    }

    // ── Orçamento Anual ───────────────────────────────────────────────────

    @When("eu crio um orçamento para o exercício {int}")
    public void criarOrcamento(int exercicio) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercicio", exercicio);
        http.post("/orcamentos", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um orçamento com exercício inválido")
    public void tentarCriarOrcamentoInvalido() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercicio", 1900);
        http.post("/orcamentos", objectMapper.writeValueAsString(body));
    }

    @And("eu atualizo o último orçamento para o exercício {int}")
    public void atualizarUltimoOrcamento(int exercicio) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exercicio", exercicio);
        http.put("/orcamentos/" + ctx.getLastCreatedId(), objectMapper.writeValueAsString(body));
    }

    @And("eu adiciono um item ao último orçamento com valor {string}")
    public void adicionarItemOrcamento(String valor) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("planoContasId", ctx.getTestPlanoContasId());
        body.put("valorPrevisto", new java.math.BigDecimal(valor));
        Long orcamentoId = ctx.getLastCreatedId();
        http.post("/orcamentos/" + orcamentoId + "/itens", objectMapper.writeValueAsString(body));
    }

    @And("eu aprovo o último orçamento com {int} unidades")
    public void aprovarUltimoOrcamento(int unidades) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("numeroUnidades", unidades);
        http.patch("/orcamentos/" + ctx.getLastCreatedId() + "/aprovar", objectMapper.writeValueAsString(body));
    }

    @And("eu encerro o último orçamento")
    public void encerrarUltimoOrcamento() throws Exception {
        http.patch("/orcamentos/" + ctx.getLastCreatedId() + "/encerrar", "{}");
    }
}
