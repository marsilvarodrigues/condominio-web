Feature: Módulo de Auditoria

  Background:
    Given estou autenticado como admin do condomínio

  Scenario: Admin consulta histórico de apartamento com sucesso
    When eu consulto o histórico de auditoria de "apartamentos" do apartamento de teste
    Then o status da resposta é 200
    And a resposta contém uma lista com pelo menos 1 item em "$.data"

  Scenario: Entidade inválida retorna 400
    When eu faço GET para "/auditoria/entidade-invalida/1"
    Then o status da resposta é 400

  Scenario: Não autenticado recebe 401
    Given não estou autenticado
    When eu consulto o histórico de auditoria de "apartamentos" do apartamento de teste
    Then o status da resposta é 401

  Scenario: Usuário regular sem permissão recebe 403
    Given estou autenticado como usuário regular
    When eu consulto o histórico de auditoria de "apartamentos" do apartamento de teste
    Then o status da resposta é 403
