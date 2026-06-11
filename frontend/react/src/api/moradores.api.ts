import { apiClient, apiClientGlobal, extractData } from './client'
import type {
  PessoaDTO,
  CreatePessoaDTO,
  UpdatePessoaDTO,
  PessoaFilterDTO,
  ProprietarioDTO,
  CreateProprietarioDTO,
  UpdateProprietarioDTO,
  ProprietarioFilterDTO,
  PageResponse,
  HistoricoOcupacaoDTO,
} from '@/types'

export const pessoasApi = {
  list: (filter?: PessoaFilterDTO) =>
    apiClient
      .get<{ data: PageResponse<PessoaDTO> }>('/pessoas', { params: filter })
      .then(extractData),

  getById: (id: number) =>
    apiClient.get<{ data: PessoaDTO }>(`/pessoas/${id}`).then(extractData),

  create: (body: CreatePessoaDTO) =>
    apiClient.post<{ data: PessoaDTO }>('/pessoas', body).then(extractData),

  update: (id: number, body: UpdatePessoaDTO) =>
    apiClient.put<{ data: PessoaDTO }>(`/pessoas/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/pessoas/${id}`),

  assignToApartamento: (id: number, apartamentoId: number) =>
    apiClient
      .post<{ data: PessoaDTO }>(`/pessoas/${id}/apartamento`, null, { params: { apartamentoId } })
      .then(extractData),

  removeFromApartamento: (id: number) =>
    apiClient.delete<{ data: PessoaDTO }>(`/pessoas/${id}/apartamento`).then(extractData),

  /**
   * GET /pessoas/apartamentos/{apartamentoId}/historico-ocupacao
   * Acessível por ADMIN, SINDICO e PROPRIETARIO.
   */
  historicoOcupacao: (apartamentoId: number): Promise<HistoricoOcupacaoDTO[]> =>
    apiClient
      .get<{ data: HistoricoOcupacaoDTO[] }>(
        `/pessoas/apartamentos/${apartamentoId}/historico-ocupacao`,
      )
      .then((r) => r.data.data),
}

export const proprietariosApi = {
  list: (filter?: ProprietarioFilterDTO) =>
    apiClient
      .get<{ data: PageResponse<ProprietarioDTO> }>('/proprietarios', { params: filter })
      .then(extractData),

  getById: (id: number) =>
    apiClient.get<{ data: ProprietarioDTO }>(`/proprietarios/${id}`).then(extractData),

  create: (body: CreateProprietarioDTO) =>
    apiClient.post<{ data: ProprietarioDTO }>('/proprietarios', body).then(extractData),

  update: (id: number, body: UpdateProprietarioDTO) =>
    apiClient.put<{ data: ProprietarioDTO }>(`/proprietarios/${id}`, body).then(extractData),

  remove: (id: number) => apiClient.delete(`/proprietarios/${id}`),

  associarApartamento: (id: number, apartamentoId: number) =>
    apiClient
      .post<{ data: ProprietarioDTO }>(`/proprietarios/${id}/apartamentos`, null, {
        params: { apartamentoId },
      })
      .then(extractData),

  desassociarApartamento: (id: number, aptId: number) =>
    apiClient
      .delete<{ data: ProprietarioDTO }>(`/proprietarios/${id}/apartamentos/${aptId}`)
      .then(extractData),

  listByApartamento: (aptId: number) =>
    apiClient
      .get<{ data: ProprietarioDTO[] }>(`/apartamentos/${aptId}/proprietarios`)
      .then(extractData),

  /**
   * Retorna os dados do proprietário autenticado com todos os seus imóveis.
   * O backend extrai o proprietario_id do JWT — nenhum parâmetro enviado.
   * Usa apiClientGlobal (sem X-Condominio-Id) — acesso cross-tenant.
   */
  meusImoveis: (): Promise<ProprietarioDTO> =>
    apiClientGlobal
      .get<{ data: ProprietarioDTO }>('/proprietarios/meus-imoveis')
      .then(extractData),
}
