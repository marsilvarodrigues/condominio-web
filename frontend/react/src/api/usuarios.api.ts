import { apiClient, extractData } from './client'
import type { UserDTO, CreateUserDTO, PageResponse } from '@/types'

export const usuariosApi = {
  list: (params?: { page?: number; size?: number }) =>
    apiClient
      .get<{ data: PageResponse<UserDTO> }>('/users', { params: { page: params?.page ?? 0, size: params?.size ?? 20 } })
      .then(extractData),

  getById: (id: number) =>
    apiClient.get<{ data: UserDTO }>(`/users/${id}`).then(extractData),

  create: (body: CreateUserDTO) =>
    apiClient.post<{ data: UserDTO }>('/users', body).then(extractData),

  update: (id: number, body: Partial<CreateUserDTO>) =>
    apiClient.put<{ data: UserDTO }>(`/users/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/users/${id}`),

  enable: (id: number) => apiClient.patch(`/users/${id}/enable`),

  disable: (id: number) => apiClient.patch(`/users/${id}/disable`),
}
