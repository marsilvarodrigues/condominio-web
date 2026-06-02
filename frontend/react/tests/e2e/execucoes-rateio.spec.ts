import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet } from './helpers'

const EXECUCOES_PAGE = {
  content: [
    {
      id: 1,
      tipo: 'AUTOMATICO',
      dataExecucao: '2026-06-02T02:00:00',
      status: 'SUCESSO',
      totalRateado: 12400.00,
      descricao: 'Rateio automático mensal',
      lancamentos: [
        { id: 1, apartamentoId: 10, apartamentoNumero: '101', blocoNome: 'A', coeficiente: 0.0125, valor: 155.00, status: 'PAGO' },
        { id: 2, apartamentoId: 11, apartamentoNumero: '102', blocoNome: 'A', coeficiente: 0.0125, valor: 155.00, status: 'PENDENTE' },
      ],
    },
    {
      id: 2,
      tipo: 'MANUAL',
      dataExecucao: '2026-05-15T10:30:00',
      status: 'PARCIAL',
      totalRateado: 3200.00,
      descricao: 'Rateio despesa extra',
      lancamentos: [],
    },
    {
      id: 3,
      tipo: 'RECALCULO',
      dataExecucao: '2026-04-01T08:00:00',
      status: 'ERRO',
      totalRateado: 0,
      descricao: null,
      lancamentos: [],
    },
  ],
  totalElements: 3,
  totalPages: 1,
  size: 20,
  number: 0,
}

test.describe('Execuções de Rateio', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/rateio/execucoes**', EXECUCOES_PAGE)
    await page.goto('/rateio/execucoes')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /execuções de rateio/i })).toBeVisible()
  })

  test('lista todas as execuções', async ({ page }) => {
    await expect(page.getByText('Rateio automático mensal')).toBeVisible()
    await expect(page.getByText('Rateio despesa extra')).toBeVisible()
  })

  test('exibe tipo de execução de cada registro', async ({ page }) => {
    await expect(page.getByText('Automático')).toBeVisible()
    await expect(page.getByText('Manual')).toBeVisible()
    await expect(page.getByText('Recálculo')).toBeVisible()
  })

  test('exibe status de cada execução com chip colorido', async ({ page }) => {
    await expect(page.getByText('Sucesso')).toBeVisible()
    await expect(page.getByText('Parcial')).toBeVisible()
    await expect(page.getByText('Erro')).toBeVisible()
  })

  test('exibe total rateado de cada execução', async ({ page }) => {
    await expect(page.getByText(/12\.400|12,400/)).toBeVisible()
    await expect(page.getByText(/3\.200|3,200/)).toBeVisible()
  })

  test('expande execução ao clicar no ícone de expansão', async ({ page }) => {
    await page.getByRole('button').filter({ has: page.locator('svg') }).first().click()
    // Os lançamentos da primeira execução devem aparecer
    await expect(page.getByText('101')).toBeVisible()
    await expect(page.getByText('102')).toBeVisible()
  })

  test('recolhe execução ao clicar novamente no ícone', async ({ page }) => {
    const expandBtn = page.getByRole('button').filter({ has: page.locator('svg') }).first()
    await expandBtn.click()
    await expect(page.getByText('101')).toBeVisible()
    await expandBtn.click()
    await expect(page.getByText('101')).not.toBeVisible()
  })

  test('botão de atualizar está disponível', async ({ page }) => {
    await expect(page.getByRole('button', { name: /atualizar/i }).or(
      page.locator('[aria-label="Atualizar"], [title="Atualizar"]'),
    ).first()).toBeVisible()
  })

  test('paginação não aparece com menos resultados que o tamanho da página', async ({ page }) => {
    // 3 itens < 20 por página → sem paginação
    await expect(page.getByRole('button', { name: /próxima|anterior/i })).not.toBeVisible()
  })
})
