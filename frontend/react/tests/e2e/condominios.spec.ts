import { test, expect } from '@playwright/test'

test.describe('Condomínios Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.evaluate(() => {
      const authState = {
        state: {
          accessToken: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiY29uZG9taW5pb19pZHMiOltdLCJleHAiOjk5OTk5OTk5OTl9.placeholder',
          refreshToken: 'refresh',
          user: { email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [] },
          activeCondominioId: null,
        },
        version: 0,
      }
      localStorage.setItem('condogest-auth', JSON.stringify(authState))
    })

    await page.route('**/api/condominios', (route) => {
      if (route.request().method() === 'GET') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            requestId: 'test',
            timestamp: new Date().toISOString(),
            data: [
              {
                id: 1,
                nome: 'Residencial Primavera',
                cnpj: '12345678000195',
                email: 'admin@primavera.com',
                endereco: {
                  logradouro: 'Rua das Flores, 100',
                  cep: '01310100',
                  cidade: 'São Paulo',
                  estadoId: 35,
                  estadoUf: 'SP',
                },
              },
            ],
          }),
        })
      } else {
        route.continue()
      }
    })

    await page.goto('/condominios')
  })

  test('shows page title', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /condomínios/i })).toBeVisible()
  })

  test('lists condominios from API', async ({ page }) => {
    await expect(page.getByText('Residencial Primavera')).toBeVisible()
  })

  test('opens create dialog on button click', async ({ page }) => {
    await page.getByRole('button', { name: /novo condomínio/i }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByLabelText(/nome/i)).toBeVisible()
  })

  test('closes dialog on cancel', async ({ page }) => {
    await page.getByRole('button', { name: /novo condomínio/i }).click()
    await page.getByRole('button', { name: /cancelar/i }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })
})
