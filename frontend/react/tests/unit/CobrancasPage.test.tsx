import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { theme } from '@/theme/theme'
import { useAuthStore } from '@/store/authStore'
import { server } from './mocks/server'
import CobrancasPage from '@/pages/cobrancas/CobrancasPage'

const BASE = '/api'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/cobrancas']}>
          <CobrancasPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

const cobrancaBase = {
  id: 1,
  vencimento: '2026-07-10',
  valor: 350.75,
  status: 'PENDENTE' as const,
  criadaEm: '2026-06-01T10:00:00',
  pagoEm: null,
  emailEnviado: false,
  apartamentoId: 5,
  apartamentoNumero: '101',
  blocoNome: 'A',
  moradorId: 9,
  moradorNome: 'João Silva',
  moradorEmail: 'joao@test.com',
  boletoUrl: 'https://boleto.test/1',
  boletoCodBarras: '0000',
  pixQrCodeBase64: null,
  pixCopiaCola: null,
  emailEnviadoEm: null,
}

function mockCobrancasList(content: typeof cobrancaBase[] = [cobrancaBase]) {
  server.use(
    http.get(`${BASE}/cobrancas`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: {
          content,
          totalElements: content.length,
          totalPages: 1,
          size: 20,
          number: 0,
        },
      }),
    ),
  )
}

function mockResumo() {
  server.use(
    http.get(`${BASE}/cobrancas/resumo`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: { totalPendente: 350.75, quantidadeVencida: 2 },
      }),
    ),
  )
}

function mockRateioExecucoes() {
  server.use(
    http.get(`${BASE}/rateio/execucoes`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: { content: [], totalElements: 0, totalPages: 0, size: 50, number: 0 },
      }),
    ),
  )
}

describe('CobrancasPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })
    mockResumo()
    mockRateioExecucoes()
  })

  it('renders the list of charges from the API', async () => {
    mockCobrancasList()
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('João Silva')).toBeInTheDocument()
    })
    expect(screen.getByText(/A\s*—\s*101/)).toBeInTheDocument()
  })

  it('shows the empty state when there are no charges', async () => {
    mockCobrancasList([])
    renderPage()

    await waitFor(() => {
      expect(screen.getByText(/nenhuma cobrança encontrada/i)).toBeInTheDocument()
    })
  })

  it('shows resumo chips for pending and overdue totals', async () => {
    mockCobrancasList()
    renderPage()

    await waitFor(() => {
      expect(screen.getByText(/r\$\s*350,75\s*pendente/i)).toBeInTheDocument()
    })
    expect(screen.getByText(/2 vencidas/i)).toBeInTheDocument()
  })

  it('opens the gerar cobrancas dialog', async () => {
    mockCobrancasList()
    renderPage()

    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /gerar cobranças/i }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/gerar cobranças/i)).toBeInTheDocument()
  })

  it('cancels a charge successfully', async () => {
    mockCobrancasList()
    server.use(
      http.post(`${BASE}/cobrancas/1/cancelar`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: { ...cobrancaBase, status: 'CANCELADA' },
        }),
      ),
    )
    renderPage()

    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument())

    // The icon buttons only get an accessible name from MUI's Tooltip once hovered,
    // so target the Cancel action via its icon and the closest <button> ancestor.
    const cancelIcon = screen.getByTestId('CancelIcon')
    await userEvent.click(cancelIcon.closest('button')!)

    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText(/motivo/i), 'Pagamento já feito')
    await userEvent.click(within(dialog).getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
    await waitFor(() => {
      expect(screen.getByText(/cobrança cancelada/i)).toBeInTheDocument()
    })
  })

  it('shows an error state gracefully when the list request fails', async () => {
    server.use(
      http.get(`${BASE}/cobrancas`, () => HttpResponse.json({ message: 'erro' }, { status: 500 })),
    )
    renderPage()

    await waitFor(() => {
      expect(screen.getByText(/nenhuma cobrança encontrada/i)).toBeInTheDocument()
    })
  })
})
