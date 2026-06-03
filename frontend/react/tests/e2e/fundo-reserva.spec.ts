import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const MOVIMENTACOES = [
  { id: 1, tipo: 'ENTRADA', valor: 5000, descricao: 'Contribuição mensal', data: '2026-06-01', saldoApos: 25000 },
  { id: 2, tipo: 'SAIDA', valor: 1200, descricao: 'Reparo emergencial', data: '2026-05-15', saldoApos: 20000 },
  { id: 3, tipo: 'RENDIMENTO', valor: 180.50, descricao: 'Rendimento CDB', data: '2026-05-01', saldoApos: 21200 },
]

test.describe('Fundo de Reserva', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/fundo-reserva/movimentacoes', MOVIMENTACOES)
    mockGet(page, '**/api/fundo-reserva/saldo', 25000)
    await page.goto('/financeiro/fundo-reserva')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /fundo de reserva/i })).toBeVisible()
  })

  test('exibe saldo atual em destaque', async ({ page }) => {
    await expect(page.getByText(/25\.000|25,000/)).toBeVisible()
  })

  test('lista movimentações', async ({ page }) => {
    await expect(page.getByText('Contribuição mensal')).toBeVisible()
    await expect(page.getByText('Reparo emergencial')).toBeVisible()
    await expect(page.getByText('Rendimento CDB')).toBeVisible()
  })

  test('exibe tipo de movimentação como chip colorido', async ({ page }) => {
    await expect(page.getByText('Entrada').first()).toBeVisible()
    await expect(page.getByText('Saída').first()).toBeVisible()
    await expect(page.getByText('Rendimento').first()).toBeVisible()
  })

  test('abre dialog de nova movimentação', async ({ page }) => {
    await page.getByRole('button', { name: /nova movimentação/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabel(/tipo/i)).toBeVisible()
    await expect(page.getByLabel(/valor/i)).toBeVisible()
    await expect(page.getByLabel(/descrição/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /nova movimentação/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('salva nova movimentação e fecha dialog', async ({ page }) => {
    mockMutation(page, '**/api/fundo-reserva/movimentacoes', MOVIMENTACOES[0], 200)
    await page.getByRole('button', { name: /nova movimentação/i }).click()
    await page.getByLabel(/valor/i).fill('3000')
    await page.getByLabel(/descrição/i).fill('Aporte extra')
    await page.getByLabel(/data/i).fill('2026-06-02')
    await page.getByRole('button', { name: /salvar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })
})
