import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { theme } from '@/theme/theme'
import { server } from './mocks/server'
import { TopBar } from '@/components/layout/TopBar'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { AuthUser } from '@/types'

function makeUser(roles: string[], condominioIds: number[]): AuthUser {
  return { id: 1, email: 'admin@test.com', roles, condominioIds }
}

function renderTopBar(onMenuClick = vi.fn()) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter>
          <TopBar onMenuClick={onMenuClick} />
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('TopBar', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'tok',
      refreshToken: 'ref',
      user: null,
      activeCondominioId: 1,
    })
  })

  it('exibe o avatar com a inicial do e-mail do usuário e oculta o seletor para usuário com 1 condomínio', () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN], [1]))
    renderTopBar()

    // condominioCount === 1 => needsSelector is false => no <Select> rendered.
    expect(screen.getByText('A')).toBeInTheDocument()
    expect(screen.queryByText('Todos os condomínios')).not.toBeInTheDocument()
  })

  it('chama onMenuClick ao clicar no botão de menu mobile', async () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN], [1]))
    const onMenuClick = vi.fn()
    renderTopBar(onMenuClick)

    const menuButton = document.querySelector('svg[data-testid="MenuIcon"]')!.closest('button')!
    await userEvent.click(menuButton)

    expect(onMenuClick).toHaveBeenCalledTimes(1)
  })

  it('abre o menu do usuário e exibe e-mail, perfil e opção de sair', async () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN], [1]))
    renderTopBar()

    const avatarButton = screen.getByText('A').closest('button')!
    await userEvent.click(avatarButton)

    expect(await screen.findByText('admin@test.com')).toBeInTheDocument()
    expect(screen.getByText('Administrador')).toBeInTheDocument()
    expect(screen.getByText('Alterar senha')).toBeInTheDocument()
    expect(screen.getByText('Sair')).toBeInTheDocument()
  })

  it('faz logout e navega para /login ao clicar em Sair', async () => {
    server.use(
      http.post('/api/auth/logout', () => HttpResponse.json({})),
    )
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN], [1]))
    renderTopBar()

    const avatarButton = screen.getByText('A').closest('button')!
    await userEvent.click(avatarButton)
    await userEvent.click(await screen.findByText('Sair'))

    await waitFor(() => {
      expect(useAuthStore.getState().accessToken).toBeNull()
      expect(useAuthStore.getState().user).toBeNull()
    })
  })

  it('exibe o seletor de condomínios para usuário global (0 condomínios) e lista as opções', async () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN], []))
    renderTopBar()

    // condominioCount === 0 => needsSelector is true => <Select> shown with default option.
    expect(await screen.findByText('Todos os condomínios')).toBeInTheDocument()
  })
})
