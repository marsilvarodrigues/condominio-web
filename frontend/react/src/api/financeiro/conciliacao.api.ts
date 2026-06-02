import { apiClient, extractData } from '../client'
import type { ConciliacaoItemDTO } from '@/types'

export const conciliacaoApi = {
  importarExtrato: (contaId: number, arquivo: File) => {
    const form = new FormData()
    form.append('file', arquivo)
    return apiClient.post(`/conciliacao/importar/${contaId}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  listarItens: (contaId: number) =>
    apiClient
      .get<{ data: ConciliacaoItemDTO[] }>(`/conciliacao/itens/${contaId}`)
      .then(extractData),

  associar: (itemId: number, lancamentoId: number) =>
    apiClient.post(`/conciliacao/associacao/${itemId}`, { lancamentoBancarioId: lancamentoId }),

  sugestoes: (itemId: number) =>
    apiClient
      .get<{ data: unknown[] }>(`/conciliacao/associacao/sugestoes/${itemId}`)
      .then(extractData),

  desassociar: (itemId: number) =>
    apiClient.delete(`/conciliacao/associacao/${itemId}`),
}
