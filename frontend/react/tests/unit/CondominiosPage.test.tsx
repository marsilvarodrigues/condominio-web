import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { theme } from '@/theme/theme'
import CondominiosPage from '@/pages/condominios/CondominiosPage'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { AuthUser, CondominioDTO } from '@/types'

const BASE = '/api'

function makeUser(roles: string[]): AuthUser {
  return { id: 1, email: 'admin@test.com', roles, condominioIds: [1] }
}

function makeCondominio(overrides: Partial<CondominioDTO> = {}): CondominioDTO {
  return {
    id: 1,
    nome: 'Condomínio Jardim',
    cnpj: '12345678000195',
    email: 'contato@jardim.com',
    endereco: {
      logradouro: 'Rua das Flores, 100',
      cep: '01310100',
      cidade: 'São Paulo',
      estado: { id: 35, nome: 'São Paulo', uf: 'SP' },
    },
    ...overrides,
  }
}

function mockCondominiosList(condominios: CondominioDTO[]) {
  server.use(
    http.get(`${BASE}/condominios`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: condominios,
      }),
    ),
  )
}

function mockEstadosSearch() {
  server.use(
    http.get(`${BASE}/estados`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: [{ id: 35, nome: 'São Paulo', uf: 'SP' }],
      }),
    ),
  )
}

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/condominios']}>
          <CondominiosPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('CondominiosPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'tok',
      refreshToken: 'ref',
      user: null,
      activeCondominioId: 1,
    })
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN]))
    mockCondominiosList([makeCondominio()])
    mockEstadosSearch()
  })

  it('renders the list of condominios from the API', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Condomínio Jardim')).toBeInTheDocument()
    })
    expect(screen.getByText('contato@jardim.com')).toBeInTheDocument()
    expect(screen.getByText('São Paulo / SP')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    server.use(
      http.post(`${BASE}/condominios`, () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: makeCondominio({ id: 2, nome: 'Condomínio Novo' }),
          },
          { status: 201 },
        ),
      ),
    )

    renderPage()
    await waitFor(() => expect(screen.getByText('Condomínio Jardim')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo condomínio/i }))
    expect(screen.getByText('Novo Condomínio', { selector: '.MuiDialogTitle-root' })).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Nome'), 'Condomínio Novo')
    await userEvent.type(screen.getByLabelText('CNPJ'), '98765432000100')
    await userEvent.type(screen.getByLabelText('E-mail'), 'novo@condo.com')
    await userEvent.type(screen.getByLabelText('Logradouro'), 'Av. Central, 500')
    await userEvent.type(screen.getByLabelText('CEP'), '02020020')
    await userEvent.type(screen.getByLabelText('Cidade'), 'São Paulo')

    const estadoInput = screen.getByLabelText('Estado')
    await userEvent.type(estadoInput, 'SP')
    const option = await screen.findByText(/SP – São Paulo/i)
    await userEvent.click(option)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByText('Novo Condomínio', { selector: '.MuiDialogTitle-root' })).not.toBeInTheDocument()
    })
  })

  it('shows validation errors for invalid input', async () => {
    renderPage()
    await waitFor(() => expect(screen.getByText('Condomínio Jardim')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo condomínio/i }))
    await userEvent.type(screen.getByLabelText('Nome'), 'A')
    await userEvent.type(screen.getByLabelText('E-mail'), 'not-an-email')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText('Mínimo 3 caracteres')).toBeInTheDocument()
      expect(screen.getByText('E-mail inválido')).toBeInTheDocument()
      // estado defaults to undefined (untouched field) on create, which zod
      // reports as a type error ("Required") rather than the custom refine
      // message ("Selecione um estado") that fires only for an explicit null.
      expect(screen.getByText('Required')).toBeInTheDocument()
    })
  })

  it('deletes a condominio via the confirm dialog when user has ADMIN role', async () => {
    server.use(
      http.delete(`${BASE}/condominios/1`, () => new HttpResponse(null, { status: 204 })),
    )

    renderPage()
    await waitFor(() => expect(screen.getByText('Condomínio Jardim')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    expect(screen.getByText('Excluir Condomínio')).toBeInTheDocument()
    expect(screen.getByText(/deseja excluir o condomínio "condomínio jardim"/i)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByText('Excluir Condomínio')).not.toBeInTheDocument()
    })
  })

  it('hides the delete action for non-ADMIN roles (e.g. SINDICO)', async () => {
    useAuthStore.getState().setUser(makeUser([ROLES.SINDICO]))

    renderPage()
    await waitFor(() => expect(screen.getByText('Condomínio Jardim')).toBeInTheDocument())

    expect(screen.queryByRole('button', { name: /excluir/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /editar/i })).toBeInTheDocument()
  })
})
