Feature: Gerenciamento de Plano de Contas

  Scenario: Admin cria plano de contas com sucesso retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "1" e descricao "Receitas" do tipo "RECEITA"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.codigo" com valor "1"

  Scenario: Admin cria plano de contas com tipoRateio e escopo retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "1A" descricao "Taxa Cond" tipo "RECEITA" tipoRateio "IGUALITARIO" e escopo "TODOS"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.tipoRateio" com valor "IGUALITARIO"
    And a resposta tem o campo "$.data.escopoRateio" com valor "TODOS"

  Scenario: Admin cria plano de contas com fracao ideal por bloco retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "1B" descricao "Manutenção" tipo "DESPESA" tipoRateio "FRACAO_IDEAL" e escopo "POR_BLOCO"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.tipoRateio" com valor "FRACAO_IDEAL"
    And a resposta tem o campo "$.data.escopoRateio" com valor "POR_BLOCO"

  Scenario: Dados inválidos retornam 400
    Given estou autenticado como admin do condomínio
    When eu tento criar um plano de contas sem codigo
    Then o status da resposta é 400

  Scenario: Não autenticado recebe 401
    Given não estou autenticado
    When eu faço GET para "/plano-contas"
    Then o status da resposta é 401

  Scenario: Sem permissão recebe 403
    Given estou autenticado como usuário regular
    When eu crio um plano de contas com codigo "2" e descricao "Despesas" do tipo "DESPESA"
    Then o status da resposta é 403

  Scenario: Admin lista planos de contas retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "3" e descricao "Taxas" do tipo "RECEITA"
    And eu faço GET para "/plano-contas"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: Buscar plano de contas por ID retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "4" e descricao "Manutenção" do tipo "DESPESA"
    And eu busco o último recurso criado em "/plano-contas"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.codigo" com valor "4"

  Scenario: Inexistente retorna 404
    Given estou autenticado como admin do condomínio
    When eu faço GET para "/plano-contas/99999"
    Then o status da resposta é 404

  Scenario: Admin atualiza plano de contas retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "5" e descricao "Original" do tipo "RECEITA"
    And eu atualizo o último plano de contas com descricao "Atualizado"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.descricao" com valor "Atualizado"

  Scenario: Admin deleta plano de contas sem filhos retorna 204
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "6" e descricao "Para deletar" do tipo "DESPESA"
    And eu deleto o último recurso criado em "/plano-contas"
    Then o status da resposta é 204

  Scenario: Listar árvore de planos de contas retorna 200
    Given estou autenticado como admin do condomínio
    When eu faço GET para "/plano-contas/arvore"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"
