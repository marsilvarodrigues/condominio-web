import { test, expect } from '@playwright/test'

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    // Simulate logged-in state by injecting auth store state
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
    await page.goto('/')
  })

  test('shows dashboard page title', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /painel|dashboard/i })).toBeVisible()
  })

  test('shows stat cards', async ({ page }) => {
    await expect(page.getByText(/apartamentos/i).first()).toBeVisible()
    await expect(page.getByText(/saldo/i).first()).toBeVisible()
  })

  test('sidebar navigation is visible', async ({ page }) => {
    await expect(page.getByText(/condomínios/i).first()).toBeVisible()
  })
})
