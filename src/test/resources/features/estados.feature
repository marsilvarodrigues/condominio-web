Feature: Consulta de Estados (somente leitura)

  Background:
    Given estou autenticado como master

  Scenario: Listar todos os estados retorna lista não vazia
    When eu faço GET para "/estados"
    Then o status da resposta é 200
    And a resposta contém uma lista com pelo menos 1 item em "$.data"

  Scenario: Buscar estado por ID existente retorna 200
    When eu faço GET para "/estados/1"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.id" com valor "1"

  Scenario: Buscar estado por UF filtra resultado
    When eu faço GET para "/estados?uf=SP"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data[0].uf" com valor "SP"

  Scenario: Buscar estado por nome filtra resultado
    When eu faço GET para "/estados?nome=São Paulo"
    Then o status da resposta é 200
    And a resposta contém uma lista com pelo menos 1 item em "$.data"

  Scenario: Estado inexistente retorna 404
    When eu faço GET para "/estados/99999"
    Then o status da resposta é 404

  Scenario: Sem autenticação retorna 401
    Given não estou autenticado
    When eu faço GET para "/estados"
    Then o status da resposta é 401
