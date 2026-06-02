import { test, expect } from '@playwright/test'
import { setAdminAuth } from './helpers'

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    await page.goto('/')
  })

  test('exibe título do painel administrativo para admin', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /painel administrativo/i })).toBeVisible()
  })

  test('exibe e-mail do usuário no subtítulo', async ({ page }) => {
    await expect(page.getByText(/admin@test\.com/i)).toBeVisible()
  })

  test('exibe os quatro cards de métricas', async ({ page }) => {
    await expect(page.getByText(/apartamentos/i).first()).toBeVisible()
    await expect(page.getByText(/saldo bancário/i)).toBeVisible()
    await expect(page.getByText(/despesas do mês/i)).toBeVisible()
    await expect(page.getByText(/inadimplência/i)).toBeVisible()
  })

  test('exibe card de último rateio', async ({ page }) => {
    await expect(page.getByText(/último rateio/i)).toBeVisible()
  })

  test('exibe card de resumo financeiro', async ({ page }) => {
    await expect(page.getByText(/resumo financeiro/i)).toBeVisible()
    await expect(page.getByText(/receita prevista/i)).toBeVisible()
    await expect(page.getByText(/despesa realizada/i)).toBeVisible()
    await expect(page.getByText(/fundo de reserva/i)).toBeVisible()
  })

  test('sidebar de navegação está visível', async ({ page }) => {
    await expect(page.getByText(/condomínios/i).first()).toBeVisible()
    await expect(page.getByText(/hierarquia/i)).toBeVisible()
  })

  test('link para Condomínios navega corretamente', async ({ page }) => {
    await page.getByRole('link', { name: /condomínios/i }).first().click()
    await expect(page).toHaveURL(/\/condominios/)
  })

  test('redireciona para /login quando não autenticado', async ({ page }) => {
    // Limpa auth e tenta acessar direto
    await page.evaluate(() => localStorage.removeItem('condogest-auth'))
    await page.goto('/')
    await expect(page).toHaveURL(/\/login/)
  })
})
