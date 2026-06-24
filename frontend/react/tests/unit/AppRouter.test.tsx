import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { theme } from '@/theme/theme'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import { AppRouter } from '@/router/AppRouter'
import type { AuthUser } from '@/types'

function makeUser(roles: string[]): AuthUser {
  return { id: 1, email: 'admin@test.com', roles, condominioIds: [1] }
}

// AppRouter renders its own hardcoded <BrowserRouter>, so it must NOT be wrapped in
// another router here. Navigation is driven by mutating real jsdom history before render.
function renderAppRouter() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <AppRouter />
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('AppRouter', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/')
    useAuthStore.setState({
      accessToken: null,
      refreshToken: null,
      user: null,
      activeCondominioId: null,
    })
  })

  afterEach(() => {
    window.history.pushState({}, '', '/')
  })

  it('exibe o loader de bootstrap antes de resolver a autenticação', () => {
    // No refreshToken => bootstrap() resolves on the same microtask queue, but
    // synchronously right after render (before any await) isBootstrapping is still true.
    renderAppRouter()
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('redireciona usuário não autenticado de uma rota protegida para /login', async () => {
    window.history.pushState({}, '', '/condominios')
    renderAppRouter()

    expect(await screen.findByText('CondoGest')).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toBeInTheDocument()
  })

  it('renderiza o conteúdo do dashboard quando autenticado', async () => {
    useAuthStore.setState({ accessToken: 'tok', refreshToken: 'ref' })
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN]))

    renderAppRouter()

    expect(await screen.findByText('Administração da Plataforma')).toBeInTheDocument()
  })
})
