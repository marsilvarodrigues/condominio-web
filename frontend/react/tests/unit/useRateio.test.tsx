import React from 'react'
import { describe, it, expect, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import {
  useGruposDespesa,
  useGruposDespesaMutations,
  useCoeficientesGrupo,
  useCoeficientesMutations,
  useSimularRateio,
  useRecalcularRateio,
  useExecucoesRateio,
} from '@/hooks/useRateio'
import type {
  GrupoDespesaDTO,
  CoeficienteRateioDTO,
  SimulacaoRateioDTO,
  RateioExecucaoDTO,
  RateioLoteResultado,
  PageResponse,
} from '@/types'

function makeWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
  return { qc, wrapper }
}

const grupo: GrupoDespesaDTO = {
  id: 1,
  nome: 'Manutenção Geral',
  tipoRateio: 'IGUALITARIO',
  escopo: 'TODOS',
  blocoId: null,
  planoContasId: null,
  parametrosJson: null,
}

const coeficiente: CoeficienteRateioDTO = {
  id: 1,
  grupoDespesaId: 1,
  apartamentoId: 10,
  apartamentoNumero: '101',
  blocoNome: 'A',
  coeficiente: 0.25,
  vigenciaInicio: '2026-01-01',
  vigenciaFim: null,
}

const simulacao: SimulacaoRateioDTO = {
  grupoNome: 'Manutenção Geral',
  totalDespesas: 1000,
  linhas: [
    {
      apartamentoId: 10,
      apartamentoNumero: '101',
      blocoNome: 'A',
      coeficiente: 0.25,
      valorRateado: 250,
    },
  ],
}

const execucao: RateioExecucaoDTO = {
  id: 1,
  despesaId: 5,
  despesaDescricao: 'Conta de água',
  grupoDespesaId: 1,
  tipoExecucao: 'MANUAL',
  dataExecucao: '2026-06-01T10:00:00Z',
  despesaTotal: 1000,
  totalUnidades: 4,
  totalCotas: 1,
  status: 'SUCESSO',
  erroMensagem: null,
}

const loteResultado: RateioLoteResultado = {
  total: 4,
  sucesso: 4,
  erro: 0,
  duracaoMs: 120,
}

const execucaoPage: PageResponse<RateioExecucaoDTO> = {
  content: [execucao],
  totalElements: 1,
  totalPages: 1,
  size: 20,
  number: 0,
}

describe('useGruposDespesa', () => {
  it('fetches the grupos-despesa list', async () => {
    server.use(
      http.get('/api/grupos-despesa', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: [grupo] }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useGruposDespesa(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual([grupo])
  })

  it('propagates errors', async () => {
    server.use(
      http.get('/api/grupos-despesa', () => HttpResponse.json({ error: 'fail' }, { status: 500 })),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useGruposDespesa(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useGruposDespesaMutations', () => {
  it('create mutation invalidates the grupos-despesa query', async () => {
    server.use(
      http.post('/api/grupos-despesa', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: grupo }, { status: 201 }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => useGruposDespesaMutations(), { wrapper })

    await act(async () => {
      await result.current.create.mutateAsync({
        nome: 'Manutenção Geral',
        tipoRateio: 'IGUALITARIO',
        escopo: 'TODOS',
      })
    })

    await waitFor(() => expect(result.current.create.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['grupos-despesa'] })
  })

  it('remove mutation surfaces errors', async () => {
    server.use(
      http.delete('/api/grupos-despesa/1', () =>
        HttpResponse.json({ error: 'fail' }, { status: 400 }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useGruposDespesaMutations(), { wrapper })

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

describe('useCoeficientesGrupo', () => {
  it('fetches coeficientes for a grupoDespesaId', async () => {
    server.use(
      http.get('/api/grupos-despesa/1/coeficientes', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: [coeficiente] }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCoeficientesGrupo(1), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual([coeficiente])
  })

  it('does not fetch when grupoDespesaId is not > 0', () => {
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useCoeficientesGrupo(0), { wrapper })

    expect(result.current.fetchStatus).toBe('idle')
  })
})

describe('useCoeficientesMutations', () => {
  it('create mutation invalidates the grupo-scoped coeficientes query', async () => {
    server.use(
      http.post('/api/grupos-despesa/1/coeficientes', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: coeficiente }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => useCoeficientesMutations(1), { wrapper })

    await act(async () => {
      await result.current.create.mutateAsync({ apartamentoId: 10, coeficiente: 0.25 })
    })

    await waitFor(() => expect(result.current.create.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['coeficientes', 1] })
  })

  it('update mutation invalidates the grupo-scoped coeficientes query', async () => {
    server.use(
      http.put('/api/grupos-despesa/1/coeficientes/1', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: coeficiente }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => useCoeficientesMutations(1), { wrapper })

    await act(async () => {
      await result.current.update.mutateAsync({ id: 1, body: { coeficiente: 0.3 } })
    })

    await waitFor(() => expect(result.current.update.isSuccess).toBe(true))
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['coeficientes', 1] })
  })
})

describe('useSimularRateio', () => {
  it('simulates a rateio and returns the simulation result', async () => {
    server.use(
      http.post('/api/rateio/simular', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: simulacao }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useSimularRateio(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ grupoId: 1, ano: 2026 })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(simulacao)
  })

  it('propagates errors from the simulation endpoint', async () => {
    server.use(
      http.post('/api/rateio/simular', () => HttpResponse.json({ error: 'fail' }, { status: 400 })),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useSimularRateio(), { wrapper })

    await act(async () => {
      try {
        await result.current.mutateAsync({ grupoId: 1, ano: 2026 })
      } catch {
        // expected
      }
    })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useRecalcularRateio', () => {
  it('recalculates and invalidates the rateio-execucoes query', async () => {
    server.use(
      http.post('/api/rateio/recalcular', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: loteResultado }),
      ),
    )
    const { qc, wrapper } = makeWrapper()
    const invalidateSpy = vi.spyOn(qc, 'invalidateQueries')
    const { result } = renderHook(() => useRecalcularRateio(), { wrapper })

    await act(async () => {
      await result.current.mutateAsync({ confirmar: true })
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(loteResultado)
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['rateio-execucoes'] })
  })
})

describe('useExecucoesRateio', () => {
  it('fetches a page of execucoes with default page/size', async () => {
    server.use(
      http.get('/api/rateio/execucoes', () =>
        HttpResponse.json({ requestId: 'r', timestamp: 't', data: execucaoPage }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useExecucoesRateio(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(execucaoPage)
  })

  it('propagates errors', async () => {
    server.use(
      http.get('/api/rateio/execucoes', () =>
        HttpResponse.json({ error: 'fail' }, { status: 500 }),
      ),
    )
    const { wrapper } = makeWrapper()
    const { result } = renderHook(() => useExecucoesRateio(1, 10), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
