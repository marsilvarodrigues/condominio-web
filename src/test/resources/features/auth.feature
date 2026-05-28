Feature: Autenticação via JWT

  Scenario: Login com credenciais válidas retorna accessToken e refreshToken
    When eu faço login com "master@bdd.com" e senha "Master@123"
    Then o status da resposta é 200
    And a resposta contém um accessToken
    And a resposta contém um refreshToken

  Scenario: Login com credenciais inválidas retorna 401
    When eu faço login com "naoexiste@bdd.com" e senha "errada"
    Then o status da resposta é 401

  Scenario: Requisição sem token retorna 401
    Given não estou autenticado
    When eu faço GET para "/estados"
    Then o status da resposta é 401

  Scenario: Refresh de token retorna novo accessToken
    Given estou autenticado como master
    When eu faço refresh do token
    Then o status da resposta é 200
    And a resposta contém um accessToken

  Scenario: Logout retorna 204 e invalida o token
    Given estou autenticado como master
    When eu faço logout
    Then o status da resposta é 204
