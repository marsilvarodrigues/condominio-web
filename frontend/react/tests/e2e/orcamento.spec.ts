import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const ANO = new Date().getFullYear()

const ITENS = [
  { id: 1, ano: ANO, contaId: 3, contaNome: 'Manutenção', grupoDespesaId: 1, grupoDespesaNome: 'Manutenção Geral', valorOrcado: 12000, valorRealizado: 8500, statusRateio: 'RATEADA' },
  { id: 2, ano: ANO, contaId: 4, contaNome: 'Limpeza', grupoDespesaId: null, grupoDespesaNome: null, valorOrcado: 6000, valorRealizado: 0, statusRateio: null },
]

test.describe('Orçamento Anual', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, `**/api/orcamentos**`, ITENS)
    await page.goto('/financeiro/orcamento')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /orçamento anual/i })).toBeVisible()
  })

  test('lista itens do orçamento', async ({ page }) => {
    await expect(page.getByText('Manutenção')).toBeVisible()
    await expect(page.getByText('Limpeza')).toBeVisible()
  })

  test('exibe status de rateio dos itens', async ({ page }) => {
    await expect(page.getByText('RATEADA').or(page.getByText('Rateado')).first()).toBeVisible()
    await expect(page.getByText('Pendente').first()).toBeVisible()
  })

  test('exibe total orçado', async ({ page }) => {
    // R$ 18.000 = 12000 + 6000
    await expect(page.getByText(/18\.000|18,000/)).toBeVisible()
  })

  test('campo de ano está visível e editável', async ({ page }) => {
    const anoInput = page.getByLabel(/ano/i)
    await expect(anoInput).toBeVisible()
    await expect(anoInput).toHaveValue(String(ANO))
  })

  test('botão Recalcular Rateio está visível', async ({ page }) => {
    await expect(page.getByRole('button', { name: /recalcular/i })).toBeVisible()
  })

  test('abre dialog para adicionar item', async ({ page }) => {
    await page.getByRole('button', { name: /adicionar item/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/id da conta/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /adicionar item/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre confirmação ao remover item', async ({ page }) => {
    await page.getByRole('button', { name: /remover/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/remover/i)).toBeVisible()
  })
})
