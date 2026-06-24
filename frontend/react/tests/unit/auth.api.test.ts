import { describe, it, expect } from 'vitest'
import { http, HttpResponse } from 'msw'
import { authApi } from '@/api/auth.api'
import { server } from './mocks/server'

describe('authApi', () => {
  describe('login', () => {
    it('posts credentials to /auth/login and returns the flat token response', async () => {
      let capturedBody: unknown
      server.use(
        http.post('/api/auth/login', async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json({
            accessToken: 'access-123',
            refreshToken: 'refresh-123',
            tokenType: 'Bearer',
            expiresIn: 3600,
          })
        }),
      )

      const result = await authApi.login({ email: 'user@test.com', password: 'secret' })

      expect(capturedBody).toEqual({ email: 'user@test.com', password: 'secret' })
      expect(result).toEqual({
        accessToken: 'access-123',
        refreshToken: 'refresh-123',
        tokenType: 'Bearer',
        expiresIn: 3600,
      })
    })

    it('rejects when the backend returns 401 for bad credentials', async () => {
      server.use(
        http.post('/api/auth/login', () => HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })),
      )

      await expect(authApi.login({ email: 'bad@test.com', password: 'wrong' })).rejects.toBeTruthy()
    })
  })

  describe('refresh', () => {
    it('posts refreshToken to /auth/refresh and returns the flat token response', async () => {
      let capturedBody: unknown
      server.use(
        http.post('/api/auth/refresh', async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json({
            accessToken: 'new-access',
            refreshToken: 'new-refresh',
            tokenType: 'Bearer',
            expiresIn: 3600,
          })
        }),
      )

      const result = await authApi.refresh({ refreshToken: 'old-refresh' })

      expect(capturedBody).toEqual({ refreshToken: 'old-refresh' })
      expect(result).toEqual({
        accessToken: 'new-access',
        refreshToken: 'new-refresh',
        tokenType: 'Bearer',
        expiresIn: 3600,
      })
    })

    it('rejects when the refresh token is invalid (401)', async () => {
      server.use(
        http.post('/api/auth/refresh', () => HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })),
      )

      await expect(authApi.refresh({ refreshToken: 'expired' })).rejects.toBeTruthy()
    })
  })

  describe('logout', () => {
    it('posts to /auth/logout', async () => {
      let called = false
      server.use(
        http.post('/api/auth/logout', () => {
          called = true
          return HttpResponse.json({})
        }),
      )

      await authApi.logout()

      expect(called).toBe(true)
    })
  })

  describe('changePassword', () => {
    it('patches /users/:id/password with the body', async () => {
      let capturedBody: unknown
      let capturedUrl = ''
      server.use(
        http.patch('/api/users/:id/password', async ({ request, params }) => {
          capturedBody = await request.json()
          capturedUrl = params.id as string
          return HttpResponse.json({})
        }),
      )

      const body = { currentPassword: 'old', newPassword: 'new', confirmPassword: 'new' }
      await authApi.changePassword(7, body)

      expect(capturedUrl).toBe('7')
      expect(capturedBody).toEqual(body)
    })

    it('rejects when the current password is wrong (400)', async () => {
      server.use(
        http.patch('/api/users/:id/password', () =>
          HttpResponse.json({ error: 'validation_error' }, { status: 400 }),
        ),
      )

      await expect(
        authApi.changePassword(7, { currentPassword: 'bad', newPassword: 'new', confirmPassword: 'new' }),
      ).rejects.toBeTruthy()
    })
  })
})
