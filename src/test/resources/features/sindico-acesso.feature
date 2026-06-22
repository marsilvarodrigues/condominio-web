Feature: Acesso do Síndico — escrita nos domínios operacionais

  # O síndico tem ROLE_SINDICO e deve poder executar operações de escrita
  # nos mesmos recursos que o ADMIN, com exceção de deletar condomínios
  # (mantido exclusivo do ADMIN) e gerenciar usuários.

  Scenario: Síndico lista blocos com sucesso (200)
    Given estou autenticado como síndico
    When eu faço GET para "/blocos"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data"

  Scenario: Síndico cria bloco com sucesso (201)
    Given estou autenticado como síndico
    When eu crio um bloco com numero 7 e letra "S"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.bloco" com valor "S"

  Scenario: Síndico atualiza bloco com sucesso (200)
    Given estou autenticado como síndico
    When eu atualizo o bloco de teste com letra "S-Updated"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.bloco" com valor "S-Updated"

  Scenario: Síndico lista apartamentos com sucesso (200)
    Given estou autenticado como síndico
    When eu faço GET para "/apartamentos"
    Then o status da resposta é 200

  Scenario: Síndico lista plano de contas com sucesso (200)
    Given estou autenticado como síndico
    When eu faço GET para "/plano-contas"
    Then o status da resposta é 200

  Scenario: Síndico cria plano de contas com sucesso (201)
    Given estou autenticado como síndico
    When eu crio um plano de contas com codigo "3.1.01" e descricao "Taxa Condominial" do tipo "RECEITA"
    Then o status da resposta é 201

  Scenario: Síndico lista cobranças com sucesso (200)
    Given estou autenticado como síndico
    When eu faço GET para "/cobrancas"
    Then o status da resposta é 200

  Scenario: Usuário regular não pode criar bloco (403)
    Given estou autenticado como usuário regular
    When eu crio um bloco com numero 5 e letra "X"
    Then o status da resposta é 403

  Scenario: Não autenticado recebe 401 em operações de síndico
    Given não estou autenticado
    When eu faço GET para "/blocos"
    Then o status da resposta é 401
