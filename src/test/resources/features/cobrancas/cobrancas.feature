@e2e
Feature: Módulo de Cobrança

  Background:
    Given estou autenticado como admin do condomínio
    And existe uma execução de rateio com cotas para 3 apartamentos

  Scenario: Admin gera cobranças com sucesso
    When eu gero cobranças para a execução de rateio com vencimento em 30 dias
    Then o status da resposta é 201

  Scenario: Gerar cobranças é idempotente
    Given cobranças já foram geradas para a execução de rateio
    When eu gero cobranças novamente para a mesma execução
    Then o status da resposta é 201

  Scenario: Admin cancela uma cobrança
    Given existe uma cobrança com status PENDENTE
    When eu cancelo a cobrança com motivo "Solicitação do morador"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.status" com valor "CANCELADA"

  Scenario: Admin reenvia e-mail de cobrança
    Given existe uma cobrança com status PENDENTE
    When eu reenvio o e-mail da cobrança
    Then o status da resposta é 204

  Scenario: POST sem autenticação retorna 401
    Given não estou autenticado
    When eu tento gerar cobranças
    Then o status da resposta é 401

  Scenario: POST sem permissão retorna 403
    Given estou autenticado como usuário regular
    When eu tento gerar cobranças
    Then o status da resposta é 403

  Scenario: GET cobrança inexistente retorna 404
    When eu faço GET para "/cobrancas/99999"
    Then o status da resposta é 404
