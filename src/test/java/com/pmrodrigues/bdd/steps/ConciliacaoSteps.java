package com.pmrodrigues.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.bdd.HttpTestHelper;
import com.pmrodrigues.bdd.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for bank reconciliation association BDD scenarios.
 */
@RequiredArgsConstructor
public class ConciliacaoSteps {

    private final ScenarioContext ctx;
    private final HttpTestHelper http;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    /**
     * Creates an ExtratoImportacao and an ItemExtrato directly via JDBC,
     * since no REST API exists for these entities.
     *
     * <p>Requires that {@code ctx.testContaBancariaId} is already set (created via API).
     */
    @And("eu crio um item de extrato via JDBC com valor {string} e descricao {string}")
    public void criarItemExtratoViaJdbc(String valor, String descricao) {
        Long condominioId = ctx.getTestCondominioId();
        Long contaBancariaId = ctx.getTestContaBancariaId();

        Long extratoImportacaoId = jdbc.queryForObject(
                """
                INSERT INTO extrato_importacoes
                    (condominio_id, conta_bancaria_id, formato, data_importacao,
                     total_itens, itens_conciliados, itens_pendentes, status, deleted)
                VALUES (?, ?, 'OFX', NOW(), 1, 0, 1, 'CONCLUIDO', false)
                RETURNING id
                """,
                Long.class,
                condominioId, contaBancariaId);

        Long itemExtratoId = jdbc.queryForObject(
                """
                INSERT INTO itens_extrato
                    (condominio_id, extrato_importacao_id, data_lancamento,
                     valor, tipo, descricao, status, deleted)
                VALUES (?, ?, CURRENT_DATE, ?, 'CREDITO', ?, 'PENDENTE', false)
                RETURNING id
                """,
                Long.class,
                condominioId, extratoImportacaoId,
                new java.math.BigDecimal(valor), descricao);

        ctx.setTestItemExtratoId(itemExtratoId);
    }

    @When("eu associo o último item de extrato ao último item de orçamento")
    public void associarItemExtratoAoItemOrcamento() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("itemOrcamentoId", ctx.getTestItemOrcamentoId());
        http.post("/conciliacao/associacao/" + ctx.getTestItemExtratoId(),
                objectMapper.writeValueAsString(body));
    }

    @And("eu desassocio o último item de extrato com justificativa {string}")
    public void desassociarItemExtrato(String justificativa) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("justificativa", justificativa);
        http.deleteWithBody("/conciliacao/associacao/" + ctx.getTestItemExtratoId(),
                objectMapper.writeValueAsString(body));
    }

    @When("eu busco sugestões para o último item de extrato")
    public void buscarSugestoes() {
        http.get("/conciliacao/associacao/sugestoes/" + ctx.getTestItemExtratoId());
    }

    @When("eu calculo a contribuição do último item de orçamento")
    public void calcularContribuicao() {
        http.get("/conciliacao/associacao/contribuicao/" + ctx.getTestItemOrcamentoId());
    }

    @When("eu tento associar o último item de extrato sem informar o item de orçamento")
    public void associarSemItemOrcamento() throws Exception {
        http.post("/conciliacao/associacao/" + ctx.getTestItemExtratoId(), "{}");
    }

    @When("eu tento associar um item de extrato inexistente ao último item de orçamento")
    public void associarItemExtratoInexistente() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("itemOrcamentoId", ctx.getTestItemOrcamentoId());
        http.post("/conciliacao/associacao/99999", objectMapper.writeValueAsString(body));
    }

    @When("eu tento desassociar o último item de extrato sem justificativa")
    public void desassociarSemJustificativa() throws Exception {
        http.deleteWithBody("/conciliacao/associacao/" + ctx.getTestItemExtratoId(),
                "{\"justificativa\":\"\"}");
    }

    @When("eu tento desassociar um item de extrato inexistente com justificativa {string}")
    public void desassociarItemExtratoInexistente(String justificativa) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("justificativa", justificativa);
        http.deleteWithBody("/conciliacao/associacao/99999", objectMapper.writeValueAsString(body));
    }
}
