Feature: Gerenciamento de Apartamentos

  Scenario: Listar apartamentos como master
    Given estou autenticado como master
    When eu faço GET para "/apartamentos"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: Filtrar apartamentos por numero
    Given estou autenticado como master
    When eu faço GET para "/apartamentos?numero=101"
    Then o status da resposta é 200

  Scenario: Admin do condomínio cria apartamento retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um apartamento com numero "101" no bloco de teste
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.numero" com valor "101"

  Scenario: Buscar apartamento por ID
    Given estou autenticado como admin do condomínio
    Given eu crio um apartamento com numero "201" no bloco de teste
    When eu busco o último recurso criado em "/apartamentos"
    Then o status da resposta é 200

  Scenario: Buscar apartamento inexistente retorna 404
    Given estou autenticado como master
    When eu faço GET para "/apartamentos/99999"
    Then o status da resposta é 404

  Scenario: Atualizar apartamento retorna 200
    Given estou autenticado como admin do condomínio
    Given eu crio um apartamento com numero "301" no bloco de teste
    When eu atualizo o último apartamento criado com numero "301-A"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.numero" com valor "301-A"

  Scenario: Deletar apartamento retorna 204
    Given estou autenticado como admin do condomínio
    Given eu crio um apartamento com numero "401" no bloco de teste
    When eu deleto o último recurso criado em "/apartamentos"
    Then o status da resposta é 204

  Scenario: Criar apartamento com dados inválidos retorna 400
    Given estou autenticado como admin do condomínio
    When eu tento criar um apartamento sem numero
    Then o status da resposta é 400

  Scenario: ROLE_USER não pode criar apartamento (403)
    Given estou autenticado como usuário regular
    When eu crio um apartamento com numero "501" no bloco de teste
    Then o status da resposta é 403

  Scenario: Sem autenticação retorna 401
    Given não estou autenticado
    When eu faço GET para "/apartamentos"
    Then o status da resposta é 401
