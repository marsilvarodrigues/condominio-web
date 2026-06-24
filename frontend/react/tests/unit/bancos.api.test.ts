import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { bancosApi, contasBancariasApi } from '@/api/financeiro/bancos.api'

const BASE = '/api'

function envelope<T>(data: T) {
  return { requestId: 'test-req', timestamp: new Date().toISOString(), data }
}

describe('bancosApi', () => {
  it('list() returns the unwrapped array from GET /bancos', async () => {
    const result = await bancosApi.list()
    expect(result).toEqual([{ id: 1, codigo: '341', nome: 'Itaú', ativo: true }])
  })

  it('create() posts the body and returns the unwrapped banco', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/bancos`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({ id: 5, codigo: '001', nome: 'Banco do Brasil', ativo: true }),
          { status: 201 },
        )
      }),
    )

    const result = await bancosApi.create({ codigo: '001', nome: 'Banco do Brasil' })

    expect(receivedBody).toEqual({ codigo: '001', nome: 'Banco do Brasil' })
    expect(result).toEqual({ id: 5, codigo: '001', nome: 'Banco do Brasil', ativo: true })
  })

  it('remove() issues DELETE to /bancos/:id', async () => {
    let calledUrl: string | undefined
    server.use(
      http.delete(`${BASE}/bancos/:id`, ({ params, request }) => {
        calledUrl = request.url
        expect(params.id).toBe('7')
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await bancosApi.remove(7)

    expect(calledUrl).toContain('/bancos/7')
  })

  it('propagates errors from the server', async () => {
    server.use(
      http.get(`${BASE}/bancos`, () =>
        HttpResponse.json({ error: 'internal_error' }, { status: 500 }),
      ),
    )

    await expect(bancosApi.list()).rejects.toBeTruthy()
  })
})

describe('contasBancariasApi', () => {
  it('list() unwraps the paginated content array from GET /contas-bancarias', async () => {
    server.use(
      http.get(`${BASE}/contas-bancarias`, () =>
        HttpResponse.json(
          envelope({
            content: [
              {
                id: 1,
                agencia: '0001',
                conta: '123456-7',
                tipo: 'CORRENTE',
                descricao: 'Conta principal',
                bancoId: 1,
                bancoNome: 'Itaú',
                saldo: 1000,
              },
            ],
            totalElements: 1,
            totalPages: 1,
            size: 20,
            number: 0,
          }),
        ),
      ),
    )

    const result = await contasBancariasApi.list()

    expect(result).toEqual([
      {
        id: 1,
        agencia: '0001',
        conta: '123456-7',
        tipo: 'CORRENTE',
        descricao: 'Conta principal',
        bancoId: 1,
        bancoNome: 'Itaú',
        saldo: 1000,
      },
    ])
  })

  it('getById() returns the unwrapped conta', async () => {
    server.use(
      http.get(`${BASE}/contas-bancarias/3`, () =>
        HttpResponse.json(
          envelope({
            id: 3,
            agencia: '0002',
            conta: '654321-0',
            tipo: 'POUPANCA',
            descricao: 'Poupança',
            bancoId: 2,
            bancoNome: 'Bradesco',
            saldo: 500,
          }),
        ),
      ),
    )

    const result = await contasBancariasApi.getById(3)

    expect(result.id).toBe(3)
    expect(result.tipo).toBe('POUPANCA')
  })

  it('create() posts the body and returns the unwrapped conta', async () => {
    let receivedBody: unknown
    server.use(
      http.post(`${BASE}/contas-bancarias`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 10,
            agencia: '0003',
            conta: '111111-1',
            tipo: 'CORRENTE',
            descricao: 'Nova conta',
            bancoId: 1,
            bancoNome: 'Itaú',
            saldo: 0,
          }),
          { status: 201 },
        )
      }),
    )

    const body = { agencia: '0003', conta: '111111-1', tipo: 'CORRENTE' as const, bancoId: 1 }
    const result = await contasBancariasApi.create(body)

    expect(receivedBody).toEqual(body)
    expect(result.id).toBe(10)
  })

  it('update() puts the body to /contas-bancarias/:id and returns the unwrapped conta', async () => {
    let receivedBody: unknown
    server.use(
      http.put(`${BASE}/contas-bancarias/4`, async ({ request }) => {
        receivedBody = await request.json()
        return HttpResponse.json(
          envelope({
            id: 4,
            agencia: '0004',
            conta: '222222-2',
            tipo: 'INVESTIMENTO',
            descricao: 'Atualizada',
            bancoId: 1,
            bancoNome: 'Itaú',
            saldo: 200,
          }),
        )
      }),
    )

    const result = await contasBancariasApi.update(4, { descricao: 'Atualizada' })

    expect(receivedBody).toEqual({ descricao: 'Atualizada' })
    expect(result.descricao).toBe('Atualizada')
  })

  it('lancamentos() requests with contaBancariaId and size params, unwraps content', async () => {
    let receivedUrl: URL | undefined
    server.use(
      http.get(`${BASE}/lancamentos-bancarios`, ({ request }) => {
        receivedUrl = new URL(request.url)
        return HttpResponse.json(
          envelope({
            content: [
              {
                id: 1,
                contaBancariaId: 9,
                contaBancariaDescricao: 'Conta principal',
                dataLancamento: '2026-01-01',
                valor: 150,
                tipo: 'CREDITO',
                descricao: 'Pagamento',
                origem: 'COTA_CONDOMINIO',
                referenciaId: null,
                status: 'PENDENTE',
                createdAt: '2026-01-01T00:00:00Z',
                updatedAt: '2026-01-01T00:00:00Z',
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

    const result = await contasBancariasApi.lancamentos(9)

    expect(receivedUrl?.searchParams.get('contaBancariaId')).toBe('9')
    expect(receivedUrl?.searchParams.get('size')).toBe('200')
    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(1)
  })

  it('remove() issues DELETE to /contas-bancarias/:id', async () => {
    let calledUrl: string | undefined
    server.use(
      http.delete(`${BASE}/contas-bancarias/:id`, ({ request }) => {
        calledUrl = request.url
        return new HttpResponse(null, { status: 204 })
      }),
    )

    await contasBancariasApi.remove(8)

    expect(calledUrl).toContain('/contas-bancarias/8')
  })

  it('propagates errors from the server', async () => {
    server.use(
      http.get(`${BASE}/contas-bancarias/99`, () =>
        HttpResponse.json({ error: 'not_found' }, { status: 404 }),
      ),
    )

    await expect(contasBancariasApi.getById(99)).rejects.toBeTruthy()
  })
})
