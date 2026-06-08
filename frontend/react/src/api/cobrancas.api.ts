import { apiClient, extractData } from './client'
import type { CobrancaDTO, CobrancaResumoDTO, PageResponse } from '@/types'

/**
 * API client for the Cobrança module endpoints.
 */
export const cobrancasApi = {
  /**
   * Returns a paginated list of charge summaries for the given apartment, newest first.
   */
  porApartamento: (apartamentoId: number, page = 0, size = 20) =>
    apiClient
      .get<{ data: PageResponse<CobrancaResumoDTO> }>(
        `/cobrancas/apartamentos/${apartamentoId}/cobrancas`,
        { params: { page, size, sort: 'createdAt,desc' } },
      )
      .then((r) => r.data.data),

  /**
   * Cancels a charge by ID, providing a human-readable reason.
   */
  cancelar: (id: number, motivo: string) =>
    apiClient
      .post<{ data: CobrancaDTO }>(`/cobrancas/${id}/cancelar`, { motivo })
      .then(extractData),

  /**
   * Re-sends the billing email for a charge.
   */
  reenviarEmail: (id: number) =>
    apiClient.post<void>(`/cobrancas/${id}/reenviar-email`),
}
