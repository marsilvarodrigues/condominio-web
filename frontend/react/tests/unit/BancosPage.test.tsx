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
import BancosPage from '@/pages/financeiro/BancosPage'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/bancos']}>
          <BancosPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('BancosPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })
  })

  it('renders the bank list from the API', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('Itaú')).toBeInTheDocument()
    })
    expect(screen.getByText('341')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    server.use(
      http.post('/api/bancos', () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: { id: 2, codigo: '237', nome: 'Bradesco', ativo: true },
          },
          { status: 201 },
        ),
      ),
    )
    renderPage()
    await waitFor(() => expect(screen.getByText('Itaú')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo banco/i }))

    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText(/código/i), '237')
    await userEvent.type(within(dialog).getByLabelText(/nome/i), 'Bradesco')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows a validation error for invalid input', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('Itaú')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo banco/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/nome/i), 'A')
    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/mínimo 2 caracteres/i)).toBeInTheDocument()
    })
  })

  it('opens and confirms the delete dialog', async () => {
    server.use(
      http.delete('/api/bancos/:id', () => new HttpResponse(null, { status: 204 })),
    )
    renderPage()
    await waitFor(() => expect(screen.getByText('Itaú')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/excluir o banco "itaú"/i)).toBeInTheDocument()

    await userEvent.click(within(dialog).getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
