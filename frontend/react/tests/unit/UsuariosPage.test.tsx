import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { server } from './mocks/server'
import { theme } from '@/theme/theme'
import UsuariosPage from '@/pages/usuarios/UsuariosPage'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { AuthUser, UserDTO } from '@/types'

const BASE = '/api'

function makeUser(roles: string[]): AuthUser {
  return { id: 1, email: 'admin@test.com', roles, condominioIds: [] }
}

function makeUserDTO(overrides: Partial<UserDTO> = {}): UserDTO {
  return {
    id: 1,
    email: 'usuario@test.com',
    name: 'Usuário Teste',
    enabled: true,
    roles: ['ROLE_USER'],
    condominioIds: [],
    activationToken: null,
    activationTokenExpiry: null,
    ...overrides,
  }
}

function mockUsersList(users: UserDTO[]) {
  server.use(
    http.get(`${BASE}/users`, () =>
      HttpResponse.json({
        requestId: 'test-req',
        timestamp: new Date().toISOString(),
        data: {
          content: users,
          totalElements: users.length,
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
        <MemoryRouter initialEntries={['/usuarios']}>
          <UsuariosPage />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('UsuariosPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'tok',
      refreshToken: 'ref',
      user: null,
      activeCondominioId: null,
    })
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN]))
    mockUsersList([makeUserDTO()])
  })

  it('renders the list of users from the API', async () => {
    renderPage()

    await waitFor(() => {
      expect(screen.getByText('Usuário Teste')).toBeInTheDocument()
    })
    expect(screen.getByText('usuario@test.com')).toBeInTheDocument()
    expect(screen.getByText('Ativo')).toBeInTheDocument()
  })

  it('opens the create dialog and submits a valid form', async () => {
    server.use(
      http.post(`${BASE}/users`, () =>
        HttpResponse.json(
          {
            requestId: 'test-req',
            timestamp: new Date().toISOString(),
            data: makeUserDTO({ id: 2, name: 'Novo Usuário', email: 'novo@test.com' }),
          },
          { status: 201 },
        ),
      ),
    )

    renderPage()
    await waitFor(() => expect(screen.getByText('Usuário Teste')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo usuário/i }))
    expect(screen.getByText('Novo Usuário', { selector: '.MuiDialogTitle-root' })).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Nome'), 'Novo Usuário')
    await userEvent.type(screen.getByLabelText('E-mail'), 'novo@test.com')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.queryByText('Novo Usuário', { selector: '.MuiDialogTitle-root' })).not.toBeInTheDocument()
    })
  })

  it('does not submit the create form when required fields are empty', async () => {
    let createCalled = false
    server.use(
      http.post(`${BASE}/users`, () => {
        createCalled = true
        return HttpResponse.json({ requestId: 'test-req', timestamp: new Date().toISOString(), data: makeUserDTO() }, { status: 201 })
      }),
    )

    renderPage()
    await waitFor(() => expect(screen.getByText('Usuário Teste')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /novo usuário/i }))
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    // Required-but-empty fields should block the native required validation from
    // ever reaching the mutation — dialog stays open and no request is fired.
    await waitFor(() => {
      expect(screen.getByText('Novo Usuário', { selector: '.MuiDialogTitle-root' })).toBeInTheDocument()
    })
    expect(createCalled).toBe(false)
  })

  it('deletes a user via the confirm dialog', async () => {
    server.use(
      http.delete(`${BASE}/users/1`, () => new HttpResponse(null, { status: 204 })),
    )

    renderPage()
    await waitFor(() => expect(screen.getByText('Usuário Teste')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))

    expect(screen.getByText('Excluir Usuário')).toBeInTheDocument()
    expect(screen.getByText(/deseja excluir o usuário "usuario@test.com"/i)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(screen.queryByText('Excluir Usuário')).not.toBeInTheDocument()
    })
  })
})
