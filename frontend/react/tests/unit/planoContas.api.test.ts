import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { planoContasApi } from '@/api/financeiro/planoContas.api'

const BASE = '/api'

function envelope<T>(data: T) {
  return { requestId: 'test-req', timestamp: new Date().toISOString(), data }
}

describe('planoContasApi', () => {
  it('list() returns the unwrapped array from GET /plano-contas', async () => {
    const result = await planoContasApi.list()
    expect(result).toEqual([])
  })

  it('list() returns non-empty data when overridden', async () => {
    server.use(
      http.get(`${BASE}/plano-contas`, () =>
        HttpResponse.json(
          envelope([
            {
              id: 1,
              codigo: '1.1',
              nome: 'Receitas',
              tipo: 'RECEITA',
              contaParenteId: null,
              contaParenteNome: null,
              nivel: 1,
            },
          ]),
        ),
      ),
    )

    const result = await planoContasApi.list()

    expect(result).toHaveLength(1)
    expect(result[0].nome).toBe('Receitas')
  })

  it('getById() returns the unwrapped conta', async () => {
    server.use(
      http.get(`${BASE}/plano-contas/3`, () =>
        HttpResponse.json(
          envelope({
            id: 3,
            codigo: '1.1.1',
            nome: 'Cotas',
            tipo: 'RECEITA',
            contaParenteId: 1,
            contaParenteNome: 'Receitas',
            nivel: 2,
          }),
        ),
      ),
    )

    const result = await planoContasApi.getById(3)

    expect(result.id).toBe(3)
    expect(result.nome).toBe('Cotas')
  })

  it('create() posts the body and returns the unwrapped conta', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/plano-contas`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 4,
            codigo: '2.1',
            nome: 'Despesas Gerais',
            tipo: 'DESPESA',
            contaParenteId: null,
            contaParenteNome: null,
            nivel: 1,
          }),
          { status: 201 },
        )
      }),
    )

    const body = { codigo: '2.1', nome: 'Despesas Gerais', tipo: 'DESPESA' as const }
    const result = await planoContasApi.create(body)

    expect(receivedBody).toEqual(body)
    expect(result.id).toBe(4)
  })

  it('update() puts the body to /plano-contas/:id and returns the unwrapped conta', async () => {
    let receivedBody: unknown
    server.use(
      http.put(`${BASE}/plano-contas/4`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 4,
            codigo: '2.1',
            nome: 'Despesas Gerais Atualizadas',
            tipo: 'DESPESA',
            contaParenteId: null,
            contaParenteNome: null,
            nivel: 1,
          }),
        )
      }),
    )

    const result = await planoContasApi.update(4, { nome: 'Despesas Gerais Atualizadas' })

    expect(receivedBody).toEqual({ nome: 'Despesas Gerais Atualizadas' })
    expect(result.nome).toBe('Despesas Gerais Atualizadas')
  })

  it('remove() issues DELETE to /plano-contas/:id', async () => {
    let calledUrl: string | undefined
    server.use(
      http.delete(`${BASE}/plano-contas/:id`, ({ request }) => {
        calledUrl = request.url
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await planoContasApi.remove(9)

    expect(calledUrl).toContain('/plano-contas/9')
  })

  it('propagates errors from getById()', async () => {
    server.use(
      http.get(`${BASE}/plano-contas/404`, () =>
        HttpResponse.json({ error: 'not_found' }, { status: 404 }),
      ),
    )

    await expect(planoContasApi.getById(404)).rejects.toBeTruthy()
  })
})
