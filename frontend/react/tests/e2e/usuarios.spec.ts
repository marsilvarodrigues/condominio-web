import { test, expect } from '@playwright/test'
import { setAdminAuth, mockGet, mockMutation } from './helpers'

const USUARIOS = [
  {
    id: 1,
    name: 'Admin User',
    email: 'admin@test.com',
    enabled: true,
    roles: ['ROLE_ADMIN'],
    condominioIds: [],
    activationToken: null,
    activationTokenExpiry: null,
  },
  {
    id: 2,
    name: 'Maria Usuária',
    email: 'maria@test.com',
    enabled: false,
    roles: ['ROLE_USER'],
    condominioIds: [],
    activationToken: null,
    activationTokenExpiry: null,
  },
]

test.describe('Usuários', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    mockGet(page, '**/api/usuarios', USUARIOS)
    await page.goto('/usuarios')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /usuários/i })).toBeVisible()
  })

  test('lista usuários retornados pela API', async ({ page }) => {
    await expect(page.getByText('Admin User')).toBeVisible()
    await expect(page.getByText('Maria Usuária')).toBeVisible()
  })

  test('exibe chips de perfil', async ({ page }) => {
    await expect(page.getByText('Admin')).toBeVisible()
    await expect(page.getByText('Usuário')).toBeVisible()
  })

  test('exibe chip de status ativo e inativo', async ({ page }) => {
    await expect(page.getByText('Ativo')).toBeVisible()
    await expect(page.getByText('Inativo')).toBeVisible()
  })

  test('exibe quantidade de usuários no subtítulo', async ({ page }) => {
    await expect(page.getByText(/2 usuário/i)).toBeVisible()
  })

  test('abre dialog de criação ao clicar em Novo Usuário', async ({ page }) => {
    await page.getByRole('button', { name: /novo usuário/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/^nome/i)).toBeVisible()
    await expect(page.getByLabelText(/e-mail/i)).toBeVisible()
  })

  test('fecha dialog ao cancelar', async ({ page }) => {
    await page.getByRole('button', { name: /novo usuário/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('abre dialog de confirmação ao excluir', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText(/deseja excluir o usuário/i)).toBeVisible()
  })

  test('cancela exclusão e mantém o usuário', async ({ page }) => {
    await page.getByRole('button', { name: /excluir/i }).first().click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByText('Admin User')).toBeVisible()
  })
})
