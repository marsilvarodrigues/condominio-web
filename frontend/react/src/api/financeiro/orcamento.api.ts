import { apiClient, extractData } from '../client'
import type { OrcamentoItemDTO, AddOrcamentoItemDTO } from '@/types'

export const orcamentoApi = {
  listByAno: (ano: number) =>
    apiClient.get<{ data: OrcamentoItemDTO[] }>('/orcamentos', { params: { ano } }).then(extractData),

  addItem: (ano: number, body: AddOrcamentoItemDTO) =>
    apiClient.post<{ data: OrcamentoItemDTO }>(`/orcamentos/${ano}/itens`, body).then(extractData),

  removeItem: (ano: number, itemId: number) =>
    apiClient.delete(`/orcamentos/${ano}/itens/${itemId}`),

  recalcularRateio: (ano: number) =>
    apiClient.post(`/orcamentos/${ano}/recalcular`),
}
