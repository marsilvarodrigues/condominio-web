import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const GRUPOS = [
  { id: 1, nome: 'Manutenção Geral', tipoRateio: 'IGUALITARIO', escopo: 'TODOS', blocoId: null, planoContasId: null, parametrosJson: null },
  { id: 2, nome: 'Fração Ideal', tipoRateio: 'FRACAO_IDEAL', escopo: 'TODOS', blocoId: null, planoContasId: null, parametrosJson: null },
]

const COEFICIENTES = [
  { id: 1, grupoDespesaId: 1, apartamentoId: 10, apartamentoNumero: '101', blocoNome: 'A', coeficiente: 0.0125, vigenciaInicio: '2026-01-01', vigenciaFim: null },
  { id: 2, grupoDespesaId: 1, apartamentoId: 11, apartamentoNumero: '102', blocoNome: 'A', coeficiente: 0.0125, vigenciaInicio: '2026-01-01', vigenciaFim: null },
  { id: 3, grupoDespesaId: 1, apartamentoId: 12, apartamentoNumero: '201', blocoNome: 'B', coeficiente: 0.0150, vigenciaInicio: '2026-01-01', vigenciaFim: null },
]

test.describe('Coeficientes de Rateio', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/grupos-despesa', GRUPOS)
    await page.goto('/rateio/coeficientes')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /coeficientes/i })).toBeVisible()
  })

  test('exibe mensagem para selecionar grupo antes de ver coeficientes', async ({ page }) => {
    await expect(page.getByText(/selecione um grupo de despesa/i)).toBeVisible()
  })

  test('seletor de grupo está disponível com os grupos carregados', async ({ page }) => {
    await page.getByLabel(/grupo de despesa/i).click()
    await expect(page.getByRole('option', { name: 'Manutenção Geral' })).toBeVisible()
    await expect(page.getByRole('option', { name: 'Fração Ideal' })).toBeVisible()
  })

  test('botão Novo Coeficiente está desabilitado sem grupo selecionado', async ({ page }) => {
    await expect(page.getByRole('button', { name: /novo coeficiente/i })).toBeDisabled()
  })

  test('ao selecionar grupo carrega os coeficientes', async ({ page }) => {
    mockGet(page, '**/api/grupos-despesa/**/coeficientes', COEFICIENTES)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await expect(page.getByText('101')).toBeVisible()
    await expect(page.getByText('102')).toBeVisible()
    await expect(page.getByText('201')).toBeVisible()
  })

  test('exibe coeficiente formatado com 4 casas decimais', async ({ page }) => {
    mockGet(page, '**/api/grupos-despesa/**/coeficientes', COEFICIENTES)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await expect(page.getByText('0,0125').or(page.getByText('0.0125')).first()).toBeVisible()
  })

  test('botão Novo Coeficiente fica habilitado com grupo selecionado', async ({ page }) => {
    mockGet(page, '**/api/grupos-despesa/**/coeficientes', COEFICIENTES)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await expect(page.getByRole('button', { name: /novo coeficiente/i })).toBeEnabled()
  })

  test('abre dialog de novo coeficiente', async ({ page }) => {
    mockGet(page, '**/api/grupos-despesa/**/coeficientes', COEFICIENTES)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await page.getByRole('button', { name: /novo coeficiente/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabel(/apartamento/i)).toBeVisible()
    await expect(page.getByLabel(/coeficiente/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    mockGet(page, '**/api/grupos-despesa/**/coeficientes', COEFICIENTES)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await page.getByRole('button', { name: /novo coeficiente/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre confirmação ao excluir coeficiente', async ({ page }) => {
    mockGet(page, '**/api/grupos-despesa/**/coeficientes', COEFICIENTES)
    await page.getByLabel(/grupo de despesa/i).click()
    await page.getByRole('option', { name: 'Manutenção Geral' }).click()
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir o coeficiente/i)).toBeVisible()
  })
})
