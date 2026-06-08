import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet } from './helpers'

const APT = {
  id: 10, numero: '101', andar: 1, blocoId: 1, blocoNome: 'A',
  fracaoIdeal: 1.2, metragem: 65, proprietarioId: null, proprietarioNome: null,
}

const MORADORES = [
  {
    id: 1, nome: 'Carlos Pereira', tipo: 'PF', cpf: '456.789.123-00',
    cnpj: null, razaoSocial: null, email: 'carlos@email.com', telefone: null,
    apartamentoId: 10, apartamentoNumero: '101', userId: 7,
    createdAt: '2026-03-01T00:00:00', updatedAt: '2026-03-01T00:00:00',
  },
]

const PROPRIETARIOS_APT = [
  {
    id: 2, nome: 'Maria Oliveira', tipo: 'PROP_PF', cpf: '987.654.321-00',
    cnpj: null, razaoSocial: null, email: 'maria@email.com', telefone: null,
    apartamentos: [{ id: 10, numero: '101', blocoNome: 'A', condominioId: 1 }],
    userId: 8, createdAt: '2020-01-01T00:00:00', updatedAt: '2020-01-01T00:00:00',
  },
]

test.describe('Detalhe do Apartamento', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/apartamentos/10', APT)
    mockGet(page, '**/api/pessoas**', { content: MORADORES, totalElements: MORADORES.length })
    mockGet(page, '**/api/apartamentos/10/proprietarios', PROPRIETARIOS_APT)
    await page.goto('/hierarquia/apartamentos/10')
  })

  test('exibe título com número e bloco do apartamento', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /apt 101/i })).toBeVisible()
  })

  test('exibe botão de voltar', async ({ page }) => {
    await expect(page.getByRole('button', { name: /voltar/i })).toBeVisible()
  })

  test('exibe abas de Moradores e Proprietários', async ({ page }) => {
    await expect(page.getByRole('tab', { name: /moradores/i })).toBeVisible()
    await expect(page.getByRole('tab', { name: /proprietários/i })).toBeVisible()
  })

  test('aba Moradores mostra moradores do apartamento', async ({ page }) => {
    await expect(page.getByText('Carlos Pereira')).toBeVisible()
    await expect(page.getByText('carlos@email.com')).toBeVisible()
    await expect(page.getByText('456.789.123-00')).toBeVisible()
  })

  test('exibe botão "Novo Morador"', async ({ page }) => {
    await expect(page.getByRole('button', { name: /novo morador/i })).toBeVisible()
  })

  test('abre dialog de criar morador com campos de cadastro', async ({ page }) => {
    await page.getByRole('button', { name: /novo morador/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabel(/nome/i)).toBeVisible()
    await expect(page.getByLabel(/e-mail/i)).toBeVisible()
    await expect(page.getByLabel(/cpf/i)).toBeVisible()
  })

  test('abre dialog de editar morador ao clicar no ícone de edição', async ({ page }) => {
    await page.getByRole('button', { name: /editar/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    const nomeField = page.getByLabel(/nome/i)
    await expect(nomeField).toHaveValue('Carlos Pereira')
  })

  test('fecha dialog de morador ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /novo morador/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre confirmação ao excluir morador', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir/i)).toBeVisible()
  })

  test('aba Proprietários mostra proprietários do apartamento', async ({ page }) => {
    await page.getByRole('tab', { name: /proprietários/i }).click()
    await expect(page.getByText('Maria Oliveira')).toBeVisible()
    await expect(page.getByText('987.654.321-00')).toBeVisible()
    await expect(page.getByText('Pessoa Física')).toBeVisible()
  })

  test('exibe botão "Novo Proprietário" na aba proprietários', async ({ page }) => {
    await page.getByRole('tab', { name: /proprietários/i }).click()
    await expect(page.getByRole('button', { name: /novo proprietário/i })).toBeVisible()
  })

  test('abre dialog de novo proprietário com tipo, nome e e-mail', async ({ page }) => {
    await page.getByRole('tab', { name: /proprietários/i }).click()
    await page.getByRole('button', { name: /novo proprietário/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabel(/nome/i)).toBeVisible()
    await expect(page.getByLabel(/e-mail/i)).toBeVisible()
    await expect(page.getByLabel(/tipo/i)).toBeVisible()
  })

  test('abre dialog de editar proprietário ao clicar no ícone de edição', async ({ page }) => {
    await page.getByRole('tab', { name: /proprietários/i }).click()
    await page.getByRole('button', { name: /editar/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    const nomeField = page.getByLabel(/nome/i)
    await expect(nomeField).toHaveValue('Maria Oliveira')
  })

  test('botão voltar navega para /hierarquia', async ({ page }) => {
    await page.getByRole('button', { name: /voltar/i }).click()
    await expect(page).toHaveURL('/hierarquia')
  })
})
