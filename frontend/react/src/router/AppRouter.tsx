import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { CircularProgress, Box } from '@mui/material'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from './ProtectedRoute'
import { ROLES } from '@/utils/constants'

const LoginPage = lazy(() => import('@/pages/auth/LoginPage'))
const ChangePasswordPage = lazy(() => import('@/pages/auth/ChangePasswordPage'))
const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage'))
const CondominiosPage = lazy(() => import('@/pages/condominios/CondominiosPage'))
const HierarquiaPage = lazy(() => import('@/pages/hierarquia/HierarquiaPage'))
const UsuariosPage = lazy(() => import('@/pages/usuarios/UsuariosPage'))
const PlanoContasPage = lazy(() => import('@/pages/financeiro/PlanoContasPage'))
const OrcamentoAnualPage = lazy(() => import('@/pages/financeiro/OrcamentoAnualPage'))
const FundoReservaPage = lazy(() => import('@/pages/financeiro/FundoReservaPage'))
const BancosPage = lazy(() => import('@/pages/financeiro/BancosPage'))
const ContasBancariasPage = lazy(() => import('@/pages/financeiro/ContasBancariasPage'))
const ConciliacaoPage = lazy(() => import('@/pages/financeiro/ConciliacaoPage'))
const GruposDespesaPage = lazy(() => import('@/pages/financeiro/GruposDespesaPage'))
const CoeficientesPage = lazy(() => import('@/pages/financeiro/CoeficientesPage'))
const SimulacaoRateioPage = lazy(() => import('@/pages/financeiro/SimulacaoRateioPage'))
const ExecucoesRateioPage = lazy(() => import('@/pages/financeiro/ExecucoesRateioPage'))

function Loader() {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
      <CircularProgress />
    </Box>
  )
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<Loader />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />

            <Route
              path="condominios"
              element={
                <ProtectedRoute requiredRole={ROLES.ADMIN}>
                  <CondominiosPage />
                </ProtectedRoute>
              }
            />
            <Route path="hierarquia" element={<HierarquiaPage />} />
            <Route
              path="usuarios"
              element={
                <ProtectedRoute requiredRole={ROLES.ADMIN}>
                  <UsuariosPage />
                </ProtectedRoute>
              }
            />
            <Route path="alterar-senha" element={<ChangePasswordPage />} />

            <Route path="financeiro/plano-contas" element={<PlanoContasPage />} />
            <Route path="financeiro/orcamento" element={<OrcamentoAnualPage />} />
            <Route path="financeiro/fundo-reserva" element={<FundoReservaPage />} />
            <Route path="financeiro/bancos" element={<BancosPage />} />
            <Route path="financeiro/contas" element={<ContasBancariasPage />} />
            <Route path="financeiro/conciliacao" element={<ConciliacaoPage />} />

            <Route path="rateio/grupos" element={<GruposDespesaPage />} />
            <Route path="rateio/coeficientes" element={<CoeficientesPage />} />
            <Route path="rateio/simulacao" element={<SimulacaoRateioPage />} />
            <Route path="rateio/execucoes" element={<ExecucoesRateioPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
