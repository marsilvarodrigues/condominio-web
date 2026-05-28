Feature: Gerenciamento de Condomínios

  Background:
    Given estou autenticado como master

  Scenario: Listar condomínios retorna lista com o condomínio de teste
    When eu faço GET para "/condominios"
    Then o status da resposta é 200
    And a resposta contém uma lista com pelo menos 1 item em "$.data"

  Scenario: Filtrar condomínios por nome
    When eu faço GET para "/condominios?nome=Cond Test"
    Then o status da resposta é 200
    And a resposta contém uma lista com pelo menos 1 item em "$.data"

  Scenario: Filtrar condomínios por CNPJ
    When eu faço GET para "/condominios?cnpj=00.000.000/0001-00"
    Then o status da resposta é 200
    And a resposta contém uma lista com pelo menos 1 item em "$.data"

  Scenario: Buscar condomínio de teste por ID
    When eu busco o condomínio de teste por ID
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.nome" com valor "Cond Test BDD"

  Scenario: Buscar condomínio inexistente retorna 404
    When eu faço GET para "/condominios/99999"
    Then o status da resposta é 404

  Scenario: Admin cria novo condomínio retorna 201
    When eu crio um condomínio com nome "Cond Novo BDD" e cnpj "12.345.678/0001-95"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.nome" com valor "Cond Novo BDD"

  Scenario: Criar condomínio sem nome retorna 400
    When eu tento criar um condomínio sem nome
    Then o status da resposta é 400
    And a resposta tem o campo "$.data.error" com valor "validation_error"

  Scenario: Criar condomínio com CNPJ inválido retorna 400
    When eu tento criar um condomínio com cnpj inválido
    Then o status da resposta é 400
    And a resposta tem o campo "$.data.error" com valor "validation_error"
    And a resposta contém erro de validação em "cnpj"

  Scenario: Criar condomínio com CEP inválido retorna 400
    When eu tento criar um condomínio com cep inválido
    Then o status da resposta é 400
    And a resposta tem o campo "$.data.error" com valor "validation_error"

  Scenario: Atualizar condomínio retorna 200
    When eu atualizo o condomínio de teste com nome "Cond Atualizado BDD"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.nome" com valor "Cond Atualizado BDD"

  Scenario: Deletar condomínio retorna 204
    Given eu crio um condomínio com nome "Para Deletar" e cnpj "98.765.432/0001-98"
    When eu deleto o último recurso criado em "/condominios"
    Then o status da resposta é 204

  Scenario: ROLE_USER não pode criar condomínio (403)
    Given estou autenticado como usuário regular
    When eu crio um condomínio com nome "Proibido" e cnpj "45.678.901/0001-75"
    Then o status da resposta é 403

  Scenario: Sem autenticação retorna 401
    Given não estou autenticado
    When eu faço GET para "/condominios"
    Then o status da resposta é 401
