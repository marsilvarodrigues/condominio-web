Feature: Gerenciamento de Contas Bancárias

  Background:
    Given estou autenticado como admin do condomínio
    And eu crio um banco com codigo "100" e nome "Banco Teste"

  Scenario: Admin cria conta bancaria com sucesso retorna 201
    When eu crio uma conta bancaria do tipo "CORRENTE" com agencia "1234" e conta "56789"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.tipo" com valor "CORRENTE"
    And a resposta tem o campo "$.data.agencia" com valor "1234"

  Scenario: Dados inválidos retornam 400
    When eu tento criar uma conta bancaria sem agencia
    Then o status da resposta é 400

  Scenario: Não autenticado recebe 401
    Given não estou autenticado
    When eu faço GET para "/contas-bancarias"
    Then o status da resposta é 401

  Scenario: Sem permissão recebe 403
    Given estou autenticado como usuário regular
    When eu crio uma conta bancaria do tipo "CORRENTE" com agencia "9999" e conta "11111"
    Then o status da resposta é 403

  Scenario: Admin lista contas bancarias retorna 200
    When eu crio uma conta bancaria do tipo "CORRENTE" com agencia "2222" e conta "33333"
    And eu faço GET para "/contas-bancarias"
    Then o status da resposta é 200

  Scenario: Buscar conta bancaria por ID retorna 200
    When eu crio uma conta bancaria do tipo "POUPANCA" com agencia "4444" e conta "55555"
    And eu busco o último recurso criado em "/contas-bancarias"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.tipo" com valor "POUPANCA"

  Scenario: Inexistente retorna 404
    When eu faço GET para "/contas-bancarias/99999"
    Then o status da resposta é 404

  Scenario: Admin atualiza conta bancaria retorna 200
    When eu crio uma conta bancaria do tipo "CORRENTE" com agencia "6666" e conta "77777"
    And eu atualizo a última conta bancaria com agencia "8888"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.agencia" com valor "8888"

  Scenario: Admin ativa e desativa conta bancaria retorna 200
    When eu crio uma conta bancaria do tipo "CORRENTE" com agencia "1111" e conta "22222"
    And eu desativo a última conta bancaria
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.ativa" com valor "false"

  Scenario: Admin deleta conta bancaria retorna 204
    When eu crio uma conta bancaria do tipo "CORRENTE" com agencia "3333" e conta "44444"
    And eu deleto o último recurso criado em "/contas-bancarias"
    Then o status da resposta é 204
