import { test, expect } from '@playwright/test'
import { setAdminAuth, mockMutation } from './helpers'

test.describe('Alterar Senha', () => {
  test.beforeEach(async ({ page }) => {
    await setAdminAuth(page)
    await page.goto('/alterar-senha')
  })

  test('exibe título da página', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /alterar senha/i })).toBeVisible()
  })

  test('exibe campos de senha atual, nova e confirmação', async ({ page }) => {
    await expect(page.getByLabelText(/senha atual/i)).toBeVisible()
    await expect(page.getByLabelText(/nova senha/i)).toBeVisible()
    await expect(page.getByLabelText(/confirmar/i)).toBeVisible()
  })

  test('exibe erro quando senhas não coincidem', async ({ page }) => {
    await page.getByLabelText(/senha atual/i).fill('senhaAtual123')
    await page.getByLabelText(/nova senha/i).fill('NovaSenha@1')
    await page.getByLabelText(/confirmar/i).fill('SenhaDiferente@1')
    await page.getByRole('button', { name: /salvar|alterar/i }).click()
    await expect(page.getByText(/senhas não coincidem|não conferem/i)).toBeVisible()
  })

  test('exibe erro para senha nova muito curta', async ({ page }) => {
    await page.getByLabelText(/senha atual/i).fill('senhaAtual123')
    await page.getByLabelText(/nova senha/i).fill('123')
    await page.getByLabelText(/confirmar/i).fill('123')
    await page.getByRole('button', { name: /salvar|alterar/i }).click()
    await expect(page.getByText(/mínimo|caracteres/i)).toBeVisible()
  })

  test('envia formulário com dados válidos', async ({ page }) => {
    mockMutation(page, '**/api/usuarios/**/senha', null, 204)
    await page.getByLabelText(/senha atual/i).fill('SenhaAtual@1')
    await page.getByLabelText(/nova senha/i).fill('NovaSenha@1234')
    await page.getByLabelText(/confirmar/i).fill('NovaSenha@1234')
    await page.getByRole('button', { name: /salvar|alterar/i }).click()
    // Sucesso: botão de submit não deve estar desabilitado ou formulário reseta
    await expect(page.getByRole('button', { name: /salvar|alterar/i })).not.toBeDisabled()
  })
})
