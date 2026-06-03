import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const CONTAS = [
  { id: 1, codigo: '1', nome: 'Receitas', tipo: 'RECEITA', contaParenteId: null, contaParenteNome: null, nivel: 0 },
  { id: 2, codigo: '1.1', nome: 'Taxas Condominiais', tipo: 'RECEITA', contaParenteId: 1, contaParenteNome: 'Receitas', nivel: 1 },
  { id: 3, codigo: '2', nome: 'Despesas', tipo: 'DESPESA', contaParenteId: null, contaParenteNome: null, nivel: 0 },
]

test.describe('Plano de Contas', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/plano-contas', CONTAS)
    await page.goto('/financeiro/plano-contas')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /plano de contas/i })).toBeVisible()
  })

  test('lista contas retornadas pela API', async ({ page }) => {
    await expect(page.getByText('Receitas')).toBeVisible()
    await expect(page.getByText('Taxas Condominiais')).toBeVisible()
    await expect(page.getByText('Despesas')).toBeVisible()
  })

  test('exibe tipo de cada conta como chip', async ({ page }) => {
    await expect(page.getByText('Receita').first()).toBeVisible()
    await expect(page.getByText('Despesa').first()).toBeVisible()
  })

  test('exibe conta pai para contas filho', async ({ page }) => {
    await expect(page.getByText('Receitas').nth(1)).toBeVisible()
  })

  test('abre dialog de nova conta', async ({ page }) => {
    await page.getByRole('button', { name: /nova conta/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabel(/código/i)).toBeVisible()
    await expect(page.getByLabel(/^nome/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /nova conta/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('salva nova conta e fecha dialog', async ({ page }) => {
    mockMutation(page, '**/api/plano-contas', { id: 4, codigo: '3', nome: 'Transferências', tipo: 'TRANSFERENCIA', contaParenteId: null, contaParenteNome: null, nivel: 0 }, 201)
    await page.getByRole('button', { name: /nova conta/i }).click()
    await page.getByLabel(/código/i).fill('3')
    await page.getByLabel(/^nome/i).fill('Transferências')
    await page.getByRole('button', { name: /salvar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre confirmação ao excluir', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir a conta/i)).toBeVisible()
  })

  test('cancela exclusão e mantém o registro', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByText('Receitas')).toBeVisible()
  })
})
