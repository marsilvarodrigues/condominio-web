import React from 'react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import {
  useMoradoresDeApartamento,
  useProprietariosDeApartamento,
  usePessoas,
  usePessoaMutations,
  useProprietarios,
  useHistoricoOcupacao,
} from '@/hooks/useMoradores'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { PessoaDTO, ProprietarioDTO, PageResponse, HistoricoOcupacaoDTO } from '@/types'

function makeWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
  return { qc, wrapper }
}

const pessoa: PessoaDTO = {
  id: 1,
  nome: 'João Silva',
  tipo: 'MORADOR',
  cpf: '12345678901',
  email: 'joao@test.com',
  telefone: '11999999999',
  apartamentoId: 10,
  apartamentoNumero: '101',
  userId: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null,
}

const pessoaPage: PageResponse<PessoaDTO> = {
  content: [pessoa],
  totalElements: 1,
  totalPages: 1,
  size: 20,
  number: 0,
}

const proprietario: ProprietarioDTO = {
  id: 1,
  nome: 'Maria Souza',
  tipo: 'PROP_PF',
  cpf: '98765432100',
  cnpj: null,
  razaoSocial: null,
  email: 'maria@test.com',
  telefone: '11888888888',
  apartamentos: [],
  userId: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null,
}

const historico: HistoricoOcupacaoDTO = {
  id: 1,
  apartamentoId: 10,
  pessoaId: 1,
  nomeMorador: 'João Silva',
  emailMorador: 'joao@test.com',
  cpfMorador: '12345678901',
  dataEntrada: '2025-01-01',
  dataSaida: '2025-12-31',
  createdAt: '2025-01-01T00:00:00Z',
}

beforeEach(() => {
  useAuthStore.setState({
    user: null,
    accessToken: null,
    refreshToken: null,
    activeCondominioId: null,
  })
})

describe('useMoradoresDeApartamento', () => {
  it('fetches the moradores list scoped to the apartment', async () => {
    server.use(
      http.get('/api/pessoas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: pessoaPage }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useMoradoresDeApartamento(10), { wrapper })

    await waitFor(() => expect(result.current.query.isSuccess).toBe(true))
    expect(result.current.query.data).toEqual([pessoa])
  })

  it('does not fetch when aptId is not > 0', () => {
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useMoradoresDeApartamento(0), { wrapper })

    expect(result.current.query.fetchStatus).toBe('idle')
  })

  it('create mutation invalidates the apartment-scoped query', async () => {
    server.use(
      http.get('/api/pessoas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: pessoaPage }),
      ),
      http.post('/api/pessoas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: pessoa }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => useMoradoresDeApartamento(10), { wrapper })

    await act(async () => {
      await result.current.create.mutateAsync({
        nome: 'João Silva',
        email: 'joao@test.com',
        cpf: '12345678901',
        apartamentoId: 10,
      })
    })

    await waitFor(() => expect(result.current.create.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['moradores-apt', 10] })
  })

  it('remove mutation surfaces errors', async () => {
    server.use(
      http.get('/api/pessoas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: pessoaPage }),
      ),
      http.delete('/api/pessoas/1', () => HttpResponse.json({ error: 'fail' }, { status: 400 })),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useMoradoresDeApartamento(10), { wrapper })

    await act(async () => {
      try {
        await result.current.remove.mutateAsync(1)
      } catch {
        // expected
      }
    })

    await waitFor(() => expect(result.current.remove.isError).toBe(true))
  })
})

describe('useProprietariosDeApartamento', () => {
  it('fetches proprietarios for the apartment', async () => {
    server.use(
      http.get('/api/apartamentos/10/proprietarios', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: [proprietario] }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useProprietariosDeApartamento(10), { wrapper })

    await waitFor(() => expect(result.current.query.isSuccess).toBe(true))
    expect(result.current.query.data).toEqual([proprietario])
  })

  it('does not fetch when aptId is not > 0', () => {
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useProprietariosDeApartamento(0), { wrapper })

    expect(result.current.query.fetchStatus).toBe('idle')
  })

  it('create mutation creates proprietario then associates apartment and invalidates', async () => {
    server.use(
      http.get('/api/apartamentos/10/proprietarios', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: [proprietario] }),
      ),
      http.post('/api/proprietarios', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: proprietario }),
      ),
      http.post('/api/proprietarios/1/apartamentos', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: proprietario }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => useProprietariosDeApartamento(10), { wrapper })

    await act(async () => {
      await result.current.create.mutateAsync({
        nome: 'Maria Souza',
        email: 'maria@test.com',
        tipo: 'PROP_PF',
        cpf: '98765432100',
      })
    })

    await waitFor(() => expect(result.current.create.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['proprietarios-apt', 10] })
  })

  it('desassociar mutation invalidates the apartment-scoped query', async () => {
    server.use(
      http.get('/api/apartamentos/10/proprietarios', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: [proprietario] }),
      ),
      http.delete('/api/proprietarios/1/apartamentos/10', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: proprietario }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => useProprietariosDeApartamento(10), { wrapper })

    await act(async () => {
      await result.current.desassociar.mutateAsync(1)
    })

    await waitFor(() => expect(result.current.desassociar.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['proprietarios-apt', 10] })
  })
})

describe('usePessoas', () => {
  it('fetches the global pessoas list', async () => {
    server.use(
      http.get('/api/pessoas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: pessoaPage }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => usePessoas(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(pessoaPage)
  })

  it('propagates errors', async () => {
    server.use(
      http.get('/api/pessoas', () => HttpResponse.json({ error: 'fail' }, { status: 500 })),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => usePessoas(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('usePessoaMutations', () => {
  it('update mutation invalidates the pessoas query key', async () => {
    server.use(
      http.put('/api/pessoas/1', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: pessoa }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => usePessoaMutations(), { wrapper })

    await act(async () => {
      await result.current.update.mutateAsync({ id: 1, body: { nome: 'Novo Nome' } })
    })

    await waitFor(() => expect(result.current.update.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['pessoas'] })
  })
})

describe('useProprietarios', () => {
  it('fetches the global proprietarios list', async () => {
    server.use(
      http.get('/api/proprietarios', () =>
        HttpResponse.json({
          requestId: 'r',
          timestamp: 't',
          data: { content: [proprietario], totalElements: 1, totalPages: 1, size: 20, number: 0 },
        }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useProprietarios(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.content).toEqual([proprietario])
  })
})

describe('useHistoricoOcupacao', () => {
  it('fetches history when aptId > 0 and user has ADMIN role', async () => {
    useAuthStore.setState({
      user: { id: 1, email: 'admin@test.com', roles: [ROLES.ADMIN], condominioIds: [] },
    })
    server.use(
      http.get('/api/pessoas/apartamentos/10/historico-ocupacao', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: { content: [historico] } }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useHistoricoOcupacao(10), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual([historico])
  })

  it('does not fetch when user lacks an allowed role', () => {
    useAuthStore.setState({
      user: { id: 2, email: 'morador@test.com', roles: [ROLES.MORADOR], condominioIds: [] },
    })
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useHistoricoOcupacao(10), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
  })

  it('does not fetch when aptId is not > 0, even with an allowed role', () => {
    useAuthStore.setState({
      user: { id: 1, email: 'admin@test.com', roles: [ROLES.ADMIN], condominioIds: [] },
    })
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useHistoricoOcupacao(0), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
  })
})
