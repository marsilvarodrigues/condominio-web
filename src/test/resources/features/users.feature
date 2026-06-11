Feature: Gerenciamento de Usuários

  Background:
    Given estou autenticado como master

  Scenario: Listar todos os usuários retorna lista
    When eu faço GET para "/users"
    Then o status da resposta é 200
    And a resposta contém uma lista em "$.data.content"

  Scenario: Admin cria usuário com condominioIds retorna 201
    When eu crio um usuário com email "novo@bdd.com" no condomínio de teste
    Then o status da resposta é 201
    And a resposta tem o campo "$.data.email" com valor "novo@bdd.com"

  Scenario: Buscar usuário por ID retorna 200
    Given eu crio um usuário com email "busca@bdd.com" no condomínio de teste
    When eu busco o último recurso criado em "/users"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.email" com valor "busca@bdd.com"

  Scenario: Buscar usuário inexistente retorna 404
    When eu faço GET para "/users/99999"
    Then o status da resposta é 404

  Scenario: Atualizar usuário retorna 200 com dados atualizados
    Given eu crio um usuário com email "update@bdd.com" no condomínio de teste
    When eu atualizo o último usuário criado com nome "Nome Atualizado"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.name" com valor "Nome Atualizado"

  Scenario: Deletar usuário retorna 204
    Given eu crio um usuário com email "delete@bdd.com" no condomínio de teste
    When eu deleto o último recurso criado em "/users"
    Then o status da resposta é 204

  Scenario: Criar usuário com email já em uso retorna 400
    Given eu crio um usuário com email "duplicado@bdd.com" no condomínio de teste
    When eu crio um usuário com email "duplicado@bdd.com" no condomínio de teste
    Then o status da resposta é 400

  Scenario: Criar usuário com condomínio inexistente retorna 400
    When eu tento criar um usuário com condominioId inválido com email "sem-cond@bdd.com"
    Then o status da resposta é 400

  Scenario: ROLE_USER não pode criar usuário (403)
    Given estou autenticado como usuário regular
    When eu crio um usuário com email "proibido@bdd.com" no condomínio de teste
    Then o status da resposta é 403

  Scenario: Usuário regular não pode atualizar dados de outro usuário (403)
    Given estou autenticado como usuário regular
    When eu tento atualizar o usuário master com nome "Hackeado"
    Then o status da resposta é 403

  Scenario: Usuário regular pode atualizar seus próprios dados (200)
    Given estou autenticado como usuário regular
    When eu atualizo meus próprios dados com nome "Meu Novo Nome"
    Then o status da resposta é 200
    And a resposta tem o campo "$.data.name" com valor "Meu Novo Nome"

  Scenario: Sem autenticação retorna 401
    Given não estou autenticado
    When eu faço GET para "/users"
    Then o status da resposta é 401

  Scenario: Usuário altera senha com sucesso retorna 204
    When eu altero minha senha de "Master@123" para "NovaSenh@456" confirmando "NovaSenh@456"
    Then o status da resposta é 204

  Scenario: Senha atual inválida retorna 400
    When eu altero minha senha de "SenhaErrada" para "NovaSenh@456" confirmando "NovaSenh@456"
    Then o status da resposta é 400
    And a resposta tem o campo "$.data.message" com valor "Senha atual inválida"

  Scenario: Nova senha diferente da confirmação retorna 400
    When eu altero minha senha de "Master@123" para "NovaSenh@456" confirmando "DiferentE@789"
    Then o status da resposta é 400
    And a resposta tem o campo "$.data.message" com valor "Nova senha não confere com a confirmação"

  Scenario: Senha anteriormente utilizada retorna 400
    When eu altero minha senha de "Master@123" para "NovaSenh@456" confirmando "NovaSenh@456"
    And eu altero minha senha de "NovaSenh@456" para "Master@123" confirmando "Master@123"
    Then o status da resposta é 400
    And a resposta tem o campo "$.data.message" com valor "Senha já utilizada anteriormente"

  Scenario: Usuário regular não pode alterar senha de outro usuário retorna 403
    Given estou autenticado como usuário regular
    When eu altero minha senha de "Master@123" para "NovaSenh@456" confirmando "NovaSenh@456"
    Then o status da resposta é 403

  Scenario: Troca de senha sem autenticação retorna 401
    Given não estou autenticado
    When eu altero minha senha de "Master@123" para "NovaSenh@456" confirmando "NovaSenh@456"
    Then o status da resposta é 401
