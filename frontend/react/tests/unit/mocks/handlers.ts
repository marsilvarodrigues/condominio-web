import { http, HttpResponse } from 'msw'

const BASE = '/api'

export const handlers = [
  // Auth
  http.post(`${BASE}/auth/login`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: {
        accessToken: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiY29uZG9taW5pb19pZHMiOltdLCJleHAiOjk5OTk5OTk5OTl9.fake',
        refreshToken: 'refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      },
    }),
  ),

  // Auth — refresh (flat AuthResponseDTO, no ApiResponse envelope — matches the real backend)
  http.post(`${BASE}/auth/refresh`, () =>
    HttpResponse.json({
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    }),
  ),

  // Condomínios
  http.get(`${BASE}/condominios`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: [
        {
          id: 1,
          nome: 'Condomínio Teste',
          cnpj: '12345678000195',
          email: 'admin@condo.com',
          endereco: {
            logradouro: 'Rua Teste, 123',
            cep: '01310100',
            cidade: 'São Paulo',
            estadoId: 35,
            estadoUf: 'SP',
          },
        },
      ],
    }),
  ),

  // Usuários
  http.get(`${BASE}/usuarios`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: [
        {
          id: 1,
          name: 'Admin User',
          email: 'admin@test.com',
          enabled: true,
          roles: ['ROLE_ADMIN'],
          condominioIds: [],
          activationToken: null,
          activationTokenExpiry: null,
        },
      ],
    }),
  ),

  // Grupos de Despesa
  http.get(`${BASE}/grupos-despesa`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: [
        {
          id: 1,
          nome: 'Manutenção Geral',
          tipoRateio: 'IGUALITARIO',
          escopo: 'TODOS',
          blocoId: null,
          planoContasId: null,
          parametrosJson: null,
        },
      ],
    }),
  ),

  http.post(`${BASE}/grupos-despesa`, () =>
    HttpResponse.json(
      {
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: { id: 2, nome: 'Novo Grupo', tipoRateio: 'IGUALITARIO', escopo: 'TODOS' },
      },
      { status: 201 },
    ),
  ),

  // Blocos
  http.get(`${BASE}/blocos`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: [{ id: 1, numero: 1, bloco: 'A', condominioId: 1 }],
    }),
  ),

  // Bancos
  http.get(`${BASE}/bancos`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: [{ id: 1, codigo: '341', nome: 'Itaú', ativo: true }],
    }),
  ),

  // Contas Bancárias
  http.get(`${BASE}/contas-bancarias`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: [],
    }),
  ),

  // Plano de Contas
  http.get(`${BASE}/plano-contas`, () =>
    HttpResponse.json({
      requestId: 'test-req',
      timestamp: new Date().toISOString(),
      data: [],
    }),
  ),
]
