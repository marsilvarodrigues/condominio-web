import { apiClient, apiClientGlobal, extractData } from './client'
import type { CondominioDTO, CreateCondominioDTO } from '@/types'

export const condominiosApi = {
  list: () => apiClient.get<{ data: CondominioDTO[] }>('/condominios').then(extractData),

  /** Fetches all condominios accessible to the current user without requiring X-Condominio-Id.
   *  Used by the TopBar selector for users with 0 (global admin) or >1 assigned condominios. */
  listForSelector: () =>
    apiClientGlobal.get<{ data: CondominioDTO[] }>('/condominios').then(extractData),

  getById: (id: number) =>
    apiClient.get<{ data: CondominioDTO }>(`/condominios/${id}`).then(extractData),

  create: (body: CreateCondominioDTO) =>
    apiClient.post<{ data: CondominioDTO }>('/condominios', body).then(extractData),

  update: (id: number, body: Partial<CreateCondominioDTO>) =>
    apiClient.put<{ data: CondominioDTO }>(`/condominios/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/condominios/${id}`),
}
