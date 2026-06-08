import { apiClient } from './client'
import type { LoginRequest, TokenResponse, RefreshRequest, ChangePasswordDTO } from '@/types'

export const authApi = {
  login: (body: LoginRequest) =>
    apiClient.post<{ data: TokenResponse }>('/auth/login', body).then((r) => r.data.data),

  refresh: (body: RefreshRequest) =>
    apiClient.post<{ data: TokenResponse }>('/auth/refresh', body).then((r) => r.data.data),

  logout: () => apiClient.post('/auth/logout'),

  changePassword: (userId: number, body: ChangePasswordDTO) =>
    apiClient.patch(`/users/${userId}/password`, body),
}
