import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet } from './helpers'

const CONTAS = [
  { id: 1, agencia: '1234', conta: '56789-0', tipo: 'CORRENTE', descricao: '', bancoId: 1, bancoNome: 'Banco do Brasil', saldo: 48320.50 },
]

const LANCAMENTOS = [
  {
    id: 1, contaBancariaId: 1, contaBancariaDescricao: 'Conta principal',
    dataLancamento: '2026-06-01', valor: 500, tipo: 'CREDITO',
    descricao: 'Taxa condominial Apto 101', origem: 'COTA_CONDOMINIO',
    referenciaId: null, status: 'CONCILIADO', createdAt: '2026-06-01T00:00:00', updatedAt: '2026-06-01T00:00:00',
  },
  {
    id: 2, contaBancariaId: 1, contaBancariaDescricao: 'Conta principal',
    dataLancamento: '2026-05-30', valor: 200, tipo: 'DEBITO',
    descricao: 'Débito não identificado', origem: 'MANUAL',
    referenciaId: null, status: 'PENDENTE', createdAt: '2026-05-30T00:00:00', updatedAt: '2026-05-30T00:00:00',
  },
]

test.describe('Conciliação Bancária', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/contas-bancarias', { content: CONTAS, totalElements: CONTAS.length })
    await page.goto('/financeiro/conciliacao')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /conciliação bancária/i })).toBeVisible()
  })

  test('exibe mensagem para selecionar conta antes de ver lançamentos', async ({ page }) => {
    await expect(page.getByText(/selecione uma conta bancária/i)).toBeVisible()
  })

  test('seletor de conta está disponível', async ({ page }) => {
    await expect(page.getByLabel(/conta bancária/i)).toBeVisible()
  })

  test('seletor de status está desabilitado sem conta selecionada', async ({ page }) => {
    await expect(page.getByLabel('Status', { exact: true })).toBeDisabled()
  })

  test('ao selecionar conta carrega os lançamentos', async ({ page }) => {
    mockGet(page, '**/api/lancamentos-bancarios**', { content: LANCAMENTOS, totalElements: LANCAMENTOS.length })
    await page.getByLabel(/conta bancária/i).click()
    await page.getByRole('option', { name: /banco do brasil/i }).click()
    await expect(page.getByText('Taxa condominial Apto 101')).toBeVisible()
    await expect(page.getByText('Débito não identificado')).toBeVisible()
  })

  test('exibe status dos lançamentos como chips coloridos', async ({ page }) => {
    mockGet(page, '**/api/lancamentos-bancarios**', { content: LANCAMENTOS, totalElements: LANCAMENTOS.length })
    await page.getByLabel(/conta bancária/i).click()
    await page.getByRole('option', { name: /banco do brasil/i }).click()
    await expect(page.getByText('Conciliado')).toBeVisible()
    await expect(page.getByText('Pendente').first()).toBeVisible()
  })

  test('exibe cards de resumo após carregar lançamentos', async ({ page }) => {
    mockGet(page, '**/api/lancamentos-bancarios**', { content: LANCAMENTOS, totalElements: LANCAMENTOS.length })
    await page.getByLabel(/conta bancária/i).click()
    await page.getByRole('option', { name: /banco do brasil/i }).click()
    await expect(page.getByText('Pendentes')).toBeVisible()
    await expect(page.getByText('Conciliados')).toBeVisible()
    await expect(page.getByText('Total')).toBeVisible()
  })
})
