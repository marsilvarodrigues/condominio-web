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
import CoeficientesPage from '@/pages/financeiro/CoeficientesPage'

const COEFICIENTE = {
  id: 10,
  grupoDespesaId: 1,
  apartamentoId: 5,
  apartamentoNumero: '101',
  blocoNome: 'A',
  coeficiente: 0.0833,
  vigenciaInicio: '2024-01-01',
  vigenciaFim: null,
}

function mockCoeficientesList(coeficientes: unknown[] = [COEFICIENTE]) {
  server.use(
    http.get('/api/grupos-despesa/:id/coeficientes', () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: coeficientes,
      }),
    ),
  )
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/coeficientes']}>
          <CoeficientesPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

async function selectGrupo() {
  const select = screen.getByLabelText(/grupo de despesa/i)
  await userEvent.click(select)
  const option = await screen.findByRole('option', { name: /manutenção geral/i })
  await userEvent.click(option)
}

describe('CoeficientesPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })
    mockCoeficientesList()
  })

  it('prompts to select a grupo de despesa before showing the table', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText(/selecione um grupo de despesa/i)).toBeInTheDocument()
    })
  })

  it('renders the coeficientes list once a grupo is selected', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/grupo de despesa/i)).toBeInTheDocument())

    await selectGrupo()

    await waitFor(() => {
      expect(screen.getByText('101')).toBeInTheDocument()
    })
    expect(screen.getByText('0.0833')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    server.use(
      http.post('/api/grupos-despesa/:id/coeficientes', () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: { ...COEFICIENTE, id: 11, apartamentoId: 6, apartamentoNumero: '102' },
          },
          { status: 201 },
        ),
      ),
    )
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/grupo de despesa/i)).toBeInTheDocument())
    await selectGrupo()
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo coeficiente/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/id do apartamento/i), '6')
    await userEvent.type(within(dialog).getByLabelText(/coeficiente/i), '0.05')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows a validation error for invalid input', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/grupo de despesa/i)).toBeInTheDocument())
    await selectGrupo()
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo coeficiente/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(within(dialog).getByText(/selecione um apartamento/i)).toBeInTheDocument()
    })
  })

  it('opens and confirms the delete dialog', async () => {
    server.use(
      http.delete('/api/grupos-despesa/:id/coeficientes/:coefId', () => new HttpResponse(null, { status: 204 })),
    )
    renderPage()
    await waitFor(() => expect(screen.getByLabelText(/grupo de despesa/i)).toBeInTheDocument())
    await selectGrupo()
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/excluir o coeficiente do apartamento 101/i)).toBeInTheDocument()

    await userEvent.click(within(dialog).getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
