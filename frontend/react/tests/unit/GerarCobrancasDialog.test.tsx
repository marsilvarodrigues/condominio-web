import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { theme } from '@/theme/theme'
import { useAuthStore } from '@/store/authStore'
import { server } from './mocks/server'
import { GerarCobrancasDialog } from '@/pages/cobrancas/GerarCobrancasDialog'

const BASE = '/api'

function renderDialog(open = true, onClose = vi.fn()) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <GerarCobrancasDialog open={open} onClose={onClose} />
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

function mockExecucoes() {
  server.use(
    http.get(`${BASE}/rateio/execucoes`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: {
          content: [
            {
              id: 10,
              despesaId: 1,
              despesaDescricao: 'Manutenção',
              grupoDespesaId: 1,
              tipoExecucao: 'IGUALITARIO',
              dataExecucao: '2026-06-01T10:00:00',
              despesaTotal: 1500.5,
              totalUnidades: 10,
              totalCotas: 10,
              status: 'SUCESSO',
              erroMensagem: null,
            },
          ],
          totalElements: 1,
          totalPages: 1,
          size: 50,
          number: 0,
        },
      }),
    ),
  )
}

describe('GerarCobrancasDialog', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })
  })

  it('renders the dialog with the list of available rateio executions', async () => {
    mockExecucoes()
    renderDialog()

    expect(screen.getByText(/gerar cobranças/i)).toBeInTheDocument()

    await userEvent.click(screen.getByLabelText(/execução de rateio/i))
    const listbox = await screen.findByRole('listbox')
    expect(within(listbox).getByText(/execução #10/i)).toBeInTheDocument()
  })

  it('generates charges successfully on valid submit', async () => {
    mockExecucoes()
    server.use(
      http.post(`${BASE}/cobrancas/gerar`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [],
        }),
      ),
    )
    const onClose = vi.fn()
    renderDialog(true, onClose)

    await userEvent.click(screen.getByLabelText(/execução de rateio/i))
    const listbox = await screen.findByRole('listbox')
    await userEvent.click(within(listbox).getByText(/execução #10/i))

    const vencimentoInput = screen.getByLabelText(/data de vencimento/i)
    await userEvent.type(vencimentoInput, '2026-07-10')

    await userEvent.click(screen.getByRole('button', { name: /gerar e enviar/i }))

    await waitFor(() => {
      expect(onClose).toHaveBeenCalled()
    })
    await waitFor(() => {
      expect(screen.getByText(/cobranças geradas com sucesso/i)).toBeInTheDocument()
    })
  })

  it('shows a validation error when submitting without selecting an execution', async () => {
    mockExecucoes()
    renderDialog()

    await waitFor(() => {
      expect(screen.queryByText(/carregando/i)).not.toBeInTheDocument()
    })

    await userEvent.click(screen.getByRole('button', { name: /gerar e enviar/i }))

    await waitFor(() => {
      expect(screen.getByText(/selecione uma execução/i)).toBeInTheDocument()
    })
  })

  it('shows an empty state message when there are no successful rateio executions', async () => {
    server.use(
      http.get(`${BASE}/rateio/execucoes`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: { content: [], totalElements: 0, totalPages: 0, size: 50, number: 0 },
        }),
      ),
    )
    renderDialog()

    await userEvent.click(screen.getByLabelText(/execução de rateio/i))
    const listbox = await screen.findByRole('listbox')
    expect(within(listbox).getByText(/nenhuma execução disponível/i)).toBeInTheDocument()
  })

  it('shows an API error message when the mutation fails', async () => {
    mockExecucoes()
    server.use(
      http.post(`${BASE}/cobrancas/gerar`, () =>
        HttpResponse.json({ message: 'Erro ao gerar cobranças.' }, { status: 400 }),
      ),
    )
    renderDialog()

    await userEvent.click(screen.getByLabelText(/execução de rateio/i))
    const listbox = await screen.findByRole('listbox')
    await userEvent.click(within(listbox).getByText(/execução #10/i))

    const vencimentoInput = screen.getByLabelText(/data de vencimento/i)
    await userEvent.type(vencimentoInput, '2026-07-10')

    await userEvent.click(screen.getByRole('button', { name: /gerar e enviar/i }))

    // The client doesn't extract a custom backend message from the error response;
    // axios surfaces its own generic message for non-2xx responses (see src/api/client.ts).
    await waitFor(() => {
      expect(screen.getByText(/request failed with status code 400/i)).toBeInTheDocument()
    })
  })
})
