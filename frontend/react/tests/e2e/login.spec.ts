import { test, expect } from '@playwright/test'

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('displays login form', async ({ page }) => {
    await expect(page.getByLabel(/e-mail/i)).toBeVisible()
    await expect(page.getByLabel(/senha/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /entrar/i })).toBeVisible()
  })

  test('shows validation errors on empty submit', async ({ page }) => {
    await page.getByRole('button', { name: /entrar/i }).click()
    await expect(page.getByText(/e-mail inválido/i)).toBeVisible()
  })

  test('shows error for invalid email format', async ({ page }) => {
    await page.getByLabel(/e-mail/i).fill('notanemail')
    await page.getByRole('button', { name: /entrar/i }).click()
    await expect(page.getByText(/e-mail inválido/i)).toBeVisible()
  })

  test('redirects to dashboard on successful login', async ({ page }) => {
    await page.route('**/api/auth/login', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          requestId: 'test',
          timestamp: new Date().toISOString(),
          data: {
            accessToken: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiY29uZG9taW5pb19pZHMiOltdLCJleHAiOjk5OTk5OTk5OTl9.placeholder',
            refreshToken: 'refresh-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
          },
        }),
      }),
    )

    await page.getByLabel(/e-mail/i).fill('admin@test.com')
    await page.getByLabel(/senha/i).fill('password123')
    await page.getByRole('button', { name: /entrar/i }).click()

    await expect(page).toHaveURL(/\/$|\/dashboard/)
  })
})
