Feature: Gerenciamento de Orçamento Anual

  Scenario: Admin cria orçamento com sucesso retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um orçamento para o exercício 2026
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.status" com valor "RASCUNHO"

  Scenario: Dados inválidos retornam 400
    Given estou autenticado como admin do condomínio
    When eu tento criar um orçamento com exercício inválido
    Then o status da resposta é 400

  Scenario: Não autenticado recebe 401
    Given não estou autenticado
    When eu faço GET para "/orcamentos"
    Then o status da resposta é 401

  Scenario: Sem permissão recebe 403
    Given estou autenticado como usuário regular
    When eu crio um orçamento para o exercício 2026
    Then o status da resposta é 403

  Scenario: Admin lista orçamentos retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um orçamento para o exercício 2026
    And eu faço GET para "/orcamentos"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: Buscar orçamento por ID retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um orçamento para o exercício 2026
    And eu busco o último recurso criado em "/orcamentos"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.exercicio" com valor "2026"

  Scenario: Inexistente retorna 404
    Given estou autenticado como admin do condomínio
    When eu faço GET para "/orcamentos/99999"
    Then o status da resposta é 404

  Scenario: Admin atualiza orçamento em rascunho retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um orçamento para o exercício 2026
    And eu atualizo o último orçamento para o exercício 2027
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.exercicio" com valor "2027"

  Scenario: Admin adiciona item ao orçamento retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um plano de contas com codigo "10" e descricao "Limpeza" do tipo "DESPESA"
    And eu crio um orçamento para o exercício 2026
    And eu adiciono um item ao último orçamento com valor "500.00"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.valorPrevisto" com valor "500.0"

  Scenario: Admin aprova orçamento retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um orçamento para o exercício 2026
    And eu aprovo o último orçamento com 10 unidades
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.status" com valor "APROVADO"

  Scenario: Admin encerra orçamento aprovado retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um orçamento para o exercício 2026
    And eu aprovo o último orçamento com 10 unidades
    And eu encerro o último orçamento
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.status" com valor "ENCERRADO"

  Scenario: Admin deleta orçamento em rascunho retorna 204
    Given estou autenticado como admin do condomínio
    When eu crio um orçamento para o exercício 2026
    And eu deleto o último recurso criado em "/orcamentos"
    Then o status da resposta é 204
