import { apiClient, extractData } from '../client'
import type {
  GrupoDespesaDTO,
  CreateGrupoDespesaDTO,
  CoeficienteRateioDTO,
  CreateCoeficienteRateioDTO,
  SimularRateioRequest,
  SimulacaoRateioDTO,
  RateioExecucaoDTO,
  RateioLoteResultado,
  RecalcularRateioRequest,
  PageResponse,
} from '@/types'

// ── Grupos de Despesa ─────────────────────────────────────────────────────────

export const gruposDespesaApi = {
  list: () =>
    apiClient.get<{ data: GrupoDespesaDTO[] }>('/grupos-despesa').then(extractData),

  getById: (id: number) =>
    apiClient.get<{ data: GrupoDespesaDTO }>(`/grupos-despesa/${id}`).then(extractData),

  create: (body: CreateGrupoDespesaDTO) =>
    apiClient.post<{ data: GrupoDespesaDTO }>('/grupos-despesa', body).then(extractData),

  update: (id: number, body: Partial<GrupoDespesaDTO>) =>
    apiClient.put<{ data: GrupoDespesaDTO }>(`/grupos-despesa/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/grupos-despesa/${id}`),
}

// ── Coeficientes de Rateio ────────────────────────────────────────────────────

export const coeficientesApi = {
  listByGrupo: (grupoDespesaId: number) =>
    apiClient
      .get<{ data: CoeficienteRateioDTO[] }>(`/grupos-despesa/${grupoDespesaId}/coeficientes`)
      .then(extractData),

  create: (grupoDespesaId: number, body: CreateCoeficienteRateioDTO) =>
    apiClient
      .post<{ data: CoeficienteRateioDTO }>(
        `/grupos-despesa/${grupoDespesaId}/coeficientes`,
        body,
      )
      .then(extractData),

  update: (grupoDespesaId: number, coefId: number, body: Partial<CreateCoeficienteRateioDTO>) =>
    apiClient
      .put<{ data: CoeficienteRateioDTO }>(
        `/grupos-despesa/${grupoDespesaId}/coeficientes/${coefId}`,
        body,
      )
      .then(extractData),

  remove: (grupoDespesaId: number, coefId: number) =>
    apiClient.delete(`/grupos-despesa/${grupoDespesaId}/coeficientes/${coefId}`),
}

// ── Execução de Rateio ────────────────────────────────────────────────────────

export const rateioApi = {
  simular: (body: SimularRateioRequest) =>
    apiClient
      .post<{ data: SimulacaoRateioDTO }>('/rateio/simular', body)
      .then(extractData),

  ratearDespesa: (despesaId: number) =>
    apiClient
      .post<{ data: RateioExecucaoDTO }>(`/rateio/despesa/${despesaId}`)
      .then(extractData),

  recalcular: (body: RecalcularRateioRequest) =>
    apiClient
      .post<{ data: RateioLoteResultado }>('/rateio/recalcular', body)
      .then(extractData),

  listarExecucoes: (params: { page: number; size: number }) =>
    apiClient
      .get<{ data: PageResponse<RateioExecucaoDTO> }>('/rateio/execucoes', { params })
      .then(extractData),
}
