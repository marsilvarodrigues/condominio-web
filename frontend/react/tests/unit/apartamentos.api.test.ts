import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { apartamentosApi } from '@/api/apartamentos.api'
import { server } from './mocks/server'

const apartamentoDTO = {
  id: 1,
  blocoId: 1,
  blocoNome: 'Bloco A',
  numero: '101',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  areaConstruida: 75.5,
  fracaoIdeal: 0.01,
  andar: 1,
  quantidadeMoradores: 2,
}

describe('apartamentosApi', () => {
  describe('list', () => {
    it('gets /apartamentos without params when blocoId is not given', async () => {
      let capturedParams: URLSearchParams | null = null
      server.use(
        http.get('/api/apartamentos', ({ request }) => {
          capturedParams = new URL(request.url).searchParams
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: [apartamentoDTO],
          })
        }),
      )

      const result = await apartamentosApi.list()

      expect(capturedParams!.has('blocoId')).toBe(false)
      expect(result).toEqual([apartamentoDTO])
    })

    it('gets /apartamentos with blocoId param when provided', async () => {
      let capturedParams: URLSearchParams | null = null
      server.use(
        http.get('/api/apartamentos', ({ request }) => {
          capturedParams = new URL(request.url).searchParams
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: [apartamentoDTO],
          })
        }),
      )

      const result = await apartamentosApi.list(1)

      expect(capturedParams!.get('blocoId')).toBe('1')
      expect(result).toEqual([apartamentoDTO])
    })

    it('rejects on server error', async () => {
      server.use(http.get('/api/apartamentos', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(apartamentosApi.list()).rejects.toBeTruthy()
    })
  })

  describe('getById', () => {
    it('gets /apartamentos/:id and unwraps data', async () => {
      let capturedId = ''
      server.use(
        http.get('/api/apartamentos/:id', ({ params }) => {
          capturedId = params.id as string
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: apartamentoDTO,
          })
        }),
      )

      const result = await apartamentosApi.getById(1)

      expect(capturedId).toBe('1')
      expect(result).toEqual(apartamentoDTO)
    })

    it('rejects with 404 when apartamento does not exist', async () => {
      server.use(
        http.get('/api/apartamentos/:id', () => HttpResponse.json({ error: 'not_found' }, { status: 404 })),
      )

      await expect(apartamentosApi.getById(999)).rejects.toBeTruthy()
    })
  })

  describe('create', () => {
    it('posts the body to /apartamentos and unwraps data', async () => {
      let capturedBody: unknown
      server.use(
        http.post('/api/apartamentos', async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json(
            { requestId: 'r', timestamp: new Date().toISOString(), data: { ...apartamentoDTO, id: 2 } },
            { status: 201 },
          )
        }),
      )

      const body = { blocoId: 1, numero: '102', areaConstruida: 80, fracaoIdeal: 0.012, andar: 1 }
      const result = await apartamentosApi.create(body)

      expect(capturedBody).toEqual(body)
      expect(result).toEqual({ ...apartamentoDTO, id: 2 })
    })

    it('rejects on validation error (400)', async () => {
      server.use(
        http.post('/api/apartamentos', () => HttpResponse.json({ error: 'validation_error' }, { status: 400 })),
      )

      await expect(
        apartamentosApi.create({ blocoId: 0, numero: '', areaConstruida: 0 }),
      ).rejects.toBeTruthy()
    })
  })

  describe('update', () => {
    it('puts the body to /apartamentos/:id and unwraps data', async () => {
      let capturedBody: unknown
      let capturedId = ''
      server.use(
        http.put('/api/apartamentos/:id', async ({ request, params }) => {
          capturedBody = await request.json()
          capturedId = params.id as string
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: { ...apartamentoDTO, quantidadeMoradores: 3 },
          })
        }),
      )

      const result = await apartamentosApi.update(1, { numero: '101A' })

      expect(capturedId).toBe('1')
      expect(capturedBody).toEqual({ numero: '101A' })
      expect(result).toEqual({ ...apartamentoDTO, quantidadeMoradores: 3 })
    })
  })

  describe('remove', () => {
    it('sends DELETE to /apartamentos/:id', async () => {
      let capturedId = ''
      server.use(
        http.delete('/api/apartamentos/:id', ({ params }) => {
          capturedId = params.id as string
          return new HttpResponse(null, { status: 204 })
        }),
      )

      await apartamentosApi.remove(6)

      expect(capturedId).toBe('6')
    })

    it('rejects when removal fails', async () => {
      server.use(
        http.delete('/api/apartamentos/:id', () => HttpResponse.json({ error: 'fail' }, { status: 500 })),
      )

      await expect(apartamentosApi.remove(6)).rejects.toBeTruthy()
    })
  })
})
