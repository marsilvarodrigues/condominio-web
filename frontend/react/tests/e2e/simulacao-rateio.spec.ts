import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const GRUPOS = [
  { id: 1, nome: 'Manutenção Geral', tipoRateio: 'IGUALITARIO', escopo: 'TODOS', blocoId: null, planoContasId: null, parametrosJson: null },
]

const SIMULACAO = {
  grupoNome: 'Manutenção Geral',
  totalDespesas: 12400,
  linhas: [
    { apartamentoId: 10, apartamentoNumero: '101', blocoNome: 'A', coeficiente: 0.0125, valorRateado: 155.00 },
    { apartamentoId: 11, apartamentoNumero: '102', blocoNome: 'A', coeficiente: 0.0125, valorRateado: 155.00 },
    { apartamentoId: 12, apartamentoNumero: '201', blocoNome: 'B', coeficiente: 0.0150, valorRateado: 186.00 },
  ],
}

test.describe('Simulação de Rateio', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/grupos-despesa', GRUPOS)
    await page.goto('/rateio/simulacao')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /simulação de rateio/i })).toBeVisible()
  })

  test('exibe painel de parâmetros com seletor de grupo e campo de ano', async ({ page }) => {
    await expect(page.getByLabel(/grupo de despesa/i)).toBeVisible()
    await expect(page.getByLabel(/ano/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /simular/i })).toBeVisible()
  })

  test('botão Simular está desabilitado sem grupo selecionado', async ({ page }) => {
    await expect(page.getByRole('button', { name: /simular/i })).toBeDisabled()
  })

  test('exibe placeholder antes da simulação', async ({ page }) => {
    await expect(page.getByText(/configure os parâmetros/i)).toBeVisible()
  })

  test('ao simular exibe o resultado com totais', async ({ page }) => {
    mockMutation(page, '**/api/rateio/simular', SIMULACAO)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await page.getByRole('button', { name: /simular/i }).click()
    await expect(page.getByText(/12\.400|12,400/)).toBeVisible()
    await expect(page.getByText('3')).toBeVisible() // 3 unidades
  })

  test('exibe as linhas do rateio por apartamento', async ({ page }) => {
    mockMutation(page, '**/api/rateio/simular', SIMULACAO)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await page.getByRole('button', { name: /simular/i }).click()
    await expect(page.getByText('101')).toBeVisible()
    await expect(page.getByText('102')).toBeVisible()
    await expect(page.getByText('201')).toBeVisible()
  })

  test('exibe coeficiente e valor rateado de cada unidade', async ({ page }) => {
    mockMutation(page, '**/api/rateio/simular', SIMULACAO)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await page.getByRole('button', { name: /simular/i }).click()
    await expect(page.getByText(/155,00|155.00/).first()).toBeVisible()
  })

  test('nova simulação limpa o resultado anterior', async ({ page }) => {
    mockMutation(page, '**/api/rateio/simular', SIMULACAO)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await page.getByRole('button', { name: /simular/i }).click()
    await expect(page.getByText('101')).toBeVisible()
    // Mudar ano deve limpar
    await page.getByLabel(/ano/i).fill('2025')
    await expect(page.getByText('101')).not.toBeVisible()
    await expect(page.getByText(/configure os parâmetros/i)).toBeVisible()
  })
})
