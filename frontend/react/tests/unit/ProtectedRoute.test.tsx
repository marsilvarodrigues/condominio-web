import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ThemeProvider } from '@mui/material'
import { theme } from '@/theme/theme'
import { ProtectedRoute } from '@/router/ProtectedRoute'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { AuthUser } from '@/types'

function makeUser(roles: string[]): AuthUser {
  return { id: 1, email: 'u@test.com', roles, condominioIds: [1] }
}

function renderWithRoute(element: React.ReactNode, initialPath = '/protected') {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route path="/protected" element={element} />
        </Routes>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: null,
      refreshToken: null,
      user: null,
      activeCondominioId: null,
    })
  })

  it('redireciona para /login quando não autenticado', () => {
    renderWithRoute(
      <ProtectedRoute>
        <div>Conteúdo protegido</div>
      </ProtectedRoute>,
    )
    expect(screen.getByText('Login Page')).toBeInTheDocument()
    expect(screen.queryByText('Conteúdo protegido')).not.toBeInTheDocument()
  })

  it('exibe AcessoNegadoPage quando perfil insuficiente', () => {
    useAuthStore.setState({ accessToken: 'tok' })
    useAuthStore.getState().setUser(makeUser([ROLES.MORADOR]))

    renderWithRoute(
      <ProtectedRoute requiredRoles={[ROLES.ADMIN, ROLES.SINDICO]}>
        <div>Página restrita</div>
      </ProtectedRoute>,
    )

    expect(screen.getByText(/acesso não autorizado/i)).toBeInTheDocument()
    expect(screen.queryByText('Página restrita')).not.toBeInTheDocument()
  })

  it('exibe conteúdo quando role está na lista requiredRoles', () => {
    useAuthStore.setState({ accessToken: 'tok' })
    useAuthStore.getState().setUser(makeUser([ROLES.SINDICO]))

    renderWithRoute(
      <ProtectedRoute requiredRoles={[ROLES.ADMIN, ROLES.SINDICO]}>
        <div>Página para admin/sindico</div>
      </ProtectedRoute>,
    )

    expect(screen.getByText('Página para admin/sindico')).toBeInTheDocument()
  })

  it('mantém compatibilidade com requiredRole singular', () => {
    useAuthStore.setState({ accessToken: 'tok' })
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN]))

    renderWithRoute(
      <ProtectedRoute requiredRole={ROLES.ADMIN}>
        <div>Somente admin</div>
      </ProtectedRoute>,
    )

    expect(screen.getByText('Somente admin')).toBeInTheDocument()
  })

  it('bloqueia requiredRole singular quando perfil não bate', () => {
    useAuthStore.setState({ accessToken: 'tok' })
    useAuthStore.getState().setUser(makeUser([ROLES.SINDICO]))

    renderWithRoute(
      <ProtectedRoute requiredRole={ROLES.ADMIN}>
        <div>Somente admin</div>
      </ProtectedRoute>,
    )

    expect(screen.getByText(/acesso não autorizado/i)).toBeInTheDocument()
    expect(screen.queryByText('Somente admin')).not.toBeInTheDocument()
  })

  it('exibe conteúdo sem restrição de role quando autenticado', () => {
    useAuthStore.setState({ accessToken: 'tok' })
    useAuthStore.getState().setUser(makeUser([ROLES.MORADOR]))

    renderWithRoute(
      <ProtectedRoute>
        <div>Conteúdo para todos</div>
      </ProtectedRoute>,
    )

    expect(screen.getByText('Conteúdo para todos')).toBeInTheDocument()
  })
})
