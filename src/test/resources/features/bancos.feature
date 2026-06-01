Feature: Gerenciamento de Bancos

  Scenario: Admin cria banco com sucesso retorna 201
    Given estou autenticado como master
    When eu crio um banco com codigo "001" e nome "Banco do Brasil"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.codigo" com valor "001"
    And a resposta tem o campo "$.data.nome" com valor "Banco do Brasil"

  Scenario: Dados inválidos retornam 400
    Given estou autenticado como master
    When eu tento criar um banco sem codigo
    Then o status da resposta é 400

  Scenario: Não autenticado recebe 401
    Given não estou autenticado
    When eu faço GET para "/bancos"
    Then o status da resposta é 401

  Scenario: Sem permissão recebe 403
    Given estou autenticado como usuário regular
    When eu crio um banco com codigo "002" e nome "Itaú"
    Then o status da resposta é 403

  Scenario: Admin lista bancos retorna 200
    Given estou autenticado como master
    When eu crio um banco com codigo "003" e nome "Caixa Econômica"
    And eu faço GET para "/bancos"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: Buscar banco por ID retorna 200
    Given estou autenticado como master
    When eu crio um banco com codigo "004" e nome "Bradesco"
    And eu busco o último recurso criado em "/bancos"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.codigo" com valor "004"

  Scenario: Inexistente retorna 404
    Given estou autenticado como master
    When eu faço GET para "/bancos/99999"
    Then o status da resposta é 404

  Scenario: Admin atualiza banco retorna 200
    Given estou autenticado como master
    When eu crio um banco com codigo "005" e nome "Banco Original"
    And eu atualizo o último banco com nome "Banco Atualizado"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.nome" com valor "Banco Atualizado"

  Scenario: Admin deleta banco retorna 204
    Given estou autenticado como master
    When eu crio um banco com codigo "006" e nome "Para Deletar"
    And eu deleto o último recurso criado em "/bancos"
    Then o status da resposta é 204
