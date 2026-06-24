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
import OrcamentoAnualPage from '@/pages/financeiro/OrcamentoAnualPage'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/orcamento-anual']}>
          <OrcamentoAnualPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

const orcamentoResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: [
    {
      id: 1,
      ano: new Date().getFullYear(),
      contaNome: 'Manutenção Predial',
      contaId: 10,
      grupoDespesaId: 1,
      grupoDespesaNome: 'Manutenção Geral',
      valorOrcado: 12000,
      valorRealizado: 6000,
      statusRateio: 'RATEADO',
    },
  ],
}

describe('OrcamentoAnualPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })

    server.use(
      http.get('/api/orcamentos', () => HttpResponse.json(orcamentoResponse)),
    )
  })

  it('lists budget items and shows the total orçado', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Manutenção Predial')).toBeInTheDocument()
    })
    expect(screen.getByText(/total orçado para/i)).toBeInTheDocument()
    expect(screen.getAllByText(/R\$\s*12\.000,00/).length).toBeGreaterThan(0)
  })

  it('opens the add item dialog and submits a new item', async () => {
    server.use(
      http.post('/api/orcamentos/:ano/itens', () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: { id: 2, ano: new Date().getFullYear(), contaNome: 'Limpeza', contaId: 11, grupoDespesaId: null, grupoDespesaNome: null, valorOrcado: 2000, valorRealizado: 0, statusRateio: null },
          },
          { status: 201 },
        ),
      ),
    )
    renderPage()

    await waitFor(() => expect(screen.getByText('Manutenção Predial')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /adicionar item/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/id da conta/i), '11')
    await userEvent.type(within(dialog).getByLabelText(/valor orçado/i), '2000')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('removes an item after confirming the delete dialog', async () => {
    server.use(
      http.delete('/api/orcamentos/:ano/itens/:itemId', () => new HttpResponse(null, { status: 204 })),
    )
    renderPage()

    await waitFor(() => expect(screen.getByText('Manutenção Predial')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /remover/i }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/remover "manutenção predial" do orçamento/i)).toBeInTheDocument()

    await userEvent.click(within(dialog).getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows an empty state when there are no items for the year', async () => {
    server.use(
      http.get('/api/orcamentos', () =>
        HttpResponse.json({ requestId: 'test-req', timestamp: new Date().toISOString(), data: [] }),
      ),
    )
    renderPage()

    await waitFor(() => {
      expect(screen.getByText(/nenhum item orçado para este ano/i)).toBeInTheDocument()
    })
  })
})
