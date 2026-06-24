import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { http, HttpResponse } from 'msw'
import { theme } from '@/theme/theme'
import { server } from './mocks/server'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import HierarquiaPage from '@/pages/hierarquia/HierarquiaPage'

const BASE = '/api'

function renderHierarquia(initialPath = '/hierarquia') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={[initialPath]}>
          <Routes>
            <Route path="/hierarquia" element={<HierarquiaPage />} />
            <Route
              path="/hierarquia/apartamentos/:id"
              element={<div>Detalhe do Apartamento</div>}
            />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  useAuthStore.setState({
    user: { id: 1, email: 'admin@test.com', roles: [ROLES.ADMIN], condominioIds: [1] },
    accessToken: 'token',
    refreshToken: 'refresh',
    activeCondominioId: 1,
  })
})

describe('HierarquiaPage', () => {
  it('renderiza a lista de blocos vinda da API', async () => {
    renderHierarquia()
    expect(await screen.findByText('Bloco A')).toBeInTheDocument()
    expect(screen.getByText('Nº 1')).toBeInTheDocument()
  })

  it('exibe estado vazio quando não há blocos', async () => {
    server.use(
      http.get(`${BASE}/blocos`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [],
        }),
      ),
    )
    renderHierarquia()
    expect(
      await screen.findByText('Selecione um bloco para ver os apartamentos.'),
    ).toBeInTheDocument()
  })

  it('exibe a tabela de apartamentos do bloco selecionado e mensagem de vazio quando não há apartamentos', async () => {
    server.use(
      http.get(`${BASE}/apartamentos`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [],
        }),
      ),
    )
    renderHierarquia()

    const blocoItem = await screen.findByText('Bloco A')
    await userEvent.click(blocoItem)

    expect(await screen.findByText('Bloco A — Apartamentos')).toBeInTheDocument()
    expect(await screen.findByText('Nenhum apartamento neste bloco.')).toBeInTheDocument()
  })

  it('lista apartamentos do bloco selecionado com dados da API', async () => {
    server.use(
      http.get(`${BASE}/apartamentos`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [
            {
              id: 101,
              blocoId: 1,
              blocoNome: 'A',
              numero: '101',
              createdAt: null,
              updatedAt: null,
              areaConstruida: 75,
              fracaoIdeal: 2.5,
              andar: 1,
              quantidadeMoradores: 2,
            },
          ],
        }),
      ),
    )
    renderHierarquia()

    const blocoItem = await screen.findByText('Bloco A')
    await userEvent.click(blocoItem)

    expect(await screen.findByText('101')).toBeInTheDocument()
    expect(screen.getByText('2 morador(es)')).toBeInTheDocument()
  })

  it('navega para a página de detalhe ao clicar em "Ver detalhes" de um apartamento', async () => {
    server.use(
      http.get(`${BASE}/apartamentos`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [
            {
              id: 202,
              blocoId: 1,
              blocoNome: 'A',
              numero: '202',
              createdAt: null,
              updatedAt: null,
              areaConstruida: 60,
              fracaoIdeal: 1.8,
              andar: 2,
              quantidadeMoradores: 0,
            },
          ],
        }),
      ),
    )
    renderHierarquia()

    const blocoItem = await screen.findByText('Bloco A')
    await userEvent.click(blocoItem)

    await screen.findByText('202')
    const detailButton = screen.getByRole('button', { name: /ver detalhes/i })
    await userEvent.click(detailButton)

    expect(await screen.findByText('Detalhe do Apartamento')).toBeInTheDocument()
  })

  it('exibe chip "Vago" para apartamentos sem moradores', async () => {
    server.use(
      http.get(`${BASE}/apartamentos`, () =>
        HttpResponse.json({
          requestId: 'test-req',
          timestamp: new Date().toISOString(),
          data: [
            {
              id: 303,
              blocoId: 1,
              blocoNome: 'A',
              numero: '303',
              createdAt: null,
              updatedAt: null,
              areaConstruida: 50,
              fracaoIdeal: null,
              andar: null,
              quantidadeMoradores: 0,
            },
          ],
        }),
      ),
    )
    renderHierarquia()

    const blocoItem = await screen.findByText('Bloco A')
    await userEvent.click(blocoItem)

    const row = await screen.findByText('303')
    expect(within(row.closest('tr')!).getByText('Vago')).toBeInTheDocument()
  })
})
