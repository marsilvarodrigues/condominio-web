import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const BLOCOS = [
  { id: 1, numero: 1, bloco: 'A', condominioId: 1 },
  { id: 2, numero: 2, bloco: 'B', condominioId: 1 },
]

const APARTAMENTOS = [
  { id: 10, numero: '101', andar: 1, blocoId: 1, blocoNome: 'A', fracaoIdeal: 1.2, metragem: 65, proprietarioId: null, proprietarioNome: null },
  { id: 11, numero: '102', andar: 1, blocoId: 1, blocoNome: 'A', fracaoIdeal: 1.2, metragem: 65, proprietarioId: 5, proprietarioNome: 'João Silva' },
]

test.describe('Hierarquia — Blocos e Apartamentos', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/blocos', BLOCOS)
    await page.goto('/hierarquia')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /hierarquia/i })).toBeVisible()
  })

  test('lista blocos no painel lateral', async ({ page }) => {
    await expect(page.getByText('Bloco A')).toBeVisible()
    await expect(page.getByText('Bloco B')).toBeVisible()
  })

  test('exibe mensagem de seleção antes de escolher bloco', async ({ page }) => {
    await expect(page.getByText(/selecione um bloco/i)).toBeVisible()
  })

  test('ao clicar num bloco carrega os apartamentos', async ({ page }) => {
    mockGet(page, '**/api/apartamentos**', APARTAMENTOS)
    await page.getByText('Bloco A').click()
    await expect(page.getByText('101')).toBeVisible()
    await expect(page.getByText('102')).toBeVisible()
  })

  test('exibe proprietário ou "Vago" nos apartamentos', async ({ page }) => {
    mockGet(page, '**/api/apartamentos**', APARTAMENTOS)
    await page.getByText('Bloco A').click()
    await expect(page.getByText('João Silva')).toBeVisible()
    await expect(page.getByText('Vago')).toBeVisible()
  })

  test('abre dialog de novo bloco', async ({ page }) => {
    await page.getByRole('button', { name: /novo$/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/número/i)).toBeVisible()
    await expect(page.getByLabelText(/identificação/i)).toBeVisible()
  })

  test('fecha dialog de bloco ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /novo$/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre dialog de novo apartamento ao selecionar bloco', async ({ page }) => {
    mockGet(page, '**/api/apartamentos**', APARTAMENTOS)
    await page.getByText('Bloco A').click()
    await page.getByRole('button', { name: /novo apartamento/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/número/i)).toBeVisible()
  })

  test('abre confirmação de exclusão de bloco', async ({ page }) => {
    await page.locator('[aria-label="Excluir"], [title="Excluir"]').first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir o bloco/i)).toBeVisible()
  })
})
