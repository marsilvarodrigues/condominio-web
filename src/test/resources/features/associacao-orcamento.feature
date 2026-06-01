Feature: Associação de ItemExtrato a ItemOrcamento

  Background:
    Given estou autenticado como admin do condomínio
    And eu crio um banco com codigo "400" e nome "Banco Conciliacao"
    And eu crio uma conta bancaria do tipo "CORRENTE" com agencia "4001" e conta "40001"
    And eu crio um plano de contas com codigo "4.1" e descricao "Taxa condominial BDD" do tipo "RECEITA"
    And eu crio um orçamento para o exercício 2025
    And eu adiciono um item ao último orçamento com valor "1000.00"
    And eu crio um item de extrato via JDBC com valor "500.00" e descricao "Taxa condominial BDD"

  # ── POST /conciliacao/associacao/{id} ────────────────────────────────────

  Scenario: Admin associa item de extrato a item de orçamento retorna 200
    When eu associo o último item de extrato ao último item de orçamento
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.tipo" com valor "CREDITO"

  Scenario: POST com itemOrcamentoId nulo retorna 400
    When eu tento associar o último item de extrato sem informar o item de orçamento
    Then o status da resposta é 400

  Scenario: POST não autenticado retorna 401
    Given não estou autenticado
    When eu associo o último item de extrato ao último item de orçamento
    Then o status da resposta é 401

  Scenario: POST sem permissão retorna 403
    Given estou autenticado como usuário regular
    When eu associo o último item de extrato ao último item de orçamento
    Then o status da resposta é 403

  Scenario: POST inexistente retorna 404
    When eu tento associar um item de extrato inexistente ao último item de orçamento
    Then o status da resposta é 404

  # ── DELETE /conciliacao/associacao/{id} ──────────────────────────────────

  Scenario: Admin desassocia item de extrato retorna 200
    When eu associo o último item de extrato ao último item de orçamento
    And eu desassocio o último item de extrato com justificativa "Erro de classificação"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.tipo" com valor "CREDITO"

  Scenario: DELETE com justificativa em branco retorna 400
    When eu tento desassociar o último item de extrato sem justificativa
    Then o status da resposta é 400

  Scenario: DELETE não autenticado retorna 401
    Given não estou autenticado
    When eu desassocio o último item de extrato com justificativa "Qualquer"
    Then o status da resposta é 401

  Scenario: DELETE sem permissão retorna 403
    Given estou autenticado como usuário regular
    When eu desassocio o último item de extrato com justificativa "Qualquer"
    Then o status da resposta é 403

  Scenario: DELETE inexistente retorna 404
    When eu tento desassociar um item de extrato inexistente com justificativa "Qualquer"
    Then o status da resposta é 404

  # ── GET /conciliacao/associacao/sugestoes/{id} ───────────────────────────

  Scenario: Admin obtém sugestões para item de extrato retorna 200
    When eu busco sugestões para o último item de extrato
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: GET sugestões não autenticado retorna 401
    Given não estou autenticado
    When eu faço GET para "/conciliacao/associacao/sugestoes/99999"
    Then o status da resposta é 401

  Scenario: GET sugestões inexistente retorna 404
    When eu faço GET para "/conciliacao/associacao/sugestoes/99999"
    Then o status da resposta é 404

  # ── GET /conciliacao/associacao/contribuicao/{id} ────────────────────────

  Scenario: Admin calcula contribuição sem itens associados retorna 200
    When eu calculo a contribuição do último item de orçamento
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.totalItensAssociados" com valor "0"

  Scenario: GET contribuição não autenticado retorna 401
    Given não estou autenticado
    When eu calculo a contribuição do último item de orçamento
    Then o status da resposta é 401

  Scenario: GET contribuição inexistente retorna 404
    When eu faço GET para "/conciliacao/associacao/contribuicao/99999"
    Then o status da resposta é 404
