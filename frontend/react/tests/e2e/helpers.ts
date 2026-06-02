import type { Page, Route } from '@playwright/test'

// Token JWT de teste: sub=admin@test.com, roles=[ROLE_ADMIN], exp=muito-futuro
export const ADMIN_TOKEN =
  'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSIsInJvbGVzIjpbIlJPTEVfQURNSU4iXSwiY29uZG9taW5pb19pZHMiOltdLCJleHAiOjk5OTk5OTk5OTl9.placeholder'

/** Injeta o estado de autenticação no localStorage da página. */
export async function setAdminAuth(page: Page): Promise<void> {
  await page.goto('/login')
  await page.evaluate((token) => {
    localStorage.setItem(
      'condogest-auth',
      JSON.stringify({
        state: {
          accessToken: token,
          refreshToken: 'refresh-token',
          user: { email: 'admin@test.com', roles: ['ROLE_ADMIN'], condominioIds: [] },
          activeCondominioId: null,
        },
        version: 0,
      }),
    )
  }, ADMIN_TOKEN)
}

/** Cria um payload de resposta no formato ApiResponse<T>. */
export function apiOk<T>(data: T) {
  return {
    requestId: 'test-req',
    timestamp: new Date().toISOString(),
    data,
  }
}

/** Registra um mock GET que devolve `data` para a URL dada. */
export function mockGet<T>(page: Page, urlPattern: string, data: T): void {
  page.route(urlPattern, (route: Route) => {
    if (route.request().method() === 'GET') {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(apiOk(data)),
      })
    } else {
      route.continue()
    }
  })
}

/** Registra um mock POST/PUT/DELETE que devolve 200/201/204. */
export function mockMutation(
  page: Page,
  urlPattern: string,
  responseData?: unknown,
  status = 200,
): void {
  page.route(urlPattern, (route: Route) => {
    const method = route.request().method()
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      route.fulfill({
        status,
        contentType: 'application/json',
        body: responseData ? JSON.stringify(apiOk(responseData)) : '{}',
      })
    } else {
      route.continue()
    }
  })
}
