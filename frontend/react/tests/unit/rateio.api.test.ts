import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { gruposDespesaApi, coeficientesApi, rateioApi } from '@/api/financeiro/rateio.api'

const BASE = '/api'

function envelope<T>(data: T) {
  return { requestId: 'test-req', timestamp: new Date().toISOString(), data }
}

describe('gruposDespesaApi', () => {
  it('list() returns the unwrapped array from GET /grupos-despesa', async () => {
    const result = await gruposDespesaApi.list()
    expect(result).toEqual([
      {
        id: 1,
        nome: 'Manutenção Geral',
        tipoRateio: 'IGUALITARIO',
        escopo: 'TODOS',
        blocoId: null,
        planoContasId: null,
        parametrosJson: null,
      },
    ])
  })

  it('getById() returns the unwrapped grupo', async () => {
    server.use(
      http.get(`${BASE}/grupos-despesa/5`, () =>
        HttpResponse.json(
          envelope({
            id: 5,
            nome: 'Limpeza',
            tipoRateio: 'FRACAO_IDEAL',
            escopo: 'BLOCO',
            blocoId: 2,
            planoContasId: 3,
            parametrosJson: null,
          }),
        ),
      ),
    )

    const result = await gruposDespesaApi.getById(5)

    expect(result.id).toBe(5)
    expect(result.nome).toBe('Limpeza')
  })

  it('create() posts the body and returns the unwrapped grupo', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/grupos-despesa`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 2,
            nome: 'Novo Grupo',
            tipoRateio: 'IGUALITARIO',
            escopo: 'TODOS',
            blocoId: null,
            planoContasId: null,
            parametrosJson: null,
          }),
          { status: 201 },
        )
      }),
    )

    const body = { nome: 'Novo Grupo', tipoRateio: 'IGUALITARIO' as const, escopo: 'TODOS' as const }
    const result = await gruposDespesaApi.create(body)

    expect(receivedBody).toEqual(body)
    expect(result.id).toBe(2)
  })

  it('update() puts the body to /grupos-despesa/:id and returns the unwrapped grupo', async () => {
    let receivedBody: unknown
    server.use(
      http.put(`${BASE}/grupos-despesa/2`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 2,
            nome: 'Grupo Atualizado',
            tipoRateio: 'IGUALITARIO',
            escopo: 'TODOS',
            blocoId: null,
            planoContasId: null,
            parametrosJson: null,
          }),
        )
      }),
    )

    const result = await gruposDespesaApi.update(2, { nome: 'Grupo Atualizado' })

    expect(receivedBody).toEqual({ nome: 'Grupo Atualizado' })
    expect(result.nome).toBe('Grupo Atualizado')
  })

  it('remove() issues DELETE to /grupos-despesa/:id', async () => {
    let calledUrl: string | undefined
    server.use(
      http.delete(`${BASE}/grupos-despesa/:id`, ({ request }) => {
        calledUrl = request.url
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await gruposDespesaApi.remove(6)

    expect(calledUrl).toContain('/grupos-despesa/6')
  })

  it('propagates errors from getById()', async () => {
    server.use(
      http.get(`${BASE}/grupos-despesa/404`, () =>
        HttpResponse.json({ error: 'not_found' }, { status: 404 }),
      ),
    )

    await expect(gruposDespesaApi.getById(404)).rejects.toBeTruthy()
  })
})

describe('coeficientesApi', () => {
  it('listByGrupo() returns the unwrapped array', async () => {
    server.use(
      http.get(`${BASE}/grupos-despesa/1/coeficientes`, () =>
        HttpResponse.json(
          envelope([
            {
              id: 1,
              grupoDespesaId: 1,
              apartamentoId: 10,
              apartamentoNumero: '101',
              blocoNome: 'A',
              coeficiente: 0.05,
              vigenciaInicio: '2026-01-01',
              vigenciaFim: null,
            },
          ]),
        ),
      ),
    )

    const result = await coeficientesApi.listByGrupo(1)

    expect(result).toHaveLength(1)
    expect(result[0].apartamentoNumero).toBe('101')
  })

  it('create() posts the body to /grupos-despesa/:id/coeficientes', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/grupos-despesa/1/coeficientes`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 2,
            grupoDespesaId: 1,
            apartamentoId: 11,
            apartamentoNumero: '102',
            blocoNome: 'A',
            coeficiente: 0.04,
            vigenciaInicio: '2026-01-01',
            vigenciaFim: null,
          }),
          { status: 201 },
        )
      }),
    )

    const body = { apartamentoId: 11, coeficiente: 0.04 }
    const result = await coeficientesApi.create(1, body)

    expect(receivedBody).toEqual(body)
    expect(result.id).toBe(2)
  })

  it('update() puts the body to /grupos-despesa/:id/coeficientes/:coefId', async () => {
    let receivedBody: unknown
    server.use(
      http.put(`${BASE}/grupos-despesa/1/coeficientes/2`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 2,
            grupoDespesaId: 1,
            apartamentoId: 11,
            apartamentoNumero: '102',
            blocoNome: 'A',
            coeficiente: 0.06,
            vigenciaInicio: '2026-01-01',
            vigenciaFim: null,
          }),
        )
      }),
    )

    const result = await coeficientesApi.update(1, 2, { coeficiente: 0.06 })

    expect(receivedBody).toEqual({ coeficiente: 0.06 })
    expect(result.coeficiente).toBe(0.06)
  })

  it('remove() issues DELETE to /grupos-despesa/:id/coeficientes/:coefId', async () => {
    let calledUrl: string | undefined
    server.use(
      http.delete(`${BASE}/grupos-despesa/1/coeficientes/:coefId`, ({ request }) => {
        calledUrl = request.url
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await coeficientesApi.remove(1, 3)

    expect(calledUrl).toContain('/grupos-despesa/1/coeficientes/3')
  })

  it('propagates errors from listByGrupo()', async () => {
    server.use(
      http.get(`${BASE}/grupos-despesa/404/coeficientes`, () =>
        HttpResponse.json({ error: 'not_found' }, { status: 404 }),
      ),
    )

    await expect(coeficientesApi.listByGrupo(404)).rejects.toBeTruthy()
  })
})

describe('rateioApi', () => {
  it('simular() posts the body to /rateio/simular and returns the unwrapped simulation', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/rateio/simular`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            grupoNome: 'Manutenção Geral',
            totalDespesas: 1000,
            linhas: [
              {
                apartamentoId: 10,
                apartamentoNumero: '101',
                blocoNome: 'A',
                coeficiente: 0.05,
                valorRateado: 50,
              },
            ],
          }),
        )
      }),
    )

    const body = { grupoId: 1, ano: 2026 }
    const result = await rateioApi.simular(body)

    expect(receivedBody).toEqual(body)
    expect(result.grupoNome).toBe('Manutenção Geral')
    expect(result.linhas).toHaveLength(1)
  })

  it('ratearDespesa() posts to /rateio/despesa/:despesaId and returns the unwrapped execucao', async () => {
    server.use(
      http.post(`${BASE}/rateio/despesa/7`, () =>
        HttpResponse.json(
          envelope({
            id: 1,
            despesaId: 7,
            despesaDescricao: 'Conta de água',
            grupoDespesaId: 1,
            tipoExecucao: 'MANUAL',
            dataExecucao: '2026-01-01T00:00:00Z',
            despesaTotal: 1000,
            totalUnidades: 20,
            totalCotas: 1,
            status: 'SUCESSO',
            erroMensagem: null,
          }),
        ),
      ),
    )

    const result = await rateioApi.ratearDespesa(7)

    expect(result.despesaId).toBe(7)
    expect(result.status).toBe('SUCESSO')
  })

  it('recalcular() posts the body to /rateio/recalcular and returns the unwrapped resultado', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/rateio/recalcular`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({ total: 10, sucesso: 9, erro: 1, duracaoMs: 500 }),
        )
      }),
    )

    const body = { confirmar: true }
    const result = await rateioApi.recalcular(body)

    expect(receivedBody).toEqual(body)
    expect(result).toEqual({ total: 10, sucesso: 9, erro: 1, duracaoMs: 500 })
  })

  it('listarExecucoes() sends page/size params and returns the unwrapped page', async () => {
    let receivedUrl: URL | undefined
    server.use(
      http.get(`${BASE}/rateio/execucoes`, ({ request }) => {
        receivedUrl = new URL(request.url)
        return HttpResponse.json(
          envelope({
            content: [
              {
                id: 1,
                despesaId: 7,
                despesaDescricao: 'Conta de água',
                grupoDespesaId: 1,
                tipoExecucao: 'AUTOMATICO',
                dataExecucao: '2026-01-01T00:00:00Z',
                despesaTotal: 1000,
                totalUnidades: 20,
                totalCotas: 1,
                status: 'SUCESSO',
                erroMensagem: null,
              },
            ],
            totalElements: 1,
            totalPages: 1,
            size: 10,
            number: 0,
          }),
        )
      }),
    )

    const result = await rateioApi.listarExecucoes({ page: 0, size: 10 })

    expect(receivedUrl?.searchParams.get('page')).toBe('0')
    expect(receivedUrl?.searchParams.get('size')).toBe('10')
    expect(result.content).toHaveLength(1)
    expect(result.totalElements).toBe(1)
  })

  it('propagates errors from ratearDespesa()', async () => {
    server.use(
      http.post(`${BASE}/rateio/despesa/404`, () =>
        HttpResponse.json({ error: 'not_found' }, { status: 404 }),
      ),
    )

    await expect(rateioApi.ratearDespesa(404)).rejects.toBeTruthy()
  })
})
