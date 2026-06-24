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
import FundoReservaPage from '@/pages/financeiro/FundoReservaPage'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/fundo-reserva']}>
          <FundoReservaPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

const movimentacoesResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: [
    {
      id: 1,
      tipo: 'ENTRADA',
      valor: 1000,
      descricao: 'Contribuição mensal',
      data: '2026-06-01T00:00:00Z',
      saldoApos: 5000,
    },
  ],
}

const saldoResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: 5000,
}

describe('FundoReservaPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })

    server.use(
      http.get('/api/fundo-reserva/movimentacoes', () => HttpResponse.json(movimentacoesResponse)),
      http.get('/api/fundo-reserva/saldo', () => HttpResponse.json(saldoResponse)),
    )
  })

  it('renders the current balance and movement list', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Contribuição mensal')).toBeInTheDocument()
    })
    expect(screen.getByText('Saldo Atual')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*5\.000,00/)).toBeInTheDocument()
  })

  it('opens the dialog and creates a new movimentação', async () => {
    server.use(
      http.post('/api/fundo-reserva/movimentacoes', () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: { id: 2, tipo: 'SAIDA', valor: 300, descricao: 'Reparo emergencial', data: '2026-06-10', saldoApos: 4700 },
          },
          { status: 201 },
        ),
      ),
    )
    renderPage()

    await waitFor(() => expect(screen.getByText('Contribuição mensal')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nova movimentação/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/^data$/i), '2026-06-10')
    await userEvent.type(within(dialog).getByLabelText(/valor/i), '300')
    await userEvent.type(within(dialog).getByLabelText(/descrição/i), 'Reparo emergencial')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows a validation error when descrição is too short', async () => {
    renderPage()

    await waitFor(() => expect(screen.getByText('Contribuição mensal')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nova movimentação/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/^data$/i), '2026-06-10')
    await userEvent.type(within(dialog).getByLabelText(/valor/i), '300')
    await userEvent.type(within(dialog).getByLabelText(/descrição/i), 'Ab')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/mínimo 3 caracteres/i)).toBeInTheDocument()
    })
  })

  it('shows an empty state when there are no movimentações', async () => {
    server.use(
      http.get('/api/fundo-reserva/movimentacoes', () =>
        HttpResponse.json({ requestId: 'test-req', timestamp: new Date().toISOString(), data: [] }),
      ),
    )
    renderPage()

    await waitFor(() => {
      expect(screen.getByText(/nenhuma movimentação registrada/i)).toBeInTheDocument()
    })
  })
})
