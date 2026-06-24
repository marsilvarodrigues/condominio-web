import { test, expect } from '@playwright/test'
import { ADMIN_TOKEN, mockRefresh } from './helpers'

test.describe('Login', () => {
  test.beforeEach(async ({ page }) => {
    // Garante que não há sessão ativa
    await page.goto('/login')
    await page.evaluate(() => sessionStorage.removeItem('condogest-auth'))
  })

  test('exibe formulário com campos e-mail, senha e botão', async ({ page }) => {
    await expect(page.getByLabel(/e-mail/i)).toBeVisible()
    await expect(page.getByRole('textbox', { name: /senha/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /entrar/i })).toBeVisible()
  })

  test('exibe erro de validação para e-mail inválido', async ({ page }) => {
    await page.getByLabel(/e-mail/i).fill('nao-e-email')
    await page.getByRole('button', { name: /entrar/i }).click()
    await expect(page.getByText(/e-mail inválido/i)).toBeVisible()
  })

  test('exibe erro quando senha está vazia', async ({ page }) => {
    await page.getByLabel(/e-mail/i).fill('admin@test.com')
    await page.getByRole('button', { name: /entrar/i }).click()
    await expect(page.getByText(/senha obrigatória/i)).toBeVisible()
  })

  test('botão mostra/oculta senha funciona', async ({ page }) => {
    const senhaInput = page.getByRole('textbox', { name: /senha/i })
    await expect(senhaInput).toHaveAttribute('type', 'password')
    await page.getByRole('button', { name: /mostrar|ocultar/i }).click()
    await expect(senhaInput).toHaveAttribute('type', 'text')
    await page.getByRole('button', { name: /mostrar|ocultar/i }).click()
    await expect(senhaInput).toHaveAttribute('type', 'password')
  })

  test('redireciona para o dashboard após login bem-sucedido', async ({ page }) => {
    // /auth/login devolve AuthResponseDTO plano — sem envelope ApiResponse (diferente dos demais endpoints).
    await page.route('**/api/auth/login', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ accessToken: ADMIN_TOKEN, refreshToken: 'refresh', tokenType: 'Bearer', expiresIn: 3600 }),
      }),
    )
    await page.getByLabel(/e-mail/i).fill('admin@test.com')
    await page.getByRole('textbox', { name: /senha/i }).fill('Senha@1234')
    await page.getByRole('button', { name: /entrar/i }).click()
    await expect(page).toHaveURL(/\/$|\/dashboard/)
  })

  test('exibe mensagem de erro em credenciais inválidas', async ({ page }) => {
    await page.route('**/api/auth/login', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Unauthorized' }),
      }),
    )
    await page.getByLabel(/e-mail/i).fill('admin@test.com')
    await page.getByRole('textbox', { name: /senha/i }).fill('senhaErrada')
    await page.getByRole('button', { name: /entrar/i }).click()
    await expect(page.getByRole('alert')).toBeVisible()
  })

  test('usuário já autenticado é redirecionado para o dashboard', async ({ page }) => {
    await mockRefresh(page, ADMIN_TOKEN)
    await page.evaluate(() => {
      sessionStorage.setItem('condogest-auth', JSON.stringify({
        state: { refreshToken: 'r', user: { id: 1, email: 'a@a.com', roles: ['ROLE_ADMIN'], condominioIds: [] }, activeCondominioId: null },
        version: 0,
      }))
    })
    await page.goto('/login')
    await expect(page).toHaveURL(/\/$|\/dashboard/)
  })
})
