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
import ContasBancariasPage from '@/pages/financeiro/ContasBancariasPage'

const CONTA = {
  id: 1,
  agencia: '0001',
  conta: '123456-7',
  tipo: 'CORRENTE',
  descricao: 'Conta principal',
  bancoId: 1,
  bancoNome: 'Itaú',
  saldo: 1500.5,
}

function mockContasList(contas: unknown[] = [CONTA]) {
  server.use(
    http.get('/api/contas-bancarias', () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: { content: contas },
      }),
    ),
  )
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/contas-bancarias']}>
          <ContasBancariasPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('ContasBancariasPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })
    mockContasList()
  })

  it('renders the bank account list from the API', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('0001')).toBeInTheDocument()
    })
    expect(screen.getByText('123456-7')).toBeInTheDocument()
    expect(screen.getByText('Itaú')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    server.use(
      http.post('/api/contas-bancarias', () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: { ...CONTA, id: 2, agencia: '0002', conta: '999999-9' },
          },
          { status: 201 },
        ),
      ),
    )
    renderPage()
    await waitFor(() => expect(screen.getByText('0001')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nova conta/i }))
    const dialog = await screen.findByRole('dialog')

    const bancoSelect = within(dialog).getByLabelText(/banco/i)
    await userEvent.click(bancoSelect)
    const option = await screen.findByRole('option', { name: /itaú/i })
    await userEvent.click(option)

    await userEvent.type(within(dialog).getByLabelText(/agência/i), '0002')
    await userEvent.type(within(dialog).getByLabelText(/^conta$/i), '999999-9')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows a validation error when no banco is selected', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('0001')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nova conta/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/agência/i), '0002')
    await userEvent.type(within(dialog).getByLabelText(/^conta$/i), '999999-9')
    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    // bancoId left unselected coerces to NaN, which zod rejects with its built-in
    // invalid_type message (the custom "Selecione um banco" message only fires for
    // a defined non-positive number) — assert the dialog stays open with the select
    // marked as errored (MUI applies the Mui-error class to the combobox, not aria-invalid).
    await waitFor(() => {
      expect(within(dialog).getByLabelText(/banco/i)).toHaveClass('Mui-error')
    })
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('opens and confirms the delete dialog', async () => {
    server.use(
      http.delete('/api/contas-bancarias/:id', () => new HttpResponse(null, { status: 204 })),
    )
    renderPage()
    await waitFor(() => expect(screen.getByText('0001')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/excluir a conta 0001\/123456-7/i)).toBeInTheDocument()

    await userEvent.click(within(dialog).getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
