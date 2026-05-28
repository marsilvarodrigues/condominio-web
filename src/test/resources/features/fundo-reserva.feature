Feature: Gerenciamento de Fundo de Reserva

  Scenario: Admin cria fundo de reserva com sucesso retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.contaBancariaDestino" com valor "001-1"

  Scenario: Dados inválidos retornam 400
    Given estou autenticado como admin do condomínio
    When eu tento criar um fundo de reserva com percentual inválido
    Then o status da resposta é 400

  Scenario: Não autenticado recebe 401
    Given não estou autenticado
    When eu faço GET para "/fundo-reserva"
    Then o status da resposta é 401

  Scenario: Sem permissão recebe 403
    Given estou autenticado como usuário regular
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    Then o status da resposta é 403

  Scenario: Admin obtém fundo de reserva retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    And eu faço GET para "/fundo-reserva"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.contaBancariaDestino" com valor "001-1"

  Scenario: Admin credita no fundo de reserva retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    And eu credito "1000.00" no fundo de reserva
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.tipo" com valor "CREDITO"

  Scenario: Admin debita do fundo de reserva retorna 201
    Given estou autenticado como admin do condomínio
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    And eu credito "1000.00" no fundo de reserva
    And eu debito "500.00" do fundo de reserva com justificativa "Manutenção"
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.tipo" com valor "DEBITO"

  Scenario: Débito maior que saldo retorna 409
    Given estou autenticado como admin do condomínio
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    And eu debito "99999.00" do fundo de reserva com justificativa "Muito"
    Then o status da resposta é 409

  Scenario: Admin lista movimentações retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    And eu faço GET para "/fundo-reserva/movimentacoes"
    Then o status da resposta é 200

  Scenario: Admin atualiza fundo de reserva retorna 200
    Given estou autenticado como admin do condomínio
    When eu crio um fundo de reserva com percentual "10.00" e conta "001-1"
    And eu atualizo o fundo de reserva com percentual "15.00" e conta "002-2"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.contaBancariaDestino" com valor "002-2"
