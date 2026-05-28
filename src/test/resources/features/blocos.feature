Feature: Gerenciamento de Blocos

  # Nota: criação de Blocos requer que o JWT do admin contenha condominio_id.
  # O admin do condomínio (admin@bdd.com) tem condominioId configurado.
  # O master (master@bdd.com) não tem condominioId — suas requisições de criação
  # resultarão em erro (sem TenantContext o @PrePersist não seta a FK condominio).
  # Por isso, os cenários de escrita usam "admin do condomínio".

  Scenario: Listar blocos como master retorna lista com bloco de teste
    Given estou autenticado como master
    When eu faço GET para "/blocos"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: Filtrar blocos por letra
    Given estou autenticado como master
    When eu faço GET para "/blocos?bloco=A"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data[0].bloco" com valor "A"

  Scenario: Buscar bloco por ID
    Given estou autenticado como master
    When eu busco o bloco de teste por ID
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.bloco" com valor "A"

  Scenario: Buscar bloco inexistente retorna 404
    Given estou autenticado como master
    When eu faço GET para "/blocos/99999"
    Then o status da resposta é 404

  Scenario: Admin do condomínio cria bloco retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um bloco com numero 2 e letra "B"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.bloco" com valor "B"

  Scenario: Criar bloco com dados inválidos retorna 400
    Given estou autenticado como admin do condomínio
    When eu tento criar um bloco sem numero
    Then o status da resposta é 400

  Scenario: Atualizar bloco retorna 200
    Given estou autenticado como admin do condomínio
    When eu atualizo o bloco de teste com letra "A-Updated"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.bloco" com valor "A-Updated"

  Scenario: Deletar bloco retorna 204
    Given estou autenticado como admin do condomínio
    Given eu crio um bloco com numero 9 e letra "Z"
    When eu deleto o último recurso criado em "/blocos"
    Then o status da resposta é 204

  Scenario: ROLE_USER não pode criar bloco (403)
    Given estou autenticado como usuário regular
    When eu crio um bloco com numero 5 e letra "X"
    Then o status da resposta é 403

  Scenario: Sem autenticação retorna 401
    Given não estou autenticado
    When eu faço GET para "/blocos"
    Then o status da resposta é 401
