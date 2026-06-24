import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { theme } from '@/theme/theme'
import { useAuthStore } from '@/store/authStore'
import { server } from './mocks/server'
import ConciliacaoPage from '@/pages/financeiro/ConciliacaoPage'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/conciliacao']}>
          <ConciliacaoPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

const contasResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: {
    content: [
      { id: 1, agencia: '0001', conta: '12345-6', tipo: 'CORRENTE', descricao: 'Conta Principal', bancoId: 1, bancoNome: 'Itaú', saldo: 1000 },
    ],
  },
}

const lancamentosResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: {
    content: [
      {
        id: 1,
        contaBancariaId: 1,
        contaBancariaDescricao: 'Conta Principal',
        dataLancamento: '2026-06-01T00:00:00Z',
        valor: 500,
        tipo: 'CREDITO',
        descricao: 'Pagamento de cota',
        origem: 'COTA_CONDOMINIO',
        referenciaId: null,
        status: 'PENDENTE',
        createdAt: '2026-06-01T00:00:00Z',
        updatedAt: '2026-06-01T00:00:00Z',
      },
      {
        id: 2,
        contaBancariaId: 1,
        contaBancariaDescricao: 'Conta Principal',
        dataLancamento: '2026-06-02T00:00:00Z',
        valor: 200,
        tipo: 'DEBITO',
        descricao: 'Pagamento de fornecedor',
        origem: 'DESPESA_ORDINARIA',
        referenciaId: null,
        status: 'CONCILIADO',
        createdAt: '2026-06-02T00:00:00Z',
        updatedAt: '2026-06-02T00:00:00Z',
      },
    ],
  },
}

describe('ConciliacaoPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })

    server.use(
      http.get('/api/contas-bancarias', () => HttpResponse.json(contasResponse)),
      http.get('/api/lancamentos-bancarios', () => HttpResponse.json(lancamentosResponse)),
    )
  })

  it('shows a placeholder until a bank account is selected', () => {
    renderPage()
    expect(
      screen.getByText(/selecione uma conta bancária para visualizar os lançamentos/i),
    ).toBeInTheDocument()
  })

  it('lists bank entries and summary counters after selecting an account', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('combobox', { name: /conta bancária/i })).toBeInTheDocument()
    })

    await userEvent.click(screen.getByRole('combobox', { name: /conta bancária/i }))
    await userEvent.click(await screen.findByRole('option', { name: /itaú/i }))

    await waitFor(() => {
      expect(screen.getByText('Pagamento de cota')).toBeInTheDocument()
    })
    expect(screen.getByText('Pagamento de fornecedor')).toBeInTheDocument()

    // Summary counters: 1 pendente, 1 conciliado, 2 total
    expect(screen.getByText('Pendentes')).toBeInTheDocument()
    expect(screen.getByText('Conciliados')).toBeInTheDocument()
    expect(screen.getByText('Total')).toBeInTheDocument()
  })

  it('filters entries by status', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('combobox', { name: /conta bancária/i }))
    await userEvent.click(await screen.findByRole('option', { name: /itaú/i }))

    await waitFor(() => {
      expect(screen.getByText('Pagamento de cota')).toBeInTheDocument()
    })

    await userEvent.click(screen.getByRole('combobox', { name: /status/i }))
    await userEvent.click(await screen.findByRole('option', { name: /^conciliado$/i }))

    await waitFor(() => {
      expect(screen.queryByText('Pagamento de cota')).not.toBeInTheDocument()
    })
    expect(screen.getByText('Pagamento de fornecedor')).toBeInTheDocument()
  })

  it('shows an empty state when the account has no entries', async () => {
    server.use(
      http.get('/api/lancamentos-bancarios', () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: { content: [] },
        }),
      ),
    )
    renderPage()

    await userEvent.click(screen.getByRole('combobox', { name: /conta bancária/i }))
    await userEvent.click(await screen.findByRole('option', { name: /itaú/i }))

    await waitFor(() => {
      expect(
        screen.getByText(/nenhum lançamento encontrado para esta conta/i),
      ).toBeInTheDocument()
    })
  })
})
