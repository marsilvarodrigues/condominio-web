import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { orcamentoApi } from '@/api/financeiro/orcamento.api'

const BASE = '/api'

function envelope<T>(data: T) {
  return { requestId: 'test-req', timestamp: new Date().toISOString(), data }
}

describe('orcamentoApi', () => {
  it('listByAno() sends ano as a query param and returns the unwrapped array', async () => {
    let receivedUrl: URL | undefined
    server.use(
      http.get(`${BASE}/orcamentos`, ({ request }) => {
        receivedUrl = new URL(request.url)
        return HttpResponse.json(
          envelope([
            {
              id: 1,
              ano: 2026,
              contaNome: 'Manutenção',
              contaId: 1,
              grupoDespesaId: null,
              grupoDespesaNome: null,
              valorOrcado: 1000,
              valorRealizado: 800,
              statusRateio: 'PENDENTE',
            },
          ]),
        )
      }),
    )

    const result = await orcamentoApi.listByAno(2026)

    expect(receivedUrl?.searchParams.get('ano')).toBe('2026')
    expect(result).toHaveLength(1)
    expect(result[0].contaNome).toBe('Manutenção')
  })

  it('addItem() posts the body to /orcamentos/:ano/itens and returns the unwrapped item', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/orcamentos/2026/itens`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 2,
            ano: 2026,
            contaNome: 'Água',
            contaId: 5,
            grupoDespesaId: null,
            grupoDespesaNome: null,
            valorOrcado: 500,
            valorRealizado: 0,
            statusRateio: null,
          }),
          { status: 201 },
        )
      }),
    )

    const body = { contaId: 5, valorOrcado: 500 }
    const result = await orcamentoApi.addItem(2026, body)

    expect(receivedBody).toEqual(body)
    expect(result.id).toBe(2)
    expect(result.contaNome).toBe('Água')
  })

  it('removeItem() issues DELETE to /orcamentos/:ano/itens/:itemId', async () => {
    let calledUrl: string | undefined
    server.use(
      http.delete(`${BASE}/orcamentos/2026/itens/2`, ({ request }) => {
        calledUrl = request.url
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await orcamentoApi.removeItem(2026, 2)

    expect(calledUrl).toContain('/orcamentos/2026/itens/2')
  })

  it('recalcularRateio() posts to /orcamentos/:ano/recalcular', async () => {
    let called = false
    server.use(
      http.post(`${BASE}/orcamentos/2026/recalcular`, () => {
        called = true
        return HttpResponse.json(envelope({ ok: true }))
      }),
    )

    await orcamentoApi.recalcularRateio(2026)

    expect(called).toBe(true)
  })

  it('propagates errors from listByAno()', async () => {
    server.use(
      http.get(`${BASE}/orcamentos`, () =>
        HttpResponse.json({ error: 'internal_error' }, { status: 500 }),
      ),
    )

    await expect(orcamentoApi.listByAno(2026)).rejects.toBeTruthy()
  })
})
