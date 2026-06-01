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
        if (ctx.getLastStatus() == 201) {
            ctx.setTestItemOrcamentoId(ctx.getLastCreatedId());
        }
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

    // ── Banco ─────────────────────────────────────────────────────────────

    @When("eu crio um banco com codigo {string} e nome {string}")
    public void criarBanco(String codigo, String nome) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", codigo);
        body.put("nome", nome);
        http.post("/bancos", objectMapper.writeValueAsString(body));
        if (ctx.getLastStatus() == 201) {
            ctx.setTestBancoId(ctx.getLastCreatedId());
        }
    }

    @When("eu tento criar um banco sem codigo")
    public void tentarCriarBancoSemCodigo() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", "");
        body.put("nome", "Banco Teste");
        http.post("/bancos", objectMapper.writeValueAsString(body));
    }

    @And("eu atualizo o último banco com nome {string}")
    public void atualizarUltimoBanco(String novoNome) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", novoNome);
        http.put("/bancos/" + ctx.getLastCreatedId(), objectMapper.writeValueAsString(body));
    }

    // ── Conta Bancária ────────────────────────────────────────────────────

    @When("eu crio uma conta bancaria do tipo {string} com agencia {string} e conta {string}")
    public void criarContaBancaria(String tipo, String agencia, String conta) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("bancoId", ctx.getTestBancoId());
        body.put("tipo", tipo);
        body.put("agencia", agencia);
        body.put("conta", conta);
        http.post("/contas-bancarias", objectMapper.writeValueAsString(body));
        if (ctx.getLastStatus() == 201) {
            ctx.setTestContaBancariaId(ctx.getLastCreatedId());
        }
    }

    @When("eu tento criar uma conta bancaria sem agencia")
    public void tentarCriarContaBancariaSemAgencia() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("bancoId", ctx.getTestBancoId());
        body.put("tipo", "CORRENTE");
        body.put("agencia", "");
        body.put("conta", "12345");
        http.post("/contas-bancarias", objectMapper.writeValueAsString(body));
    }

    @And("eu atualizo a última conta bancaria com agencia {string}")
    public void atualizarUltimaContaBancaria(String novaAgencia) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("agencia", novaAgencia);
        body.put("conta", "56789");
        http.put("/contas-bancarias/" + ctx.getLastCreatedId(), objectMapper.writeValueAsString(body));
    }

    @And("eu ativo a última conta bancaria")
    public void ativarUltimaContaBancaria() {
        http.patch("/contas-bancarias/" + ctx.getLastCreatedId() + "/ativar", "");
    }

    @And("eu desativo a última conta bancaria")
    public void desativarUltimaContaBancaria() {
        http.patch("/contas-bancarias/" + ctx.getLastCreatedId() + "/desativar", "");
    }

    // ── Lançamento Bancário ───────────────────────────────────────────────

    @When("eu crio um lancamento do tipo {string} com valor {string} e descricao {string}")
    public void criarLancamentoBancario(String tipo, String valor, String descricao) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contaBancariaId", ctx.getTestContaBancariaId());
        body.put("dataLancamento", java.time.LocalDate.now().toString());
        body.put("valor", new java.math.BigDecimal(valor));
        body.put("tipo", tipo);
        body.put("descricao", descricao);
        body.put("origem", "MANUAL");
        http.post("/lancamentos-bancarios", objectMapper.writeValueAsString(body));
    }

    @When("eu tento criar um lancamento sem conta bancaria")
    public void tentarCriarLancamentoSemContaBancaria() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dataLancamento", java.time.LocalDate.now().toString());
        body.put("valor", new java.math.BigDecimal("100.00"));
        body.put("tipo", "CREDITO");
        body.put("descricao", "Teste");
        body.put("origem", "MANUAL");
        http.post("/lancamentos-bancarios", objectMapper.writeValueAsString(body));
    }

    @And("eu atualizo o último lancamento com valor {string}")
    public void atualizarUltimoLancamento(String novoValor) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dataLancamento", java.time.LocalDate.now().toString());
        body.put("valor", new java.math.BigDecimal(novoValor));
        body.put("descricao", "Atualizado BDD");
        http.put("/lancamentos-bancarios/" + ctx.getLastCreatedId(), objectMapper.writeValueAsString(body));
    }
}
