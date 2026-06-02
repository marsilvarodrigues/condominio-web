import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore } from '@/store/authStore'

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
    useAuthStore.getState().setUser({ email: 'test@test.com', roles: ['ROLE_USER'], condominioIds: [] })
    expect(useAuthStore.getState().user?.email).toBe('test@test.com')
  })

  it('identifies admin role', () => {
    useAuthStore.getState().setUser({ email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [] })
    expect(useAuthStore.getState().hasRole('ROLE_ADMIN')).toBe(true)
    expect(useAuthStore.getState().hasRole('ROLE_USER')).toBe(false)
  })

  it('clears state on logout', () => {
    useAuthStore.getState().setTokens('access', 'refresh')
    useAuthStore.getState().setUser({ email: 'a@b.com', roles: [], condominioIds: [] })
    useAuthStore.getState().logout()
    const state = useAuthStore.getState()
    expect(state.isAuthenticated()).toBe(false)
    expect(state.user).toBeNull()
  })

  it('sets active condominio', () => {
    useAuthStore.getState().setActiveCondominioId(42)
    expect(useAuthStore.getState().activeCondominioId).toBe(42)
  })
})
