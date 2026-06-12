import React from 'react'
import ReactDOM from 'react-dom/client'
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ThemeProvider, CssBaseline } from '@mui/material'
import { LocalizationProvider } from '@mui/x-date-pickers'
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFnsV3'
import { ptBR as dateFnsPtBR } from 'date-fns/locale'
import type { AxiosError } from 'axios'
import { theme } from '@/theme/theme'
import { AppRouter } from '@/router/AppRouter'
import { GlobalSnackbar, AppErrorBoundary } from '@/components/common'
import { useNotificationStore } from '@/store/notificationStore'

function extractErrorMessage(error: unknown): string {
  const axiosErr = error as AxiosError<{ message?: string; error?: string }>
  if (axiosErr?.response?.data) {
    const d = axiosErr.response.data
    return d.message ?? d.error ?? axiosErr.message
  }
  if (error instanceof Error) return error.message
  return 'Erro inesperado. Tente novamente.'
}

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      // 401 is handled by the axios interceptor (logout + redirect); skip duplicate toast.
      const status = (error as AxiosError)?.response?.status
      if (status === 401) return
      useNotificationStore.getState().notifyError(extractErrorMessage(error))
    },
  }),
  mutationCache: new MutationCache({
    onError: (error) => {
      const status = (error as AxiosError)?.response?.status
      if (status === 401) return
      useNotificationStore.getState().notifyError(extractErrorMessage(error))
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={dateFnsPtBR}>
          <CssBaseline />
          <AppErrorBoundary>
            <AppRouter />
          </AppErrorBoundary>
          <GlobalSnackbar />
        </LocalizationProvider>
      </ThemeProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>,
)
