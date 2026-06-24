import { describe, it, expect, beforeEach } from 'vitest'
import { http, HttpResponse } from 'msw'
import { useAuthStore } from '@/store/authStore'
import { server } from './mocks/server'

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: null,
      refreshToken: null,
      user: null,
      activeCondominioId: null,
    })
  })

  it('starts unauthenticated', () => {
    const state = useAuthStore.getState()
    expect(state.isAuthenticated()).toBe(false)
  })

  it('becomes authenticated after setTokens', () => {
    useAuthStore.getState().setTokens('access-token', 'refresh-token')
    expect(useAuthStore.getState().isAuthenticated()).toBe(true)
  })

  it('stores user after setUser', () => {
    useAuthStore.getState().setUser({ id: 1, email: 'test@test.com', roles: ['ROLE_USER'], condominioIds: [] })
    expect(useAuthStore.getState().user?.email).toBe('test@test.com')
  })

  it('identifies admin role', () => {
    useAuthStore.getState().setUser({ id: 2, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [] })
    expect(useAuthStore.getState().hasRole('ROLE_ADMIN')).toBe(true)
    expect(useAuthStore.getState().hasRole('ROLE_USER')).toBe(false)
  })

  it('clears state on logout', () => {
    useAuthStore.getState().setTokens('access', 'refresh')
    useAuthStore.getState().setUser({ id: 3, email: 'a@b.com', roles: [], condominioIds: [] })
    useAuthStore.getState().logout()
    const state = useAuthStore.getState()
    expect(state.isAuthenticated()).toBe(false)
    expect(state.user).toBeNull()
  })

  it('sets active condominio', () => {
    useAuthStore.getState().setActiveCondominioId(42)
    expect(useAuthStore.getState().activeCondominioId).toBe(42)
  })

  describe('bootstrap', () => {
    it('faz nada quando accessToken já está presente', async () => {
      useAuthStore.setState({ accessToken: 'already-set', refreshToken: 'r' })
      await useAuthStore.getState().bootstrap()
      expect(useAuthStore.getState().accessToken).toBe('already-set')
    })

    it('faz nada quando não há refreshToken (usuário nunca logou)', async () => {
      await useAuthStore.getState().bootstrap()
      expect(useAuthStore.getState().accessToken).toBeNull()
    })

    it('renova o accessToken via /auth/refresh quando só o refreshToken está presente', async () => {
      useAuthStore.setState({ accessToken: null, refreshToken: 'valid-refresh-token' })

      await useAuthStore.getState().bootstrap()

      const state = useAuthStore.getState()
      expect(state.accessToken).toBe('new-access-token')
      expect(state.refreshToken).toBe('new-refresh-token')
    })

    it('faz logout quando o refresh falha (refreshToken expirado/inválido)', async () => {
      server.use(
        http.post('/api/auth/refresh', () => HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })),
      )
      useAuthStore.setState({
        accessToken: null,
        refreshToken: 'expired-token',
        user: { id: 1, email: 'a@b.com', roles: ['ROLE_USER'], condominioIds: [] },
      })

      await useAuthStore.getState().bootstrap()

      const state = useAuthStore.getState()
      expect(state.accessToken).toBeNull()
      expect(state.refreshToken).toBeNull()
      expect(state.user).toBeNull()
    })
  })
})
