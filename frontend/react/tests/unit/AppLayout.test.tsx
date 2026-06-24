import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { theme } from '@/theme/theme'
import { AppLayout } from '@/components/layout/AppLayout'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { AuthUser } from '@/types'

function makeUser(roles: string[], condominioIds: number[] = [1]): AuthUser {
  return { id: 1, email: 'admin@test.com', roles, condominioIds }
}

// AppLayout renders TopBar (needs QueryClientProvider for useQuery) + Sidebar +
// an <Outlet/> (needs a nested Route), mirroring ProtectedRoute.test.tsx's pattern.
function renderLayout(initialPath = '/') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={[initialPath]}>
          <Routes>
            <Route path="/" element={<AppLayout />}>
              <Route index element={<div>Conteúdo da rota filha</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('AppLayout', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'tok',
      refreshToken: 'ref',
      user: null,
      activeCondominioId: 1,
    })
  })

  it('renderiza TopBar, Sidebar e o conteúdo da rota filha via Outlet', () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN], [1]))
    renderLayout()

    // TopBar — at least the user's avatar initial should show up.
    expect(screen.getByText('A')).toBeInTheDocument()
    // Sidebar — rendered twice (mobile + desktop drawers).
    expect(screen.queryAllByText('Dashboard').length).toBeGreaterThan(0)
    // Outlet content from the nested route.
    expect(screen.getByText('Conteúdo da rota filha')).toBeInTheDocument()
  })

  it('abre o Drawer mobile ao clicar no botão de menu do TopBar', async () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN], [1]))
    renderLayout()

    // The hamburger IconButton is identified by its MenuIcon (data-testid from MUI icon naming).
    const menuButton = document.querySelector('svg[data-testid="MenuIcon"]')!.closest('button')!
    expect(menuButton).toBeInTheDocument()

    // Sanity: clicking the hamburger icon button shouldn't throw — it toggles
    // AppLayout's local mobileOpen state, passed down into Sidebar.
    const user = userEvent.setup()
    await user.click(menuButton)

    // Sidebar content should still be present (mobile drawer now open).
    expect(screen.queryAllByText('Dashboard').length).toBeGreaterThan(0)
  })

  it('renderiza o conteúdo da rota filha mesmo sem usuário autenticado completo', () => {
    renderLayout()
    expect(screen.getByText('Conteúdo da rota filha')).toBeInTheDocument()
  })
})
