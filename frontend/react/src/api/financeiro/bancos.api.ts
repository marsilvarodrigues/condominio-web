import { apiClient, extractData } from '../client'
import type {
  BancoDTO,
  CreateBancoDTO,
  ContaBancariaDTO,
  CreateContaBancariaDTO,
  LancamentoBancarioDTO,
} from '@/types'

export const bancosApi = {
  list: () => apiClient.get<{ data: BancoDTO[] }>('/bancos').then(extractData),

  create: (body: CreateBancoDTO) =>
    apiClient.post<{ data: BancoDTO }>('/bancos', body).then(extractData),

  remove: (id: number) => apiClient.delete(`/bancos/${id}`),
}

export const contasBancariasApi = {
  list: () =>
    apiClient
      .get<{ data: { content: ContaBancariaDTO[] } }>('/contas-bancarias')
      .then((r) => r.data.data.content),

  getById: (id: number) =>
    apiClient.get<{ data: ContaBancariaDTO }>(`/contas-bancarias/${id}`).then(extractData),

  create: (body: CreateContaBancariaDTO) =>
    apiClient.post<{ data: ContaBancariaDTO }>('/contas-bancarias', body).then(extractData),

  update: (id: number, body: Partial<CreateContaBancariaDTO>) =>
    apiClient.put<{ data: ContaBancariaDTO }>(`/contas-bancarias/${id}`, body).then(extractData),

  lancamentos: (contaId: number) =>
    apiClient
      .get<{ data: { content: LancamentoBancarioDTO[] } }>('/lancamentos-bancarios', {
        params: { contaBancariaId: contaId, size: 200 },
      })
      .then((r) => r.data.data.content),

  remove: (id: number) => apiClient.delete(`/contas-bancarias/${id}`),
}
