import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ThemeProvider } from '@mui/material'
import { theme } from '@/theme/theme'
import { Sidebar } from '@/components/layout/Sidebar'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { AuthUser } from '@/types'

function makeUser(roles: string[]): AuthUser {
  return { id: 1, email: 'u@test.com', roles, condominioIds: [1] }
}

function renderSidebar() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter>
        <Sidebar mobileOpen={false} onMobileClose={() => {}} />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

// Sidebar renders two Drawers (mobile + desktop), so each label appears twice.
// Use getAllByText and check length > 0, or queryAllByText for absences.
const hasLabel = (label: string) => screen.queryAllByText(label).length > 0
const hasNoLabel = (label: string) => screen.queryAllByText(label).length === 0

describe('Sidebar — visibilidade por perfil', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'tok',
      refreshToken: 'ref',
      user: null,
      activeCondominioId: 1,
    })
  })

  it('ADMIN vê Dashboard, Condomínios, Hierarquia, Usuários e Financeiro', () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN]))
    renderSidebar()

    expect(hasLabel('Dashboard')).toBe(true)
    expect(hasLabel('Condomínios')).toBe(true)
    expect(hasLabel('Hierarquia')).toBe(true)
    expect(hasLabel('Usuários')).toBe(true)
    expect(hasLabel('Financeiro')).toBe(true)
    expect(hasLabel('Cobranças')).toBe(true)
  })

  it('ADMIN não vê Comunicados, Reservas nem Meus Imóveis', () => {
    useAuthStore.getState().setUser(makeUser([ROLES.ADMIN]))
    renderSidebar()

    expect(hasNoLabel('Comunicados')).toBe(true)
    expect(hasNoLabel('Reservas')).toBe(true)
    expect(hasNoLabel('Meus Imóveis')).toBe(true)
  })

  it('SINDICO vê Financeiro e Cobranças mas não Usuários', () => {
    useAuthStore.getState().setUser(makeUser([ROLES.SINDICO]))
    renderSidebar()

    expect(hasLabel('Dashboard')).toBe(true)
    expect(hasLabel('Condomínios')).toBe(true)
    expect(hasLabel('Financeiro')).toBe(true)
    expect(hasLabel('Cobranças')).toBe(true)
    expect(hasNoLabel('Usuários')).toBe(true)
  })

  it('PROPRIETARIO vê apenas Dashboard, Hierarquia e Meus Imóveis', () => {
    useAuthStore.getState().setUser(makeUser([ROLES.PROPRIETARIO]))
    renderSidebar()

    expect(hasLabel('Dashboard')).toBe(true)
    expect(hasLabel('Hierarquia')).toBe(true)
    expect(hasLabel('Meus Imóveis')).toBe(true)
    expect(hasNoLabel('Financeiro')).toBe(true)
    expect(hasNoLabel('Cobranças')).toBe(true)
    expect(hasNoLabel('Usuários')).toBe(true)
    expect(hasNoLabel('Condomínios')).toBe(true)
  })

  it('MORADOR vê Dashboard, Hierarquia, Comunicados e Reservas', () => {
    useAuthStore.getState().setUser(makeUser([ROLES.MORADOR]))
    renderSidebar()

    expect(hasLabel('Dashboard')).toBe(true)
    expect(hasLabel('Hierarquia')).toBe(true)
    expect(hasLabel('Comunicados')).toBe(true)
    expect(hasLabel('Reservas')).toBe(true)
    expect(hasNoLabel('Financeiro')).toBe(true)
    expect(hasNoLabel('Usuários')).toBe(true)
    expect(hasNoLabel('Meus Imóveis')).toBe(true)
  })
})
