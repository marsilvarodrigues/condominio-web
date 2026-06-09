import { apiClient, apiClientGlobal } from './client'
import type {
  ContaBancariaDTO,
  FundoReservaDTO,
  OrcamentoItemDTO,
  ApartamentoDTO,
  CobrancaDTO,
  RateioExecucaoDTO,
  PageResponse,
  ResumoFinanceiro,
  ResumoOrcamento,
  ResumoApartamentos,
  ResumoCobrancas,
} from '@/types'

export const dashboardApi = {
  // ── compartilhado entre Admin e Síndico ──────────────────────────────────

  saldoBancario: async (): Promise<ResumoFinanceiro> => {
    const contas: ContaBancariaDTO[] = await apiClient
      .get<{ data: ContaBancariaDTO[] }>('/contas-bancarias')
      .then((r) => r.data.data)
    return {
      saldoTotal: contas.reduce((s, c) => s + Number(c.saldo), 0),
      quantidadeContas: contas.length,
    }
  },

  fundoReserva: (): Promise<FundoReservaDTO | null> =>
    apiClient
      .get<{ data: FundoReservaDTO[] }>('/fundo-reserva')
      .then((r) => r.data.data?.[0] ?? null)
      .catch(() => null),

  orcamentoAno: async (exercicio: number): Promise<ResumoOrcamento | null> => {
    const itens: OrcamentoItemDTO[] = await apiClient
      .get<{ data: OrcamentoItemDTO[] }>('/orcamentos', { params: { ano: exercicio } })
      .then((r) => r.data.data)
      .catch(() => [])
    if (!itens.length) return null
    const prev = itens.reduce((s, i) => s + Number(i.valorOrcado), 0)
    const real = itens.reduce((s, i) => s + Number(i.valorRealizado), 0)
    return {
      exercicio,
      totalPrevisto: prev,
      totalRealizado: real,
      percentualExecucao: prev > 0 ? (real / prev) * 100 : 0,
    }
  },

  ultimaExecucao: (): Promise<RateioExecucaoDTO | null> =>
    apiClient
      .get<{ data: PageResponse<RateioExecucaoDTO> }>('/rateio/execucoes', {
        params: { page: 0, size: 1, sort: 'dataExecucao,desc' },
      })
      .then((r) => r.data.data.content[0] ?? null)
      .catch(() => null),

  resumoApartamentos: async (): Promise<ResumoApartamentos> => {
    const apts: ApartamentoDTO[] = await apiClient
      .get<{ data: ApartamentoDTO[] }>('/apartamentos')
      .then((r) => r.data.data)
      .catch(() => [])
    const ocupados = apts.filter((a) => a.proprietarioId != null).length
    return {
      total: apts.length,
      ocupados,
      vagos: apts.length - ocupados,
      taxaOcupacao: apts.length > 0 ? (ocupados / apts.length) * 100 : 0,
    }
  },

  resumoCobrancas: async (): Promise<ResumoCobrancas | null> => {
    try {
      const page: PageResponse<CobrancaDTO> = await apiClient
        .get<{ data: PageResponse<CobrancaDTO> }>('/cobrancas', {
          params: { page: 0, size: 200 },
        })
        .then((r) => r.data.data)
      const pend = page.content.filter((c) => c.status === 'PENDENTE')
      const venc = page.content.filter((c) => c.status === 'VENCIDA')
      return {
        totalPendente: pend.reduce((s, c) => s + Number(c.valor), 0),
        quantidadePendente: pend.length,
        totalVencido: venc.reduce((s, c) => s + Number(c.valor), 0),
        quantidadeVencida: venc.length,
      }
    } catch {
      return null
    }
  },

  // ── exclusivo do Administrador (cross-tenant) ────────────────────────────

  totalCondominios: (): Promise<number> =>
    apiClientGlobal
      .get<{ data: PageResponse<unknown> }>('/condominios', { params: { page: 0, size: 1 } })
      .then((r) => r.data.data.totalElements)
      .catch(() => 0),

  totalUsuarios: (): Promise<number> =>
    apiClientGlobal
      .get<{ data: PageResponse<unknown> }>('/users', { params: { page: 0, size: 1 } })
      .then((r) => r.data.data.totalElements)
      .catch(() => 0),

  // ── exclusivo do Morador ─────────────────────────────────────────────────

  orcamentoMorador: (): Promise<ResumoOrcamento | null> =>
    apiClient
      .get<{ data: ResumoOrcamento }>('/financeiro/orcamento/minha-unidade')
      .then((r) => r.data.data)
      .catch(() => null),
}
