import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { theme } from '@/theme/theme'
import PessoasPage from '@/pages/pessoas/PessoasPage'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { AuthUser, PessoaDTO } from '@/types'

const BASE = '/api'

function makeUser(roles: string[]): AuthUser {
  return { id: 1, email: 'admin@test.com', roles, condominioIds: [1] }
}

function makePessoa(overrides: Partial<PessoaDTO> = {}): PessoaDTO {
  return {
    id: 1,
    nome: 'João da Silva',
    tipo: 'MORADOR',
    cpf: '12345678901',
    email: 'joao@test.com',
    telefone: '11999999999',
    apartamentoId: 1,
    apartamentoNumero: '101',
    userId: null,
    createdAt: null,
    updatedAt: null,
    ...overrides,
  }
}

function mockPessoasList(pessoas: PessoaDTO[]) {
  server.use(
    http.get(`${BASE}/pessoas`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: {
          content: pessoas,
          totalElements: pessoas.length,
          totalPages: 1,
          size: 20,
          number: 0,
        },
      }),
    ),
  )
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/pessoas']}>
          <PessoasPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('PessoasPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'tok',
      refreshToken: 'ref',
      user: null,
      activeCondominioId: 1,
    })
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN]))
    mockPessoasList([makePessoa()])
  })

  it('renders the list of pessoas from the API', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('João da Silva')).toBeInTheDocument()
    })
    expect(screen.getByText('joao@test.com')).toBeInTheDocument()
    expect(screen.getByText('Apt 101')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    server.use(
      http.post(`${BASE}/pessoas`, () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: makePessoa({ id: 2, nome: 'Maria Souza', email: 'maria@test.com', cpf: '98765432100' }),
          },
          { status: 201 },
        ),
      ),
    )

    renderPage()
    await waitFor(() => expect(screen.getByText('João da Silva')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo morador/i }))
    expect(screen.getByText('Novo Morador', { selector: '.MuiDialogTitle-root' })).toBeInTheDocument()

    const dialog = within(screen.getByRole('dialog'))
    await userEvent.type(dialog.getByLabelText('Nome'), 'Maria Souza')
    await userEvent.type(dialog.getByLabelText('E-mail'), 'maria@test.com')
    await userEvent.type(dialog.getByLabelText('CPF'), '98765432100')

    await userEvent.click(dialog.getByRole('button', { name: /salvar/i }))

    // Dialog closes on successful create.
    await waitFor(() => {
      expect(screen.queryByText('Novo Morador', { selector: '.MuiDialogTitle-root' })).not.toBeInTheDocument()
    })
  })

  it('shows validation errors for invalid input', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('João da Silva')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo morador/i }))
    const dialog = within(await screen.findByRole('dialog'))
    const emailInput = await dialog.findByLabelText('E-mail')
    await userEvent.type(emailInput, 'not-an-email')

    // The Salvar button lives in DialogActions and submits via the `form`
    // attribute (it's outside the <form> element). jsdom does not reliably
    // dispatch the implicit submit on a userEvent.click in that cross-element
    // setup, so we submit the form directly — equivalent to what the browser
    // does when the button is clicked.
    fireEvent.submit(document.getElementById('morador-form')!)

    await waitFor(() => {
      expect(dialog.getByText('Nome obrigatório')).toBeInTheDocument()
      expect(dialog.getByText('E-mail inválido')).toBeInTheDocument()
    })
  })

  it('deletes a pessoa via the confirm dialog', async () => {
    server.use(
      http.delete(`${BASE}/pessoas/1`, () => new HttpResponse(null, { status: 204 })),
    )

    renderPage()
    await waitFor(() => expect(screen.getByText('João da Silva')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    expect(screen.getByText('Excluir Morador')).toBeInTheDocument()
    expect(screen.getByText(/deseja excluir joão da silva/i)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByText('Excluir Morador')).not.toBeInTheDocument()
    })
  })
})
