# language: pt

Funcionalidade: Rateio de Despesas

  Contexto:
    Dado que o admin está autenticado

  Cenário: Admin cria grupo de despesa com sucesso
    Quando o admin envia POST /grupos-despesa com nome "Condomínio Geral" e tipoRateio "IGUALITARIO"
    Então a resposta deve ter status 201
    E o campo "nome" deve ser "Condomínio Geral"

  Cenário: Dados inválidos ao criar grupo de despesa retornam 400
    Quando o admin envia POST /grupos-despesa sem nome
    Então a resposta deve ter status 400

  Cenário: Não autenticado ao criar grupo de despesa recebe 401
    Quando um utilizador não autenticado envia POST /grupos-despesa
    Então a resposta deve ter status 401

  Cenário: Sem permissão ao criar grupo de despesa recebe 403
    Quando um utilizador com role USER envia POST /grupos-despesa
    Então a resposta deve ter status 403

  Cenário: Grupo inexistente retorna 404
    Quando o admin busca GET /grupos-despesa/999999
    Então a resposta deve ter status 404

  Cenário: Scheduler rateia despesas pendentes automaticamente
    Dado que existem 3 despesas com rateioStatus "PENDENTE" para o grupo IGUALITARIO
    E cada grupo tem coeficientes configurados para 4 unidades
    Quando o scheduler de rateio executa
    Então todas as 3 despesas devem ter rateioStatus "RATEADA"
    E devem existir 3 registos de RateioExecucao com tipoExecucao "AUTOMATICO" e status "SUCESSO"

  Cenário: Recálculo forçado substitui rateios anteriores
    Dado que existem despesas com rateioStatus "RATEADA"
    Quando o admin envia POST /rateio/recalcular com confirmar=true
    Então a resposta deve ter status 200
    E o campo "total" deve ser maior que 0
    E os registos de RateioExecucao gerados devem ter tipoExecucao "RECALCULO"

  Cenário: Recalcular sem confirmar retorna 400
    Quando o admin envia POST /rateio/recalcular com confirmar=false
    Então a resposta deve ter status 400

  Cenário: Centavo de arredondamento é distribuído corretamente na simulação IGUALITARIO
    Dado que existe um grupo IGUALITARIO com 3 unidades
    Quando o admin simula rateio de R$ 100,00 para esse grupo
    Então a soma das cotas deve ser exatamente R$ 100,00

  Cenário: Simulação retorna cotas por unidade
    Dado que existe um grupo FRACAO_IDEAL com coeficientes configurados
    Quando o admin envia POST /rateio/simular com despesaTotal 1000.00
    Então a resposta deve ter status 200
    E a lista de cotas não deve estar vazia
    E a somaCotas deve ser igual ao despesaTotal
