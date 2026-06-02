import { apiClient, extractData } from './client'
import type { BlocoDTO, CreateBlocoDTO } from '@/types'

export const blocosApi = {
  list: () => apiClient.get<{ data: BlocoDTO[] }>('/blocos').then(extractData),

  getById: (id: number) =>
    apiClient.get<{ data: BlocoDTO }>(`/blocos/${id}`).then(extractData),

  create: (body: CreateBlocoDTO) =>
    apiClient.post<{ data: BlocoDTO }>('/blocos', body).then(extractData),

  update: (id: number, body: Partial<CreateBlocoDTO>) =>
    apiClient.put<{ data: BlocoDTO }>(`/blocos/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/blocos/${id}`),
}
