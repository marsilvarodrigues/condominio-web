package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import com.pmrodrigues.gateway.dto.AsaasEmissaoResult;
import com.pmrodrigues.gateway.service.AsaasGatewayService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Step definitions for the Cobrança module BDD scenarios.
 */
@RequiredArgsConstructor
public class CobrancaSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;
    private final AsaasGatewayService asaasGatewayService;

    // ── Background ────────────────────────────────────────────────────────────

    @And("existe uma execução de rateio com cotas para 3 apartamentos")
    public void criarExecucaoDeRateio() {
        Long condominioId = ctx.getTestCondominioId();
        Long blocoId = ctx.getTestBlocoId();
        Long apt1Id = ctx.getTestApartamentoId();

        Long apt2Id = jdbc.queryForObject(
                "INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, deleted) " +
                "VALUES (?, ?, '102', 65.50, false) RETURNING id",
                Long.class, condominioId, blocoId);

        Long apt3Id = jdbc.queryForObject(
                "INSERT INTO apartamentos (condominio_id, bloco_id, numero, area_construida, deleted) " +
                "VALUES (?, ?, '103', 65.50, false) RETURNING id",
                Long.class, condominioId, blocoId);

        Long user1Id = criarUsuario("morador1.bdd@test.com", "Morador 1 BDD");
        Long user2Id = criarUsuario("morador2.bdd@test.com", "Morador 2 BDD");
        Long user3Id = criarUsuario("morador3.bdd@test.com", "Morador 3 BDD");

        criarMorador(user1Id, condominioId, apt1Id, "111.111.111-11");
        criarMorador(user2Id, condominioId, apt2Id, "222.222.222-22");
        criarMorador(user3Id, condominioId, apt3Id, "333.333.333-33");

        Long grupoDespesaId = jdbc.queryForObject(
                "INSERT INTO grupos_despesa (condominio_id, nome, tipo_rateio, escopo, deleted) " +
                "VALUES (?, 'Condomínio BDD', 'IGUALITARIO', 'TODOS', false) RETURNING id",
                Long.class, condominioId);

        Long despesaId = jdbc.queryForObject(
                "INSERT INTO despesas (condominio_id, grupo_despesa_id, descricao, valor_total, competencia, rateio_status, deleted) " +
                "VALUES (?, ?, 'Taxa BDD', 1500.00, CURRENT_DATE, 'RATEADA', false) RETURNING id",
                Long.class, condominioId, grupoDespesaId);

        Long execucaoId = jdbc.queryForObject(
                "INSERT INTO rateio_execucoes (condominio_id, despesa_id, grupo_despesa_id, tipo_execucao, " +
                "data_execucao, despesa_total, total_unidades, total_cotas, status) " +
                "VALUES (?, ?, ?, 'MANUAL', NOW(), 1500.00, 3, 1500.00, 'SUCESSO') RETURNING id",
                Long.class, condominioId, despesaId, grupoDespesaId);

        ctx.setTestExecucaoId(execucaoId);

        jdbc.update("INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id) " +
                "VALUES (?, ?, ?, 500.00, ?)", condominioId, despesaId, apt1Id, execucaoId);
        jdbc.update("INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id) " +
                "VALUES (?, ?, ?, 500.00, ?)", condominioId, despesaId, apt2Id, execucaoId);
        jdbc.update("INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id) " +
                "VALUES (?, ?, ?, 500.00, ?)", condominioId, despesaId, apt3Id, execucaoId);

        when(asaasGatewayService.emitir(any())).thenReturn(
                new AsaasEmissaoResult("pay_bdd", "cus_bdd", "http://boleto.url", "123456", null, null));
    }

    // ── Gerar ─────────────────────────────────────────────────────────────────

    @When("eu gero cobranças para a execução de rateio com vencimento em 30 dias")
    public void gerarCobrancas() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("execucaoId", ctx.getTestExecucaoId());
        body.put("vencimento", LocalDate.now().plusDays(30).toString());
        http.post("/cobrancas/gerar", objectMapper.writeValueAsString(body));
    }

    @Given("cobranças já foram geradas para a execução de rateio")
    public void gerarCobrancasPrimeiro() throws Exception {
        gerarCobrancas();
    }

    @When("eu gero cobranças novamente para a mesma execução")
    public void gerarCobrancasNovamente() throws Exception {
        gerarCobrancas();
    }

    @When("eu tento gerar cobranças")
    public void tentarGerarCobrancas() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("execucaoId", 1L);
        body.put("vencimento", LocalDate.now().plusDays(30).toString());
        http.post("/cobrancas/gerar", objectMapper.writeValueAsString(body));
    }

    // ── Cobrança fixture ──────────────────────────────────────────────────────

    @Given("existe uma cobrança com status PENDENTE")
    public void criarCobrancaPendente() {
        Long condominioId = ctx.getTestCondominioId();
        Long aptId = ctx.getTestApartamentoId();
        Long cotaRateioId = buscarOuCriarCotaRateio(condominioId, aptId);

        Long cobrancaId = jdbc.queryForObject(
                "INSERT INTO cobrancas (condominio_id, apartamento_id, cota_rateio_id, valor, " +
                "vencimento, status, asaas_id, email_enviado, deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, 500.00, ?, 'PENDENTE', 'pay_test', false, false, NOW(), NOW()) RETURNING id",
                Long.class, condominioId, aptId, cotaRateioId, LocalDate.now().plusDays(30));

        ctx.setTestCobrancaId(cobrancaId);
    }

    // ── Cancelar ──────────────────────────────────────────────────────────────

    @When("eu cancelo a cobrança com motivo {string}")
    public void cancelarCobranca(String motivo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("motivo", motivo);
        http.post("/cobrancas/" + ctx.getTestCobrancaId() + "/cancelar",
                objectMapper.writeValueAsString(body));
    }

    // ── Reenviar email ────────────────────────────────────────────────────────

    @When("eu reenvio o e-mail da cobrança")
    public void reenviarEmailCobranca() {
        http.post("/cobrancas/" + ctx.getTestCobrancaId() + "/reenviar-email", "{}");
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Long criarUsuario(String email, String nome) {
        jdbc.update("INSERT INTO users (email, password, name, enabled, deleted) VALUES (?, ?, ?, ?, ?)",
                email, "$2a$10$placeholder", nome, true, false);
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private void criarMorador(Long userId, Long condominioId, Long apartamentoId, String cpf) {
        jdbc.update("INSERT INTO pessoas (id, condominio_id, apartamento_id, pessoa_tipo) VALUES (?, ?, ?, 'MORADOR')",
                userId, condominioId, apartamentoId);
        jdbc.update("INSERT INTO moradores (id, cpf) VALUES (?, ?)", userId, cpf);
    }

    private Long buscarOuCriarCotaRateio(Long condominioId, Long aptId) {
        if (ctx.getTestExecucaoId() != null) {
            Long cotaId = jdbc.queryForObject(
                    "SELECT id FROM cotas_rateio WHERE apartamento_id = ? AND condominio_id = ? LIMIT 1",
                    Long.class, aptId, condominioId);
            if (cotaId != null) return cotaId;
        }
        Long grupoDespesaId = jdbc.queryForObject(
                "INSERT INTO grupos_despesa (condominio_id, nome, tipo_rateio, escopo, deleted) " +
                "VALUES (?, 'Grupo BDD Fixture', 'IGUALITARIO', 'TODOS', false) RETURNING id",
                Long.class, condominioId);
        Long despesaId = jdbc.queryForObject(
                "INSERT INTO despesas (condominio_id, grupo_despesa_id, descricao, valor_total, competencia, rateio_status, deleted) " +
                "VALUES (?, ?, 'Taxa Fixture', 500.00, CURRENT_DATE, 'RATEADA', false) RETURNING id",
                Long.class, condominioId, grupoDespesaId);
        Long execucaoId = jdbc.queryForObject(
                "INSERT INTO rateio_execucoes (condominio_id, despesa_id, grupo_despesa_id, tipo_execucao, " +
                "data_execucao, despesa_total, total_unidades, total_cotas, status) " +
                "VALUES (?, ?, ?, 'MANUAL', NOW(), 500.00, 1, 500.00, 'SUCESSO') RETURNING id",
                Long.class, condominioId, despesaId, grupoDespesaId);
        return jdbc.queryForObject(
                "INSERT INTO cotas_rateio (condominio_id, despesa_id, apartamento_id, valor, rateio_execucao_id) " +
                "VALUES (?, ?, ?, 500.00, ?) RETURNING id",
                Long.class, condominioId, despesaId, aptId, execucaoId);
    }
}
