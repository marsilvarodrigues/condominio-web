import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const GRUPOS = [
  { id: 1, nome: 'Manutenção Geral', tipoRateio: 'IGUALITARIO', escopo: 'TODOS', blocoId: null, planoContasId: null, parametrosJson: null },
  { id: 2, nome: 'Água por Consumo', tipoRateio: 'CONSUMO', escopo: 'TODOS', blocoId: null, planoContasId: null, parametrosJson: null },
  { id: 3, nome: 'Elevador Bloco A', tipoRateio: 'FRACAO_IDEAL', escopo: 'BLOCO', blocoId: 1, planoContasId: null, parametrosJson: null },
]

test.describe('Grupos de Despesa', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/grupos-despesa', GRUPOS)
    await page.goto('/rateio/grupos')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /grupos de despesa/i })).toBeVisible()
  })

  test('lista todos os grupos', async ({ page }) => {
    await expect(page.getByText('Manutenção Geral')).toBeVisible()
    await expect(page.getByText('Água por Consumo')).toBeVisible()
    await expect(page.getByText('Elevador Bloco A')).toBeVisible()
  })

  test('exibe tipo de rateio de cada grupo', async ({ page }) => {
    await expect(page.getByText('Igualitário')).toBeVisible()
    await expect(page.getByText(/consumo/i)).toBeVisible()
    await expect(page.getByText(/fração ideal/i)).toBeVisible()
  })

  test('exibe escopo de cada grupo', async ({ page }) => {
    await expect(page.getByText('Todos os apartamentos').first()).toBeVisible()
    await expect(page.getByText('Por bloco')).toBeVisible()
  })

  test('abre dialog de novo grupo', async ({ page }) => {
    await page.getByRole('button', { name: /novo grupo/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/^nome/i)).toBeVisible()
    await expect(page.getByLabelText(/tipo de rateio/i)).toBeVisible()
    await expect(page.getByLabelText(/escopo/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /novo grupo/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('salva novo grupo e fecha dialog', async ({ page }) => {
    mockMutation(page, '**/api/grupos-despesa', { id: 4, nome: 'Seguro', tipoRateio: 'FRACAO_IDEAL', escopo: 'TODOS' }, 201)
    await page.getByRole('button', { name: /novo grupo/i }).click()
    await page.getByLabelText(/^nome/i).fill('Seguro')
    await page.getByRole('button', { name: /salvar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre dialog de edição com dados preenchidos', async ({ page }) => {
    await page.getByRole('button', { name: /editar/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/^nome/i)).toHaveValue('Manutenção Geral')
  })

  test('abre confirmação ao excluir grupo', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir o grupo/i)).toBeVisible()
    await expect(page.getByText(/coeficientes associados/i)).toBeVisible()
  })

  test('cancela exclusão e mantém o grupo', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByText('Manutenção Geral')).toBeVisible()
  })
})
