import { describe, it, expect, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { useLogin, useChangePassword } from '@/hooks/useAuth'
import { useAuthStore } from '@/store/authStore'
import { server } from './mocks/server'

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/login']}>{children}</MemoryRouter>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  useAuthStore.setState({
    accessToken: null,
    refreshToken: null,
    user: null,
    activeCondominioId: null,
  })
})

// /auth/login returns a flat AuthResponseDTO (no ApiResponse envelope), matching the real
// backend contract — see tests/e2e/login.spec.ts and tests/unit/auth.api.test.ts. The default
// handler in mocks/handlers.ts wraps it in an envelope, so each test below overrides it locally.
function buildJwt(payload: Record<string, unknown>) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url')
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url')
  return `${header}.${body}.fake`
}

describe('useLogin', () => {
  it('on success, stores tokens and decoded user from the JWT', async () => {
    const token = buildJwt({
      sub: 'admin@test.com',
      roles: ['ROLE_ADMIN'],
      condominio_ids: [],
      exp: 9999999999,
    })
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json({
          accessToken: token,
          refreshToken: 'refresh-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
        }),
      ),
    )
    const wrapper = createWrapper()
    const { result } = renderHook(() => useLogin(), { wrapper })

    act(() => {
      result.current.mutate({ email: 'admin@test.com', password: 'secret' })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    const state = useAuthStore.getState()
    expect(state.accessToken).toBe(token)
    expect(state.refreshToken).toBe('refresh-token')
    expect(state.user).toEqual({
      id: 0,
      email: 'admin@test.com',
      roles: ['ROLE_ADMIN'],
      condominioIds: [],
    })
  })

  it('on mutation error (e.g. 401 invalid credentials), does not touch the auth store', async () => {
    server.use(
      http.post('/api/auth/login', () => HttpResponse.json({ error: 'invalid_credentials' }, { status: 401 })),
    )
    const wrapper = createWrapper()
    const { result } = renderHook(() => useLogin(), { wrapper })

    act(() => {
      result.current.mutate({ email: 'bad@test.com', password: 'wrong' })
    })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('when the access token cannot be decoded, sets tokens but leaves user unset', async () => {
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json({
          accessToken: 'not-a-valid-jwt',
          refreshToken: 'refresh-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
        }),
      ),
    )
    const wrapper = createWrapper()
    const { result } = renderHook(() => useLogin(), { wrapper })

    act(() => {
      result.current.mutate({ email: 'admin@test.com', password: 'secret' })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    // tokens are set before decode is attempted, but user remains unset since decoding failed
    expect(useAuthStore.getState().accessToken).toBe('not-a-valid-jwt')
    expect(useAuthStore.getState().user).toBeNull()
  })
})

describe('useChangePassword', () => {
  it('calls the change-password endpoint and resolves on success', async () => {
    server.use(
      http.patch('/api/users/1/password', () => HttpResponse.json({}, { status: 200 })),
    )
    const wrapper = createWrapper()
    const { result } = renderHook(() => useChangePassword(1), { wrapper })

    act(() => {
      result.current.mutate({
        currentPassword: 'old',
        newPassword: 'newpass123',
        confirmPassword: 'newpass123',
      })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
  })

  it('surfaces a mutation error when the backend rejects the change', async () => {
    server.use(
      http.patch('/api/users/1/password', () =>
        HttpResponse.json({ error: 'senha atual incorreta' }, { status: 400 }),
      ),
    )
    const wrapper = createWrapper()
    const { result } = renderHook(() => useChangePassword(1), { wrapper })

    act(() => {
      result.current.mutate({
        currentPassword: 'wrong',
        newPassword: 'newpass123',
        confirmPassword: 'newpass123',
      })
    })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
