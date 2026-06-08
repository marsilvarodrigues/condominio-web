import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const CONDOMINIOS = [
  {
    id: 1,
    nome: 'Residencial Primavera',
    cnpj: '12345678000195',
    email: 'admin@primavera.com',
    endereco: { logradouro: 'Rua das Flores, 100', cep: '01310100', cidade: 'São Paulo', estadoId: 35, estadoUf: 'SP' },
  },
]

test.describe('Condomínios', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/condominios', CONDOMINIOS)
    await page.goto('/condominios')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /condomínios/i })).toBeVisible()
  })

  test('lista condomínios retornados pela API', async ({ page }) => {
    await expect(page.getByText('Residencial Primavera')).toBeVisible()
    await expect(page.getByText('São Paulo')).toBeVisible()
  })

  test('exibe quantidade de condomínios no subtítulo', async ({ page }) => {
    await expect(page.getByText(/1 condomínio/i)).toBeVisible()
  })

  test('abre dialog de criação ao clicar no botão Novo', async ({ page }) => {
    await page.getByRole('button', { name: /novo condomínio/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabel(/nome/i)).toBeVisible()
    await expect(page.getByLabel(/cnpj/i)).toBeVisible()
    await expect(page.getByLabel(/e-mail/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /novo condomínio/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('salva novo condomínio e fecha dialog', async ({ page }) => {
    mockMutation(page, '**/api/condominios', { ...CONDOMINIOS[0], id: 2, nome: 'Novo Condo' }, 201)
    await page.getByRole('button', { name: /novo condomínio/i }).click()
    await page.getByLabel(/^nome/i).fill('Novo Condo')
    await page.getByLabel(/cnpj/i).fill('12345678000195')
    await page.getByLabel(/e-mail/i).fill('novo@condo.com')
    await page.getByLabel(/logradouro/i).fill('Rua Teste, 1')
    await page.getByLabel(/cep/i).fill('01310100')
    await page.getByLabel(/cidade/i).fill('São Paulo')
    await page.getByRole('button', { name: /salvar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre dialog de confirmação ao excluir', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/excluir o condomínio/i)).toBeVisible()
  })

  test('cancela exclusão e mantém o registro', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByText('Residencial Primavera')).toBeVisible()
  })
})
