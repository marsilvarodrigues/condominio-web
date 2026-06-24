import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { conciliacaoApi } from '@/api/financeiro/conciliacao.api'

const BASE = '/api'

function envelope<T>(data: T) {
  return { requestId: 'test-req', timestamp: new Date().toISOString(), data }
}

describe('conciliacaoApi', () => {
  it('listarLancamentos() requests with contaBancariaId and size params, unwraps content', async () => {
    let receivedUrl: URL | undefined
    server.use(
      http.get(`${BASE}/lancamentos-bancarios`, ({ request }) => {
        receivedUrl = new URL(request.url)
        return HttpResponse.json(
          envelope({
            content: [
              {
                id: 1,
                contaBancariaId: 5,
                contaBancariaDescricao: 'Conta principal',
                dataLancamento: '2026-02-01',
                valor: 300,
                tipo: 'DEBITO',
                descricao: 'Pagamento fornecedor',
                origem: 'DESPESA_ORDINARIA',
                referenciaId: null,
                status: 'PENDENTE',
                createdAt: '2026-02-01T00:00:00Z',
                updatedAt: '2026-02-01T00:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
            size: 200,
            number: 0,
          }),
        )
      }),
    )

    const result = await conciliacaoApi.listarLancamentos(5)

    expect(receivedUrl?.searchParams.get('contaBancariaId')).toBe('5')
    expect(receivedUrl?.searchParams.get('size')).toBe('200')
    expect(result).toHaveLength(1)
    expect(result[0].descricao).toBe('Pagamento fornecedor')
  })

  it('associar() posts itemOrcamentoId to /conciliacao/associacao/:itemExtratoId', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/conciliacao/associacao/11`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(envelope({ ok: true }))
      }),
    )

    await conciliacaoApi.associar(11, 22)

    expect(receivedBody).toEqual({ itemOrcamentoId: 22 })
  })

  it('desassociar() sends justificativa in the DELETE body to /conciliacao/associacao/:itemExtratoId', async () => {
    let receivedBody: unknown
    server.use(
      http.delete(`${BASE}/conciliacao/associacao/11`, async ({ request }) => {
        receivedBody = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await conciliacaoApi.desassociar(11, 'Lançamento duplicado')

    expect(receivedBody).toEqual({ justificativa: 'Lançamento duplicado' })
  })

  it('sugestoes() returns the unwrapped data array', async () => {
    server.use(
      http.get(`${BASE}/conciliacao/associacao/sugestoes/11`, () =>
        HttpResponse.json(envelope([{ id: 1 }, { id: 2 }])),
      ),
    )

    const result = await conciliacaoApi.sugestoes(11)

    expect(result).toEqual([{ id: 1 }, { id: 2 }])
  })

  it('propagates errors from associar()', async () => {
    server.use(
      http.post(`${BASE}/conciliacao/associacao/99`, () =>
        HttpResponse.json({ error: 'not_found' }, { status: 404 }),
      ),
    )

    await expect(conciliacaoApi.associar(99, 1)).rejects.toBeTruthy()
  })
})
