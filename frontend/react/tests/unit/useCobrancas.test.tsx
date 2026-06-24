import React from 'react'
import { describe, it, expect, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import {
  useCobrancasDoApartamento,
  useCobrancaMutations,
  useCobrancas,
  useGerarCobrancas,
  useCancelarCobranca,
} from '@/hooks/useCobrancas'
import type { CobrancaDTO, CobrancaResumoDTO, PageResponse } from '@/types'

function makeWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
  return { qc, wrapper }
}

const resumoPage: PageResponse<CobrancaResumoDTO> = {
  content: [
    {
      id: 1,
      vencimento: '2026-07-10',
      valor: 350.5,
      status: 'PENDENTE',
      criadaEm: '2026-06-01T10:00:00Z',
      pagoEm: null,
      emailEnviado: false,
    },
  ],
  totalElements: 1,
  totalPages: 1,
  size: 20,
  number: 0,
}

const cobranca: CobrancaDTO = {
  id: 1,
  vencimento: '2026-07-10',
  valor: 350.5,
  status: 'CANCELADA',
  criadaEm: '2026-06-01T10:00:00Z',
  pagoEm: null,
  emailEnviado: false,
  apartamentoId: 10,
  apartamentoNumero: '101',
  blocoNome: 'A',
  moradorId: null,
  moradorNome: null,
  moradorEmail: null,
  boletoUrl: null,
  boletoCodBarras: null,
  pixQrCodeBase64: null,
  pixCopiaCola: null,
  emailEnviadoEm: null,
}

describe('useCobrancasDoApartamento', () => {
  it('fetches charges for a given apartamentoId', async () => {
    server.use(
      http.get('/api/cobrancas/apartamentos/10/cobrancas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: resumoPage }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCobrancasDoApartamento(10), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(resumoPage)
  })

  it('does not fetch when apartamentoId is falsy (enabled: false)', () => {
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCobrancasDoApartamento(0), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
    expect(result.current.isPending).toBe(true)
  })

  it('propagates errors from the API', async () => {
    server.use(
      http.get('/api/cobrancas/apartamentos/10/cobrancas', () =>
        HttpResponse.json({ error: 'boom' }, { status: 500 }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCobrancasDoApartamento(10), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useCobrancaMutations', () => {
  it('cancelar invalidates the apartment charge list on success', async () => {
    server.use(
      http.post('/api/cobrancas/1/cancelar', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: cobranca }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')

    const { result } = renderHook(() => useCobrancaMutations(10), { wrapper })

    await act(async () => {
      await result.current.cancelar.mutateAsync({ id: 1, motivo: 'erro' })
    })

    await waitFor(() => expect(result.current.cancelar.isSuccess).toBe(true))
    expect(result.current.cancelar.data).toEqual(cobranca)
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['cobrancas', 'apartamento', 10] })
  })

  it('reenviarEmail succeeds without invalidating queries', async () => {
    server.use(
      http.post('/api/cobrancas/1/reenviar-email', () => new HttpResponse(null, { status: 200 })),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCobrancaMutations(10), { wrapper })

    await act(async () => {
      await result.current.reenviarEmail.mutateAsync(1)
    })

    await waitFor(() => expect(result.current.reenviarEmail.isSuccess).toBe(true))
  })

  it('cancelar mutation surfaces errors', async () => {
    server.use(
      http.post('/api/cobrancas/1/cancelar', () =>
        HttpResponse.json({ error: 'erro' }, { status: 400 }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCobrancaMutations(10), { wrapper })

    await act(async () => {
      try {
        await result.current.cancelar.mutateAsync({ id: 1, motivo: 'x' })
      } catch {
        // expected
      }
    })

    await waitFor(() => expect(result.current.cancelar.isError).toBe(true))
  })
})

describe('useCobrancas (paginated list)', () => {
  it('fetches the filtered, paginated list', async () => {
    server.use(
      http.get('/api/cobrancas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: resumoPage }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCobrancas({ status: 'PENDENTE' }, 0), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(resumoPage)
  })

  it('is enabled by default (no apartamentoId gate)', async () => {
    server.use(
      http.get('/api/cobrancas', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: resumoPage }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCobrancas(), { wrapper })

    expect(result.current.fetchStatus).not.toBe('idle')
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
  })
})

describe('useGerarCobrancas', () => {
  it('generates charges and invalidates the cobrancas query key', async () => {
    server.use(
      http.post('/api/cobrancas/gerar', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: [cobranca] }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')

    const { result } = renderHook(() => useGerarCobrancas(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ execucaoId: 5, vencimento: '2026-08-10' })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual([cobranca])
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['cobrancas'] })
  })

  it('propagates server errors', async () => {
    server.use(
      http.post('/api/cobrancas/gerar', () =>
        HttpResponse.json({ error: 'falha' }, { status: 400 }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useGerarCobrancas(), { wrapper })

    await act(async () => {
      try {
        await result.current.mutateAsync({ execucaoId: 5, vencimento: '2026-08-10' })
      } catch {
        // expected
      }
    })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useCancelarCobranca', () => {
  it('cancels a charge and invalidates the cobrancas query key', async () => {
    server.use(
      http.post('/api/cobrancas/1/cancelar', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: cobranca }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')

    const { result } = renderHook(() => useCancelarCobranca(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ id: 1, motivo: 'duplicada' })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['cobrancas'] })
  })
})
