Feature: Gerenciamento de Lançamentos Bancários

  Background:
    Given estou autenticado como admin do condomínio
    And eu crio um banco com codigo "200" e nome "Banco Lancamentos"
    And eu crio uma conta bancaria do tipo "CORRENTE" com agencia "2001" e conta "20001"

  Scenario: Admin cria lancamento com sucesso retorna 201
    When eu crio um lancamento do tipo "CREDITO" com valor "500.00" e descricao "Taxa condominial"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.tipo" com valor "CREDITO"
    And a resposta tem o campo "$.data.status" com valor "PENDENTE"

  Scenario: Dados inválidos retornam 400
    When eu tento criar um lancamento sem conta bancaria
    Then o status da resposta é 400

  Scenario: Não autenticado recebe 401
    Given não estou autenticado
    When eu faço GET para "/lancamentos-bancarios"
    Then o status da resposta é 401

  Scenario: Sem permissão recebe 403
    Given estou autenticado como usuário regular
    When eu crio um lancamento do tipo "CREDITO" com valor "100.00" e descricao "Teste"
    Then o status da resposta é 403

  Scenario: Admin lista lancamentos retorna 200
    When eu crio um lancamento do tipo "DEBITO" com valor "200.00" e descricao "Despesa agua"
    And eu faço GET para "/lancamentos-bancarios"
    Then o status da resposta é 200

  Scenario: Buscar lancamento por ID retorna 200
    When eu crio um lancamento do tipo "CREDITO" com valor "300.00" e descricao "Taxa extra"
    And eu busco o último recurso criado em "/lancamentos-bancarios"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.tipo" com valor "CREDITO"

  Scenario: Inexistente retorna 404
    When eu faço GET para "/lancamentos-bancarios/99999"
    Then o status da resposta é 404

  Scenario: Admin atualiza lancamento retorna 200
    When eu crio um lancamento do tipo "CREDITO" com valor "400.00" e descricao "Valor original"
    And eu atualizo o último lancamento com valor "450.00"
    Then o status da resposta é 200

  Scenario: Admin deleta lancamento retorna 204
    When eu crio um lancamento do tipo "DEBITO" com valor "150.00" e descricao "Para deletar"
    And eu deleto o último recurso criado em "/lancamentos-bancarios"
    Then o status da resposta é 204
