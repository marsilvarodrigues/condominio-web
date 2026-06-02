import { apiClient, extractData } from './client'
import type { ApartamentoDTO, CreateApartamentoDTO } from '@/types'

export const apartamentosApi = {
  list: (blocoId?: number) => {
    const params = blocoId ? { blocoId } : {}
    return apiClient.get<{ data: ApartamentoDTO[] }>('/apartamentos', { params }).then(extractData)
  },

  getById: (id: number) =>
    apiClient.get<{ data: ApartamentoDTO }>(`/apartamentos/${id}`).then(extractData),

  create: (body: CreateApartamentoDTO) =>
    apiClient.post<{ data: ApartamentoDTO }>('/apartamentos', body).then(extractData),

  update: (id: number, body: Partial<CreateApartamentoDTO>) =>
    apiClient.put<{ data: ApartamentoDTO }>(`/apartamentos/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/apartamentos/${id}`),
}
