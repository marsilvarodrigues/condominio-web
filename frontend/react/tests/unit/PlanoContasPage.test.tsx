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
import PlanoContasPage from '@/pages/financeiro/PlanoContasPage'

const CONTA = {
  id: 1,
  codigo: '3.1',
  nome: 'Despesas Administrativas',
  tipo: 'DESPESA',
  contaParenteId: null,
  contaParenteNome: null,
  nivel: 1,
}

function mockContasList(contas: unknown[] = [CONTA]) {
  server.use(
    http.get('/api/plano-contas', () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: contas,
      }),
    ),
  )
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/plano-contas']}>
          <PlanoContasPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('PlanoContasPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })
    mockContasList()
  })

  it('renders the plano de contas list from the API', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('Despesas Administrativas')).toBeInTheDocument()
    })
    expect(screen.getByText('3.1')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    server.use(
      http.post('/api/plano-contas', () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: { ...CONTA, id: 2, codigo: '3.2', nome: 'Despesas Gerais' },
          },
          { status: 201 },
        ),
      ),
    )
    renderPage()
    await waitFor(() => expect(screen.getByText('Despesas Administrativas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nova conta/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/código/i), '3.2')
    await userEvent.type(within(dialog).getByLabelText(/nome/i), 'Despesas Gerais')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows a validation error for invalid input', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('Despesas Administrativas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nova conta/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/nome/i), 'A')
    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/código obrigatório/i)).toBeInTheDocument()
    })
  })

  it('opens and confirms the delete dialog', async () => {
    server.use(
      http.delete('/api/plano-contas/:id', () => new HttpResponse(null, { status: 204 })),
    )
    renderPage()
    await waitFor(() => expect(screen.getByText('Despesas Administrativas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/excluir a conta "despesas administrativas"/i)).toBeInTheDocument()

    await userEvent.click(within(dialog).getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
