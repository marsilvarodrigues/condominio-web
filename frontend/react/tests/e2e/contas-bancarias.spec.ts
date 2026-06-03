import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const BANCOS = [
  { id: 1, codigo: '001', nome: 'Banco do Brasil', ativo: true },
  { id: 2, codigo: '341', nome: 'Itaú Unibanco', ativo: true },
]

const CONTAS = [
  { id: 1, agencia: '1234', conta: '56789-0', tipo: 'CORRENTE', descricao: 'Conta principal', bancoId: 1, bancoNome: 'Banco do Brasil', saldo: 48320.50 },
  { id: 2, agencia: '0001', conta: '98765-4', tipo: 'POUPANCA', descricao: 'Reserva', bancoId: 2, bancoNome: 'Itaú Unibanco', saldo: 12400.00 },
]

const LANCAMENTOS = [
  {
    id: 1, contaBancariaId: 1, contaBancariaDescricao: 'Conta principal',
    dataLancamento: '2026-06-01', valor: 500, tipo: 'CREDITO',
    descricao: 'Taxa condominial', origem: 'COTA_CONDOMINIO',
    referenciaId: null, status: 'CONCILIADO', createdAt: '2026-06-01T00:00:00', updatedAt: '2026-06-01T00:00:00',
  },
  {
    id: 2, contaBancariaId: 1, contaBancariaDescricao: 'Conta principal',
    dataLancamento: '2026-05-30', valor: 1200, tipo: 'DEBITO',
    descricao: 'Pagamento fornecedor', origem: 'DESPESA_ORDINARIA',
    referenciaId: null, status: 'PENDENTE', createdAt: '2026-05-30T00:00:00', updatedAt: '2026-05-30T00:00:00',
  },
]

test.describe('Contas Bancárias', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/bancos', BANCOS)
    mockGet(page, '**/api/contas-bancarias', { content: CONTAS, totalElements: CONTAS.length })
    await page.goto('/financeiro/contas')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /contas bancárias/i })).toBeVisible()
  })

  test('lista contas com banco e tipo', async ({ page }) => {
    await expect(page.getByText('Banco do Brasil')).toBeVisible()
    await expect(page.getByText('Itaú Unibanco')).toBeVisible()
    await expect(page.getByText('Corrente')).toBeVisible()
    await expect(page.getByText('Poupança')).toBeVisible()
  })

  test('exibe saldo de cada conta', async ({ page }) => {
    await expect(page.getByText(/48\.320|48,320/)).toBeVisible()
    await expect(page.getByText(/12\.400|12,400/)).toBeVisible()
  })

  test('abre dialog de nova conta', async ({ page }) => {
    await page.getByRole('button', { name: /nova conta/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabel(/agência/i)).toBeVisible()
    await expect(page.getByLabel(/^conta/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /nova conta/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre extrato ao clicar em Ver extrato', async ({ page }) => {
    mockGet(page, '**/api/lancamentos-bancarios**', { content: LANCAMENTOS, totalElements: LANCAMENTOS.length })
    await page.getByTitle('Ver extrato').first().click()
    await expect(page.getByText('Taxa condominial')).toBeVisible()
    await expect(page.getByText('Pagamento fornecedor')).toBeVisible()
  })

  test('abre confirmação ao excluir', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir a conta/i)).toBeVisible()
  })

  test('cancela exclusão e mantém a conta', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByText('Banco do Brasil')).toBeVisible()
  })
})
