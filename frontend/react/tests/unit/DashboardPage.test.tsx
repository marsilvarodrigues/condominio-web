import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Suspense } from 'react'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import DashboardPage from '@/pages/dashboard/DashboardPage'

function setUser(roles: string[]) {
  useAuthStore.setState({
    user: { email: 'test@test.com', roles, condominioIds: [] },
    accessToken: 'token',
    refreshToken: 'refresh',
    activeCondominioId: null,
  })
}

function renderDashboard() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <Suspense fallback={<div>loading</div>}>
          <DashboardPage />
        </Suspense>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  useAuthStore.setState({
    user: null, accessToken: null, refreshToken: null, activeCondominioId: null,
  })
})

describe('DashboardPage', () => {
  it('renderiza DashboardAdmin para ROLE_ADMIN', async () => {
    setUser([ROLES.ADMIN])
    renderDashboard()
    expect(await screen.findByText('Administração da Plataforma')).toBeTruthy()
  })

  it('renderiza DashboardSindico para ROLE_SINDICO', async () => {
    setUser([ROLES.SINDICO])
    renderDashboard()
    expect(await screen.findByText('Painel do Condomínio')).toBeTruthy()
  })

  it('renderiza DashboardMorador para ROLE_MORADOR', async () => {
    setUser([ROLES.MORADOR])
    renderDashboard()
    // Sem apartamento_id no JWT falso → exibe EmptySection
    expect(await screen.findByText(/Sua unidade não está vinculada/)).toBeTruthy()
  })

  it('renderiza DashboardProprietario para ROLE_PROPRIETARIO', async () => {
    setUser([ROLES.PROPRIETARIO])
    renderDashboard()
    expect(await screen.findByText('Meus Imóveis')).toBeTruthy()
  })

  it('renderiza DashboardUsuario para ROLE_USER sem outros roles', async () => {
    setUser([ROLES.USER])
    renderDashboard()
    expect(await screen.findByText('Bem-vindo ao CondoGest')).toBeTruthy()
  })

  it('exibe ProfileSwitcher com 2 opções para MORADOR+PROPRIETARIO', async () => {
    setUser([ROLES.MORADOR, ROLES.PROPRIETARIO])
    renderDashboard()
    expect(await screen.findByText('Ver como:')).toBeTruthy()
    expect(await screen.findByText('🏢 Proprietário')).toBeTruthy()
    expect(await screen.findByText('👤 Morador')).toBeTruthy()
  })

  it('NÃO renderiza Alert "Conecte a API"', async () => {
    setUser([ROLES.ADMIN])
    renderDashboard()
    await screen.findByText('Administração da Plataforma')
    expect(screen.queryByText(/Conecte a API/)).toBeNull()
  })
})
