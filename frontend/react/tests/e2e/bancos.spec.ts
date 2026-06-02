import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const BANCOS = [
  { id: 1, codigo: '001', nome: 'Banco do Brasil', ativo: true },
  { id: 2, codigo: '341', nome: 'Itaú Unibanco', ativo: true },
  { id: 3, codigo: '033', nome: 'Santander', ativo: true },
]

test.describe('Bancos', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/bancos', BANCOS)
    await page.goto('/financeiro/bancos')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /bancos/i })).toBeVisible()
  })

  test('lista bancos retornados pela API', async ({ page }) => {
    await expect(page.getByText('Banco do Brasil')).toBeVisible()
    await expect(page.getByText('Itaú Unibanco')).toBeVisible()
    await expect(page.getByText('Santander')).toBeVisible()
  })

  test('exibe código dos bancos', async ({ page }) => {
    await expect(page.getByText('001')).toBeVisible()
    await expect(page.getByText('341')).toBeVisible()
  })

  test('abre dialog de novo banco', async ({ page }) => {
    await page.getByRole('button', { name: /novo banco/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/código/i)).toBeVisible()
    await expect(page.getByLabelText(/^nome/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /novo banco/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('salva novo banco e fecha dialog', async ({ page }) => {
    mockMutation(page, '**/api/bancos', { id: 4, codigo: '237', nome: 'Bradesco', ativo: true }, 201)
    await page.getByRole('button', { name: /novo banco/i }).click()
    await page.getByLabelText(/código/i).fill('237')
    await page.getByLabelText(/^nome/i).fill('Bradesco')
    await page.getByRole('button', { name: /salvar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre confirmação ao excluir', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir o banco/i)).toBeVisible()
  })

  test('cancela exclusão e mantém o banco', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByText('Banco do Brasil')).toBeVisible()
  })
})
