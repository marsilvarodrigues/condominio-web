import { apiClient, extractData } from './client'
import type { EstadoDTO } from '@/types'

export const estadosApi = {
  search: (params: { uf?: string; nome?: string }) =>
    apiClient
      .get<{ data: EstadoDTO[] }>('/estados', { params })
      .then(extractData),
}
