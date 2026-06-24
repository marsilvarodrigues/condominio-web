import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { theme } from '@/theme/theme'
import { server } from './mocks/server'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import ApartamentoDetailPage from '@/pages/hierarquia/ApartamentoDetailPage'

const BASE = '/api'

function apiResponse(data: unknown) {
  return HttpResponse.json({
    requestId: 'test-req',
    timestamp: new Date().toISOString(),
    data,
  })
}

function renderDetail(initialPath = '/hierarquia/apartamentos/123') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={[initialPath]}>
          <Routes>
            <Route path="/hierarquia" element={<div>Hierarquia Page</div>} />
            <Route path="hierarquia/apartamentos/:id" element={<ApartamentoDetailPage />} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

function mockApartamento() {
  server.use(
    http.get(`${BASE}/apartamentos/123`, () =>
      apiResponse({
        id: 123,
        blocoId: 1,
        blocoNome: 'A',
        numero: '101',
        createdAt: null,
        updatedAt: null,
        areaConstruida: 75,
        fracaoIdeal: 2.5,
        andar: 1,
        quantidadeMoradores: 1,
      }),
    ),
  )
}

function mockMoradores(content: unknown[] = []) {
  server.use(
    http.get(`${BASE}/pessoas`, () =>
      apiResponse({ content, totalElements: content.length, totalPages: 1, size: 20, number: 0 }),
    ),
  )
}

function mockProprietarios(data: unknown[] = []) {
  server.use(
    http.get(`${BASE}/apartamentos/123/proprietarios`, () => apiResponse(data)),
  )
}

function mockCobrancas(content: unknown[] = []) {
  server.use(
    http.get(`${BASE}/cobrancas/apartamentos/123/cobrancas`, () =>
      apiResponse({ content, totalElements: content.length, totalPages: 1, size: 20, number: 0 }),
    ),
  )
}

beforeEach(() => {
  useAuthStore.setState({
    user: { id: 1, email: 'admin@test.com', roles: [ROLES.ADMIN], condominioIds: [1] },
    accessToken: 'token',
    refreshToken: 'refresh',
    activeCondominioId: 1,
  })

  mockApartamento()
  mockMoradores()
  mockProprietarios()
  mockCobrancas()
})

describe('ApartamentoDetailPage', () => {
  it('renderiza sem erros e mostra o número do apartamento e bloco', async () => {
    renderDetail()
    expect(await screen.findByText('Apt 101 — Bloco A')).toBeInTheDocument()
  })

  it('exibe a lista de moradores na aba padrão (Moradores)', async () => {
    mockMoradores([
      {
        id: 1,
        nome: 'João da Silva',
        tipo: 'MORADOR',
        cpf: '12345678900',
        email: 'joao@test.com',
        telefone: null,
        apartamentoId: 123,
      },
    ])
    renderDetail()

    await screen.findByText('Apt 101 — Bloco A')
    expect(await screen.findByText('João da Silva')).toBeInTheDocument()
    expect(screen.getByText('joao@test.com')).toBeInTheDocument()
  })

  it('exibe mensagem de vazio quando não há moradores cadastrados', async () => {
    renderDetail()
    expect(
      await screen.findByText('Apartamento sem moradores cadastrados.'),
    ).toBeInTheDocument()
  })

  it('exibe a lista de proprietários ao trocar para a aba Proprietários', async () => {
    mockProprietarios([
      {
        id: 9,
        nome: 'Maria Souza',
        tipo: 'PROP_PF',
        cpf: '98765432100',
        cnpj: null,
        razaoSocial: null,
        email: 'maria@test.com',
        telefone: null,
      },
    ])
    renderDetail()

    await screen.findByText('Apt 101 — Bloco A')
    const tab = await screen.findByRole('tab', { name: /proprietários/i })
    await userEvent.click(tab)

    expect(await screen.findByText('Maria Souza')).toBeInTheDocument()
    expect(screen.getByText('Pessoa Física')).toBeInTheDocument()
  })

  it('exibe a lista de cobranças ao trocar para a aba Cobranças', async () => {
    mockCobrancas([
      {
        id: 55,
        vencimento: '2026-07-10',
        valor: 350.5,
        status: 'PENDENTE',
        criadaEm: '2026-06-01T10:00:00Z',
        pagoEm: null,
        emailEnviado: true,
      },
    ])
    renderDetail()

    await screen.findByText('Apt 101 — Bloco A')
    const tab = await screen.findByRole('tab', { name: /cobranças/i })
    await userEvent.click(tab)

    expect(await screen.findByText('PENDENTE')).toBeInTheDocument()
    expect(screen.getByText(/350,50/)).toBeInTheDocument()
  })

  it('navega de volta para /hierarquia ao clicar em "Voltar"', async () => {
    renderDetail()
    await screen.findByText('Apt 101 — Bloco A')

    const backButton = screen.getByRole('button', { name: /voltar/i })
    await userEvent.click(backButton)

    expect(await screen.findByText('Hierarquia Page')).toBeInTheDocument()
  })
})
