@e2e
Feature: Gestão de Proprietários

  Background:
    Given estou autenticado como admin do condomínio

  Scenario: Admin cria proprietário pessoa física com sucesso
    When eu crio um proprietário pessoa física com nome "Carlos Dono" e cpf "111.222.333-44"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.tipo" com valor "PROP_PF"

  Scenario: Admin associa proprietário ao apartamento com sucesso
    Given eu criei um proprietário de teste
    When eu associo o proprietário de teste ao apartamento de teste
    Then o status da resposta é 200

  Scenario: GET proprietários do apartamento retorna lista
    Given eu criei um proprietário de teste
    And eu associo o proprietário de teste ao apartamento de teste
    When eu faço GET dos proprietários do apartamento de teste
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: POST sem autenticação retorna 401
    Given não estou autenticado
    When eu crio um proprietário pessoa física com nome "Teste" e cpf "999.888.777-66"
    Then o status da resposta é 401

  Scenario: POST sem permissão retorna 403
    Given estou autenticado como usuário regular
    When eu crio um proprietário pessoa física com nome "Teste" e cpf "999.888.777-66"
    Then o status da resposta é 403

  Scenario: GET por id inexistente retorna 404
    When eu faço GET para "/proprietarios/99999"
    Then o status da resposta é 404
