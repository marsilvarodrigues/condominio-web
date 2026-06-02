import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet } from './helpers'

const CONTAS = [
  { id: 1, agencia: '1234', conta: '56789-0', tipo: 'CORRENTE', descricao: '', bancoId: 1, bancoNome: 'Banco do Brasil', saldo: 48320.50 },
]

const ITENS_CONCILIACAO = [
  { id: 1, data: '2026-06-01', descricao: 'Taxa condominial Apto 101', valor: 500, status: 'CONCILIADO', lancamentoDescricao: 'Taxa mensal', sugestaoLancamentoId: null },
  { id: 2, data: '2026-05-30', descricao: 'Débito não identificado', valor: -200, status: 'PENDENTE', lancamentoDescricao: null, sugestaoLancamentoId: 15 },
  { id: 3, data: '2026-05-28', descricao: 'Pagamento divergente', valor: -1500, status: 'DIVERGENTE', lancamentoDescricao: 'Fornecedor X', sugestaoLancamentoId: null },
]

test.describe('Conciliação Bancária', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/contas-bancarias', CONTAS)
    await page.goto('/financeiro/conciliacao')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /conciliação bancária/i })).toBeVisible()
  })

  test('exibe mensagem para selecionar conta antes de ver itens', async ({ page }) => {
    await expect(page.getByText(/selecione uma conta bancária/i)).toBeVisible()
  })

  test('seletor de conta está disponível', async ({ page }) => {
    await expect(page.getByLabel(/conta/i)).toBeVisible()
  })

  test('botão Importar Extrato está desabilitado sem conta selecionada', async ({ page }) => {
    await expect(page.getByRole('button', { name: /importar extrato/i })).toBeDisabled()
  })

  test('ao selecionar conta carrega os itens de conciliação', async ({ page }) => {
    mockGet(page, '**/api/conciliacao/itens/**', ITENS_CONCILIACAO)
    await page.getByLabel(/conta/i).click()
    await page.getByRole('option', { name: /banco do brasil/i }).click()
    await expect(page.getByText('Taxa condominial Apto 101')).toBeVisible()
    await expect(page.getByText('Débito não identificado')).toBeVisible()
  })

  test('exibe status dos itens como chips coloridos', async ({ page }) => {
    mockGet(page, '**/api/conciliacao/itens/**', ITENS_CONCILIACAO)
    await page.getByLabel(/conta/i).click()
    await page.getByRole('option', { name: /banco do brasil/i }).click()
    await expect(page.getByText('Conciliado')).toBeVisible()
    await expect(page.getByText('Pendente').first()).toBeVisible()
    await expect(page.getByText('Divergente')).toBeVisible()
  })

  test('botão Importar Extrato fica habilitado ao selecionar conta', async ({ page }) => {
    await page.getByLabel(/conta/i).click()
    await page.getByRole('option', { name: /banco do brasil/i }).click()
    await expect(page.getByRole('button', { name: /importar extrato/i })).toBeEnabled()
  })
})
