@e2e
Feature: Gestão de Moradores

  Background:
    Given estou autenticado como admin do condomínio

  Scenario: Admin cria morador com sucesso
    When eu crio uma pessoa física com nome "João Silva" e cpf "123.456.789-09"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.nome" com valor "João Silva"
    And a resposta tem o campo "$.data.tipo" com valor "MORADOR"

  Scenario: Criar morador sem CPF retorna 400
    When eu tento criar uma pessoa física sem CPF
    Then o status da resposta é 400

  Scenario: POST não autenticado retorna 401
    Given não estou autenticado
    When eu crio uma pessoa física com nome "Teste" e cpf "123.456.789-09"
    Then o status da resposta é 401

  Scenario: POST sem permissão retorna 403
    Given estou autenticado como usuário regular
    When eu crio uma pessoa física com nome "Teste" e cpf "123.456.789-09"
    Then o status da resposta é 403

  Scenario: GET por id inexistente retorna 404
    When eu faço GET para "/pessoas/99999"
    Then o status da resposta é 404
