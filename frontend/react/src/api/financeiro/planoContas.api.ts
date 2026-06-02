import { apiClient, extractData } from '../client'
import type { PlanoContaDTO, CreatePlanoContaDTO } from '@/types'

export const planoContasApi = {
  list: () =>
    apiClient.get<{ data: PlanoContaDTO[] }>('/plano-contas').then(extractData),

  getById: (id: number) =>
    apiClient.get<{ data: PlanoContaDTO }>(`/plano-contas/${id}`).then(extractData),

  create: (body: CreatePlanoContaDTO) =>
    apiClient.post<{ data: PlanoContaDTO }>('/plano-contas', body).then(extractData),

  update: (id: number, body: Partial<CreatePlanoContaDTO>) =>
    apiClient.put<{ data: PlanoContaDTO }>(`/plano-contas/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/plano-contas/${id}`),
}
