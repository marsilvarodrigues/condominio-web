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
import GruposDespesaPage from '@/pages/financeiro/GruposDespesaPage'

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/financeiro/grupos-despesa']}>
          <GruposDespesaPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('GruposDespesaPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'token',
      refreshToken: 'refresh',
      user: { id: 1, email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [1] },
      activeCondominioId: 1,
    })
  })

  it('renders the grupo de despesa list from the API', async () => {
    renderPage()
    await waitFor(() => {
      expect(screen.getByText('Manutenção Geral')).toBeInTheDocument()
    })
    expect(screen.getByText('Todos os apartamentos')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('Manutenção Geral')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo grupo/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/nome/i), 'Novo Grupo de Teste')

    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows a validation error for invalid input', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('Manutenção Geral')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo grupo/i }))
    const dialog = await screen.findByRole('dialog')

    await userEvent.type(within(dialog).getByLabelText(/nome/i), 'A')
    await userEvent.click(within(dialog).getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(within(dialog).getByLabelText(/nome/i)).toBeInvalid()
    })
  })

  it('opens and confirms the delete dialog', async () => {
    server.use(
      http.delete('/api/grupos-despesa/:id', () => new HttpResponse(null, { status: 204 })),
    )
    renderPage()
    await waitFor(() => expect(screen.getByText('Manutenção Geral')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/excluir o grupo "manutenção geral"/i)).toBeInTheDocument()

    await userEvent.click(within(dialog).getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
