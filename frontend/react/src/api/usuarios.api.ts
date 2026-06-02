import { apiClient, extractData } from './client'
import type { UserDTO, CreateUserDTO } from '@/types'

export const usuariosApi = {
  list: () => apiClient.get<{ data: UserDTO[] }>('/users').then(extractData),

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
