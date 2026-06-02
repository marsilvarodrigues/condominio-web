Feature: Grupos de Despesa e Rateio

  Scenario: Admin cria grupo de despesa com sucesso retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um grupo de despesa com nome "Condomínio Geral" e tipoRateio "IGUALITARIO"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.nome" com valor "Condomínio Geral"
    And a resposta tem o campo "$.data.tipoRateio" com valor "IGUALITARIO"

  Scenario: Dados inválidos ao criar grupo de despesa retornam 400
    Given estou autenticado como admin do condomínio
    When eu tento criar um grupo de despesa sem nome
    Then o status da resposta é 400

  Scenario: Não autenticado ao listar grupos de despesa recebe 401
    Given não estou autenticado
    When eu faço GET para "/grupos-despesa"
    Then o status da resposta é 401

  Scenario: Sem permissão ao criar grupo de despesa recebe 403
    Given estou autenticado como usuário regular
    When eu crio um grupo de despesa com nome "Geral" e tipoRateio "IGUALITARIO"
    Then o status da resposta é 403

  Scenario: Grupo de despesa inexistente retorna 404
    Given estou autenticado como admin do condomínio
    When eu faço GET para "/grupos-despesa/999999"
    Then o status da resposta é 404

  Scenario: Admin lista grupos de despesa retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um grupo de despesa com nome "Água" e tipoRateio "CONSUMO"
    And eu faço GET para "/grupos-despesa"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: Admin busca grupo de despesa por ID retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um grupo de despesa com nome "Manutenção" e tipoRateio "FRACAO_IDEAL"
    And eu busco o último recurso criado em "/grupos-despesa"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.tipoRateio" com valor "FRACAO_IDEAL"

  Scenario: Admin deleta grupo de despesa retorna 204
    Given estou autenticado como admin do condomínio
    When eu crio um grupo de despesa com nome "Para deletar" e tipoRateio "IGUALITARIO"
    And eu deleto o último recurso criado em "/grupos-despesa"
    Then o status da resposta é 204

  Scenario: Recalcular sem confirmar retorna 400
    Given estou autenticado como admin do condomínio
    When eu envio recalcular rateio com confirmar false
    Then o status da resposta é 400
