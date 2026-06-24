import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material'
import { theme } from '@/theme/theme'
import { useAuthStore } from '@/store/authStore'
import App from '@/App'

// App is just `<AppRouter />`. AppRouter renders its own hardcoded <BrowserRouter>,
// so we don't wrap it in another router here — only the providers that normally
// sit above it in main.tsx (QueryClientProvider, ThemeProvider).
function renderApp() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <App />
      </ThemeProvider>
    </QueryClientProvider>,
  )
}

describe('App', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/')
    useAuthStore.setState({
      accessToken: null,
      refreshToken: null,
      user: null,
      activeCondominioId: null,
    })
  })

  afterEach(() => {
    window.history.pushState({}, '', '/')
  })

  it('renderiza sem quebrar e navega para a tela de login quando não autenticado', async () => {
    renderApp()

    // Unauthenticated + no refreshToken => bootstrap() resolves immediately,
    // ProtectedRoute redirects "/" to "/login", and the lazy LoginPage renders.
    expect(await screen.findByText('CondoGest')).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toBeInTheDocument()
  })
})
