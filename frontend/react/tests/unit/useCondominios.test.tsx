import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { useCondominios, useCondominioMutations } from '@/hooks/useCondominios'
import { useAuthStore } from '@/store/authStore'
import { server } from './mocks/server'
import type { CreateCondominioDTO } from '@/types'

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
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

describe('useCondominios', () => {
  it('fetches and returns the condominio list on success', async () => {
    const wrapper = createWrapper()
    const { result } = renderHook(() => useCondominios(), { wrapper })

    expect(result.current.isLoading).toBe(true)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(result.current.data).toEqual([
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

  it('surfaces an error state when the request fails', async () => {
    server.use(
      http.get('/api/condominios', () => HttpResponse.json({ error: 'server error' }, { status: 500 })),
    )
    const wrapper = createWrapper()
    const { result } = renderHook(() => useCondominios(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.data).toBeUndefined()
  })
})

describe('useCondominioMutations', () => {
  const novoCondominio: CreateCondominioDTO = {
    nome: 'Novo Condomínio',
    cnpj: '98765432000111',
    email: 'novo@condo.com',
    endereco: {
      logradouro: 'Av. Nova, 456',
      cep: '02020202',
      cidade: 'Rio de Janeiro',
      estado: 19,
    },
  }

  it('create: invalidates the condominios query on success', async () => {
    server.use(
      http.post('/api/condominios', () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: { id: 2, ...novoCondominio, endereco: { ...novoCondominio.endereco, estado: { id: 19, nome: 'Rio de Janeiro', uf: 'RJ' } } },
          },
          { status: 201 },
        ),
      ),
    )
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    )

    const { result } = renderHook(
      () => ({ list: useCondominios(), mutations: useCondominioMutations() }),
      { wrapper },
    )

    await waitFor(() => expect(result.current.list.isSuccess).toBe(true))
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')

    act(() => {
      result.current.mutations.create.mutate(novoCondominio)
    })

    await waitFor(() => expect(result.current.mutations.create.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['condominios'] })
  })

  it('update: surfaces an error state when the backend rejects the request', async () => {
    server.use(
      http.put('/api/condominios/1', () =>
        HttpResponse.json({ error: 'validation_error' }, { status: 400 }),
      ),
    )
    const wrapper = createWrapper()
    const { result } = renderHook(() => useCondominioMutations(), { wrapper })

    act(() => {
      result.current.update.mutate({ id: 1, body: { nome: 'Inválido' } })
    })

    await waitFor(() => expect(result.current.update.isError).toBe(true))
  })

  it('remove: calls the delete endpoint and resolves on success', async () => {
    server.use(http.delete('/api/condominios/1', () => HttpResponse.json({}, { status: 204 })))
    const wrapper = createWrapper()
    const { result } = renderHook(() => useCondominioMutations(), { wrapper })

    act(() => {
      result.current.remove.mutate(1)
    })

    await waitFor(() => expect(result.current.remove.isSuccess).toBe(true))
  })
})
