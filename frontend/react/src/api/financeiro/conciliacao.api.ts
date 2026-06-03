import { apiClient } from '../client'
import type { LancamentoBancarioDTO } from '@/types'

export const conciliacaoApi = {
  listarLancamentos: (contaId: number) =>
    apiClient
      .get<{ data: { content: LancamentoBancarioDTO[] } }>('/lancamentos-bancarios', {
        params: { contaBancariaId: contaId, size: 200 },
      })
      .then((r) => r.data.data.content),

  associar: (itemExtratoId: number, itemOrcamentoId: number) =>
    apiClient.post(`/conciliacao/associacao/${itemExtratoId}`, { itemOrcamentoId }),

  desassociar: (itemExtratoId: number, justificativa: string) =>
    apiClient.delete(`/conciliacao/associacao/${itemExtratoId}`, {
      data: { justificativa },
    }),

  sugestoes: (itemExtratoId: number) =>
    apiClient
      .get<{ data: unknown[] }>(`/conciliacao/associacao/sugestoes/${itemExtratoId}`)
      .then((r) => r.data.data),
}
