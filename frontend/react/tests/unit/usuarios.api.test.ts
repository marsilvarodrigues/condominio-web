import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { usuariosApi } from '@/api/usuarios.api'
import { server } from './mocks/server'

const userDTO = {
  id: 1,
  email: 'user@test.com',
  name: 'User Test',
  enabled: true,
  roles: ['ROLE_USER'],
  condominioIds: [],
  activationToken: null,
  activationTokenExpiry: null,
}

const pageResponse = {
  content: [userDTO],
  totalElements: 1,
  totalPages: 1,
  size: 20,
  number: 0,
}

describe('usuariosApi', () => {
  describe('list', () => {
    it('gets /users with default page/size params and unwraps the page response', async () => {
      let capturedParams: URLSearchParams | null = null
      server.use(
        http.get('/api/users', ({ request }) => {
          capturedParams = new URL(request.url).searchParams
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: pageResponse,
          })
        }),
      )

      const result = await usuariosApi.list()

      expect(capturedParams!.get('page')).toBe('0')
      expect(capturedParams!.get('size')).toBe('20')
      expect(result).toEqual(pageResponse)
    })

    it('forwards explicit page/size params', async () => {
      let capturedParams: URLSearchParams | null = null
      server.use(
        http.get('/api/users', ({ request }) => {
          capturedParams = new URL(request.url).searchParams
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: { ...pageResponse, number: 2, size: 5 },
          })
        }),
      )

      const result = await usuariosApi.list({ page: 2, size: 5 })

      expect(capturedParams!.get('page')).toBe('2')
      expect(capturedParams!.get('size')).toBe('5')
      expect(result.number).toBe(2)
      expect(result.size).toBe(5)
    })

    it('rejects on server error', async () => {
      server.use(http.get('/api/users', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(usuariosApi.list()).rejects.toBeTruthy()
    })
  })

  describe('getById', () => {
    it('gets /users/:id and unwraps data', async () => {
      let capturedId = ''
      server.use(
        http.get('/api/users/:id', ({ params }) => {
          capturedId = params.id as string
          return HttpResponse.json({ requestId: 'r', timestamp: new Date().toISOString(), data: userDTO })
        }),
      )

      const result = await usuariosApi.getById(1)

      expect(capturedId).toBe('1')
      expect(result).toEqual(userDTO)
    })

    it('rejects with 404 when user does not exist', async () => {
      server.use(http.get('/api/users/:id', () => HttpResponse.json({ error: 'not_found' }, { status: 404 })))

      await expect(usuariosApi.getById(999)).rejects.toBeTruthy()
    })
  })

  describe('create', () => {
    it('posts the body to /users and unwraps data', async () => {
      let capturedBody: unknown
      server.use(
        http.post('/api/users', async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json(
            { requestId: 'r', timestamp: new Date().toISOString(), data: { ...userDTO, id: 2 } },
            { status: 201 },
          )
        }),
      )

      const body = { email: 'new@test.com', name: 'New User', roles: ['ROLE_USER'], condominioIds: [] }
      const result = await usuariosApi.create(body)

      expect(capturedBody).toEqual(body)
      expect(result).toEqual({ ...userDTO, id: 2 })
    })

    it('rejects on validation error (400)', async () => {
      server.use(http.post('/api/users', () => HttpResponse.json({ error: 'validation_error' }, { status: 400 })))

      await expect(
        usuariosApi.create({ email: '', name: '', roles: [], condominioIds: [] }),
      ).rejects.toBeTruthy()
    })
  })

  describe('update', () => {
    it('puts the body to /users/:id and unwraps data', async () => {
      let capturedBody: unknown
      let capturedId = ''
      server.use(
        http.put('/api/users/:id', async ({ request, params }) => {
          capturedBody = await request.json()
          capturedId = params.id as string
          return HttpResponse.json({
            requestId: 'r',
            timestamp: new Date().toISOString(),
            data: { ...userDTO, name: 'Updated Name' },
          })
        }),
      )

      const result = await usuariosApi.update(1, { name: 'Updated Name' })

      expect(capturedId).toBe('1')
      expect(capturedBody).toEqual({ name: 'Updated Name' })
      expect(result).toEqual({ ...userDTO, name: 'Updated Name' })
    })
  })

  describe('remove', () => {
    it('sends DELETE to /users/:id', async () => {
      let capturedId = ''
      server.use(
        http.delete('/api/users/:id', ({ params }) => {
          capturedId = params.id as string
          return new HttpResponse(null, { status: 204 })
        }),
      )

      await usuariosApi.remove(3)

      expect(capturedId).toBe('3')
    })

    it('rejects when removal fails', async () => {
      server.use(http.delete('/api/users/:id', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(usuariosApi.remove(3)).rejects.toBeTruthy()
    })
  })

  describe('enable', () => {
    it('patches /users/:id/enable', async () => {
      let capturedId = ''
      server.use(
        http.patch('/api/users/:id/enable', ({ params }) => {
          capturedId = params.id as string
          return HttpResponse.json({})
        }),
      )

      await usuariosApi.enable(4)

      expect(capturedId).toBe('4')
    })

    it('rejects when enabling fails', async () => {
      server.use(http.patch('/api/users/:id/enable', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(usuariosApi.enable(4)).rejects.toBeTruthy()
    })
  })

  describe('disable', () => {
    it('patches /users/:id/disable', async () => {
      let capturedId = ''
      server.use(
        http.patch('/api/users/:id/disable', ({ params }) => {
          capturedId = params.id as string
          return HttpResponse.json({})
        }),
      )

      await usuariosApi.disable(5)

      expect(capturedId).toBe('5')
    })

    it('rejects when disabling fails', async () => {
      server.use(http.patch('/api/users/:id/disable', () => HttpResponse.json({ error: 'fail' }, { status: 500 })))

      await expect(usuariosApi.disable(5)).rejects.toBeTruthy()
    })
  })
})
