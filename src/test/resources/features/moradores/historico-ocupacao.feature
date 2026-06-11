# language: pt
@e2e
Feature: Histórico de Ocupação do Apartamento

  Background:
    Given estou autenticado como admin do condomínio
    And existe um apartamento de teste com um morador ativo

  Scenario: Remover morador grava histórico automaticamente
    When eu removo o morador do apartamento de teste
    Then o status da resposta é 200
    And o histórico do apartamento de teste contém 1 registro

  Scenario: Substituir morador grava histórico do anterior
    Given o apartamento de teste já tem um morador ativo
    When eu associo um segundo morador ao apartamento de teste
    Then o morador anterior aparece no histórico com data_saida preenchida
    And o histórico do apartamento de teste contém 1 registro

  Scenario: Admin visualiza histórico do apartamento
    Given existem registros no histórico do apartamento de teste
    When eu faço GET para o histórico de ocupação do apartamento de teste
    Then o status da resposta é 200
    And a resposta contém itens em "$.data"

  Scenario: GET sem permissão retorna 403
    Given estou autenticado como usuário regular
    When eu faço GET para o histórico de ocupação do apartamento de teste
    Then o status da resposta é 403

  Scenario: GET sem autenticação retorna 401
    Given não estou autenticado
    When eu faço GET para o histórico de ocupação do apartamento de teste
    Then o status da resposta é 401

  Scenario: Histórico de apartamento sem moradores anteriores retorna lista vazia
    Given o apartamento de teste não tem histórico de ocupação
    When eu faço GET para o histórico de ocupação do apartamento de teste
    Then o status da resposta é 200
    And a resposta tem lista vazia em "$.data"

  Scenario: Remover morador sem roles adicionais bloqueia o acesso ao sistema
    Given existe um morador com somente ROLE_MORADOR no apartamento de teste
    When eu removo o morador do apartamento de teste
    Then o morador está no histórico de ocupação do apartamento de teste
    And o morador removido tem acesso bloqueado no sistema

  Scenario: Remover morador que também é proprietário não bloqueia o acesso
    Given existe um morador que também é proprietário no apartamento de teste
    When eu removo o morador do apartamento de teste
    Then o morador está no histórico de ocupação do apartamento de teste
    And o morador removido mantém acesso ao sistema
