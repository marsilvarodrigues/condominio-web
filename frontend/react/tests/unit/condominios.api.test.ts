import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { condominiosApi } from '@/api/condominios.api'
import { server } from './mocks/server'

const enderecoDTO = {
  logradouro: 'Rua Teste, 123',
  cep: '01310100',
  cidade: 'São Paulo',
  estado: { id: 35, nome: 'São Paulo', uf: 'SP' },
}

describe('condominiosApi', () => {
  describe('list', () => {
    it('gets /condominios via apiClient and unwraps data (uses default handler)', async () => {
      const result = await condominiosApi.list()

      expect(result).toEqual([
        {
          id: 1,
          nome: 'Condomínio Teste',
          cnpj: '12345678000195',
          email: 'admin@condo.com',
          endereco: {
            logradouro: 'Rua Teste, 123',
            cep: '01310100',
            cidade: 'São Paulo',
            estadoId: 35,
            estadoUf: 'SP',
          },
        },
      ])
    })

    it('rejects on server error', async () => {
      server.use(http.get('/api/condominios', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(condominiosApi.list()).rejects.toBeTruthy()
    })
  })

  describe('listForSelector', () => {
    it('gets /condominios via apiClientGlobal (no X-Condominio-Id) and unwraps data', async () => {
      let capturedHeader: string | null = null
      server.use(
        http.get('/api/condominios', ({ request }) => {
          capturedHeader = request.headers.get('X-Condominio-Id')
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: [{ id: 2, nome: 'Outro Condo', cnpj: '00000000000191', email: 'a@b.com', endereco: enderecoDTO }],
          })
        }),
      )

      const result = await condominiosApi.listForSelector()

      expect(capturedHeader).toBeNull()
      expect(result).toEqual([
        { id: 2, nome: 'Outro Condo', cnpj: '00000000000191', email: 'a@b.com', endereco: enderecoDTO },
      ])
    })
  })

  describe('getById', () => {
    it('gets /condominios/:id and unwraps data', async () => {
      let capturedId = ''
      server.use(
        http.get('/api/condominios/:id', ({ params }) => {
          capturedId = params.id as string
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: { id: 3, nome: 'Condo X', cnpj: '11111111000111', email: 'x@x.com', endereco: enderecoDTO },
          })
        }),
      )

      const result = await condominiosApi.getById(3)

      expect(capturedId).toBe('3')
      expect(result).toEqual({ id: 3, nome: 'Condo X', cnpj: '11111111000111', email: 'x@x.com', endereco: enderecoDTO })
    })

    it('rejects with 404 when condominio does not exist', async () => {
      server.use(http.get('/api/condominios/:id', () => HttpResponse.json({ error: 'not_found' }, { status: 404 })))

      await expect(condominiosApi.getById(999)).rejects.toBeTruthy()
    })
  })

  describe('create', () => {
    it('posts the body to /condominios and unwraps data', async () => {
      let capturedBody: unknown
      server.use(
        http.post('/api/condominios', async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json(
            {
              requestId: 'r',
              timestamp: new Date().toISOString(),
              data: { id: 10, nome: 'Novo Condo', cnpj: '22222222000122', email: 'n@n.com', endereco: enderecoDTO },
            },
            { status: 201 },
          )
        }),
      )

      const body = {
        nome: 'Novo Condo',
        cnpj: '22222222000122',
        email: 'n@n.com',
        endereco: { logradouro: 'Rua Nova', cep: '01000000', cidade: 'SP', estado: 35 },
      }
      const result = await condominiosApi.create(body)

      expect(capturedBody).toEqual(body)
      expect(result).toEqual({ id: 10, nome: 'Novo Condo', cnpj: '22222222000122', email: 'n@n.com', endereco: enderecoDTO })
    })

    it('rejects on validation error (400)', async () => {
      server.use(
        http.post('/api/condominios', () => HttpResponse.json({ error: 'validation_error' }, { status: 400 })),
      )

      await expect(
        condominiosApi.create({
          nome: '',
          cnpj: '',
          email: '',
          endereco: { logradouro: '', cep: '', cidade: '', estado: 0 },
        }),
      ).rejects.toBeTruthy()
    })
  })

  describe('update', () => {
    it('puts the body to /condominios/:id and unwraps data', async () => {
      let capturedBody: unknown
      let capturedId = ''
      server.use(
        http.put('/api/condominios/:id', async ({ request, params }) => {
          capturedBody = await request.json()
          capturedId = params.id as string
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: { id: 1, nome: 'Atualizado', cnpj: '12345678000195', email: 'upd@condo.com', endereco: enderecoDTO },
          })
        }),
      )

      const result = await condominiosApi.update(1, { nome: 'Atualizado' })

      expect(capturedId).toBe('1')
      expect(capturedBody).toEqual({ nome: 'Atualizado' })
      expect(result).toEqual({ id: 1, nome: 'Atualizado', cnpj: '12345678000195', email: 'upd@condo.com', endereco: enderecoDTO })
    })
  })

  describe('remove', () => {
    it('sends DELETE to /condominios/:id', async () => {
      let capturedId = ''
      server.use(
        http.delete('/api/condominios/:id', ({ params }) => {
          capturedId = params.id as string
          return new HttpResponse(null, { status: 204 })
        }),
      )

      await condominiosApi.remove(8)

      expect(capturedId).toBe('8')
    })

    it('rejects when removal fails', async () => {
      server.use(http.delete('/api/condominios/:id', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(condominiosApi.remove(8)).rejects.toBeTruthy()
    })
  })
})
