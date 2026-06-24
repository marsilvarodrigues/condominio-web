import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { theme } from '@/theme/theme'
import { useAuthStore } from '@/store/authStore'
import { server } from './mocks/server'
import PrestacaoContasPage from '@/pages/relatorio/PrestacaoContasPage'

const BASE = '/api'

vi.mock('jspdf', () => ({
  default: vi.fn().mockImplementation(() => ({
    internal: { pageSize: { getWidth: () => 210 } },
    addImage: vi.fn(),
    save: vi.fn(),
  })),
}))

vi.mock('html2canvas', () => ({
  default: vi.fn().mockResolvedValue({
    width: 800,
    height: 600,
    toDataURL: () => 'data:image/png;base64,fake',
  }),
}))

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/relatorio/prestacao-contas']}>
          <PrestacaoContasPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

function mockAllEndpoints() {
  server.use(
    http.get(`${BASE}/contas-bancarias`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: {
          content: [
            {
              id: 1,
              agencia: '0001',
              conta: '123456-7',
              tipo: 'CORRENTE',
              descricao: 'Conta principal',
              bancoId: 1,
              bancoNome: 'Itaú',
              saldo: 15000.5,
            },
          ],
        },
      }),
    ),
    http.get(`${BASE}/orcamentos`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: [
          {
            id: 1,
            ano: 2026,
            contaNome: 'Manutenção',
            contaId: 1,
            grupoDespesaId: 1,
            grupoDespesaNome: 'Geral',
            valorOrcado: 10000,
            valorRealizado: 6000,
            statusRateio: null,
          },
        ],
      }),
    ),
    http.get(`${BASE}/rateio/execucoes`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: {
          content: [
            {
              id: 7,
              despesaId: 1,
              despesaDescricao: 'Manutenção',
              grupoDespesaId: 1,
              tipoExecucao: 'IGUALITARIO',
              dataExecucao: '2026-06-01T10:00:00',
              despesaTotal: 10000,
              totalUnidades: 10,
              totalCotas: 10,
              status: 'SUCESSO',
              erroMensagem: null,
            },
          ],
          totalElements: 1,
          totalPages: 1,
          size: 1,
          number: 0,
        },
      }),
    ),
    http.get(`${BASE}/fundo-reserva`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: [{ id: 1, saldoAtual: 5000, percentualContribuicao: 10 }],
      }),
    ),
    http.get(`${BASE}/apartamentos`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: [],
      }),
    ),
    http.get(`${BASE}/cobrancas/resumo`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: { totalPendente: 0, quantidadeVencida: 0 },
      }),
    ),
  )
}

describe('PrestacaoContasPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'sindico@test.com', roles: ['ROLE_SINDICO'], condominioIds: [1] },
      activeCondominioId: 1,
    })
  })

  it('renders the financial report with budget and bank account data', async () => {
    mockAllEndpoints()
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Manutenção')).toBeInTheDocument()
    })
    expect(screen.getByText('Itaú')).toBeInTheDocument()
    expect(screen.getByText(/exportar pdf/i)).toBeInTheDocument()
  })

  it('shows empty state messages when there is no budget or bank account data', async () => {
    server.use(
      http.get(`${BASE}/contas-bancarias`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: { content: [] },
        }),
      ),
      http.get(`${BASE}/orcamentos`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [],
        }),
      ),
      http.get(`${BASE}/rateio/execucoes`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: { content: [], totalElements: 0, totalPages: 0, size: 1, number: 0 },
        }),
      ),
      http.get(`${BASE}/fundo-reserva`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [],
        }),
      ),
      http.get(`${BASE}/apartamentos`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [],
        }),
      ),
      http.get(`${BASE}/cobrancas/resumo`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: { totalPendente: 0, quantidadeVencida: 0 },
        }),
      ),
    )
    renderPage()

    await waitFor(() => {
      expect(
        screen.getByText(/nenhum item orçamentário encontrado para este exercício/i),
      ).toBeInTheDocument()
    })
    expect(screen.getByText(/nenhuma conta bancária cadastrada/i)).toBeInTheDocument()
  })

  it('triggers the PDF export action without throwing', async () => {
    mockAllEndpoints()
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Manutenção')).toBeInTheDocument()
    })

    const exportButton = screen.getByRole('button', { name: /exportar pdf/i })
    await userEvent.click(exportButton)

    await waitFor(() => {
      expect(exportButton).not.toBeDisabled()
    })
  })
})
