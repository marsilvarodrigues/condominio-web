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
import ExecucoesRateioPage from '@/pages/financeiro/ExecucoesRateioPage'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/execucoes-rateio']}>
          <ExecucoesRateioPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

const execucoesResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: {
    content: [
      {
        id: 1,
        despesaId: 10,
        despesaDescricao: 'Manutenção do elevador',
        grupoDespesaId: 1,
        tipoExecucao: 'MANUAL',
        dataExecucao: '2026-06-01T10:00:00Z',
        despesaTotal: 1500,
        totalUnidades: 10,
        totalCotas: 10,
        status: 'SUCESSO',
        erroMensagem: null,
        lancamentos: [
          {
            id: 100,
            apartamentoId: 1,
            apartamentoNumero: '101',
            blocoNome: 'A',
            coeficiente: 0.1,
            valor: 150,
            status: 'PENDENTE',
          },
        ],
      },
    ],
    totalElements: 1,
    totalPages: 1,
    size: 20,
    number: 0,
  },
}

describe('ExecucoesRateioPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })

    server.use(
      http.get('/api/rateio/execucoes', () => HttpResponse.json(execucoesResponse)),
    )
  })

  it('lists rateio executions from the API', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Manutenção do elevador')).toBeInTheDocument()
    })
    expect(screen.getByText('Sucesso')).toBeInTheDocument()
    expect(screen.getByText('Manual')).toBeInTheDocument()
  })

  it('expands a row to show its lançamentos detail', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Manutenção do elevador')).toBeInTheDocument()
    })

    expect(screen.queryByText('101')).not.toBeInTheDocument()

    const expandButtons = screen.getAllByRole('button')
    const toggle = expandButtons.find((b) => b.querySelector('svg[data-testid="ExpandMoreIcon"]'))
    expect(toggle).toBeTruthy()
    await userEvent.click(toggle!)

    await waitFor(() => {
      expect(screen.getByText('101')).toBeInTheDocument()
    })
  })

  it('shows a loading message while fetching executions', () => {
    server.use(
      http.get('/api/rateio/execucoes', async () => {
        await new Promise((resolve) => setTimeout(resolve, 50))
        return HttpResponse.json(execucoesResponse)
      }),
    )
    renderPage()
    expect(screen.getByText(/carregando execuções/i)).toBeInTheDocument()
  })

  it('shows an empty state when there are no executions', async () => {
    server.use(
      http.get('/api/rateio/execucoes', () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 },
        }),
      ),
    )
    renderPage()

    await waitFor(() => {
      expect(screen.getByText(/nenhuma execução registrada/i)).toBeInTheDocument()
    })
  })

  it('triggers a refresh when clicking the refresh button', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Manutenção do elevador')).toBeInTheDocument()
    })

    await userEvent.click(screen.getByRole('button', { name: /atualizar/i }))

    await waitFor(() => {
      expect(screen.getByText('Manutenção do elevador')).toBeInTheDocument()
    })
  })
})
