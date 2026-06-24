import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { blocosApi } from '@/api/blocos.api'
import { server } from './mocks/server'

describe('blocosApi', () => {
  describe('list', () => {
    it('gets /blocos and unwraps data (uses default handler)', async () => {
      const result = await blocosApi.list()

      expect(result).toEqual([{ id: 1, numero: 1, bloco: 'A', condominioId: 1 }])
    })

    it('rejects on server error', async () => {
      server.use(http.get('/api/blocos', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(blocosApi.list()).rejects.toBeTruthy()
    })
  })

  describe('getById', () => {
    it('gets /blocos/:id and unwraps data', async () => {
      let capturedId = ''
      server.use(
        http.get('/api/blocos/:id', ({ params }) => {
          capturedId = params.id as string
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: { id: 5, numero: 2, bloco: 'B', condominioId: 1 },
          })
        }),
      )

      const result = await blocosApi.getById(5)

      expect(capturedId).toBe('5')
      expect(result).toEqual({ id: 5, numero: 2, bloco: 'B', condominioId: 1 })
    })

    it('rejects with 404 when bloco does not exist', async () => {
      server.use(http.get('/api/blocos/:id', () => HttpResponse.json({ error: 'not_found' }, { status: 404 })))

      await expect(blocosApi.getById(999)).rejects.toBeTruthy()
    })
  })

  describe('create', () => {
    it('posts the body to /blocos and unwraps data', async () => {
      let capturedBody: unknown
      server.use(
        http.post('/api/blocos', async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json(
            {
              requestId: 'r',
              timestamp: new Date().toISOString(),
              data: { id: 10, numero: 3, bloco: 'C', condominioId: 1 },
            },
            { status: 201 },
          )
        }),
      )

      const body = { numero: 3, bloco: 'C' }
      const result = await blocosApi.create(body)

      expect(capturedBody).toEqual(body)
      expect(result).toEqual({ id: 10, numero: 3, bloco: 'C', condominioId: 1 })
    })

    it('rejects on validation error (400)', async () => {
      server.use(
        http.post('/api/blocos', () => HttpResponse.json({ error: 'validation_error' }, { status: 400 })),
      )

      await expect(blocosApi.create({ numero: 0, bloco: '' })).rejects.toBeTruthy()
    })
  })

  describe('update', () => {
    it('puts the body to /blocos/:id and unwraps data', async () => {
      let capturedBody: unknown
      let capturedId = ''
      server.use(
        http.put('/api/blocos/:id', async ({ request, params }) => {
          capturedBody = await request.json()
          capturedId = params.id as string
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: { id: 1, numero: 9, bloco: 'Z', condominioId: 1 },
          })
        }),
      )

      const result = await blocosApi.update(1, { numero: 9 })

      expect(capturedId).toBe('1')
      expect(capturedBody).toEqual({ numero: 9 })
      expect(result).toEqual({ id: 1, numero: 9, bloco: 'Z', condominioId: 1 })
    })
  })

  describe('remove', () => {
    it('sends DELETE to /blocos/:id', async () => {
      let capturedId = ''
      server.use(
        http.delete('/api/blocos/:id', ({ params }) => {
          capturedId = params.id as string
          return new HttpResponse(null, { status: 204 })
        }),
      )

      await blocosApi.remove(4)

      expect(capturedId).toBe('4')
    })

    it('rejects when removal fails', async () => {
      server.use(http.delete('/api/blocos/:id', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(blocosApi.remove(4)).rejects.toBeTruthy()
    })
  })
})
