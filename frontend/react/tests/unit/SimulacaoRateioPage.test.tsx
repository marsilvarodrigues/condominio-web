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
import SimulacaoRateioPage from '@/pages/financeiro/SimulacaoRateioPage'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/simulacao-rateio']}>
          <SimulacaoRateioPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

const gruposResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: [
    { id: 1, nome: 'Manutenção Geral', tipoRateio: 'IGUALITARIO', escopo: 'TODOS', blocoId: null, planoContasId: null, parametrosJson: null },
  ],
}

const simulacaoResponse = {
  requestId: 'test-req',
  timestamp: new Date().toISOString(),
  data: {
    grupoNome: 'Manutenção Geral',
    totalDespesas: 1000,
    linhas: [
      { apartamentoId: 1, apartamentoNumero: '101', blocoNome: 'A', coeficiente: 0.5, valorRateado: 500 },
      { apartamentoId: 2, apartamentoNumero: '102', blocoNome: 'A', coeficiente: 0.5, valorRateado: 500 },
    ],
  },
}

describe('SimulacaoRateioPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })

    server.use(
      http.get('/api/grupos-despesa', () => HttpResponse.json(gruposResponse)),
      http.post('/api/rateio/simular', () => HttpResponse.json(simulacaoResponse)),
    )
  })

  it('shows the placeholder before running a simulation', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('combobox', { name: /grupo de despesa/i })).toBeInTheDocument()
    })
    expect(
      screen.getByText(/configure os parâmetros acima e clique em simular/i),
    ).toBeInTheDocument()
  })

  it('runs a simulation and displays the resulting distribution', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('combobox', { name: /grupo de despesa/i })).toBeInTheDocument()
    })

    await userEvent.click(screen.getByRole('combobox', { name: /grupo de despesa/i }))
    await userEvent.click(await screen.findByRole('option', { name: /manutenção geral/i }))

    await userEvent.click(screen.getByRole('button', { name: /simular/i }))

    await waitFor(() => {
      expect(screen.getByText('101')).toBeInTheDocument()
    })
    expect(screen.getByText('102')).toBeInTheDocument()
    expect(screen.getByText('Total a Ratear')).toBeInTheDocument()
    expect(screen.getByText('Unidades')).toBeInTheDocument()
  })

  it('disables the simular button until a grupo is selected', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('combobox', { name: /grupo de despesa/i })).toBeInTheDocument()
    })

    expect(screen.getByRole('button', { name: /simular/i })).toBeDisabled()
  })

  it('shows an error alert when the simulation request fails', async () => {
    server.use(
      http.post('/api/rateio/simular', () => HttpResponse.json({ message: 'erro' }, { status: 500 })),
    )
    renderPage()

    await waitFor(() => {
      expect(screen.getByRole('combobox', { name: /grupo de despesa/i })).toBeInTheDocument()
    })

    await userEvent.click(screen.getByRole('combobox', { name: /grupo de despesa/i }))
    await userEvent.click(await screen.findByRole('option', { name: /manutenção geral/i }))

    await userEvent.click(screen.getByRole('button', { name: /simular/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao simular rateio/i)).toBeInTheDocument()
    })
  })
})
