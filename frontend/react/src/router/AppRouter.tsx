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
const ApartamentoDetailPage = lazy(() => import('@/pages/hierarquia/ApartamentoDetailPage'))
const PlaceholderPage = lazy(() => import('@/pages/dashboard/PlaceholderPage'))
const CobrancasPage = lazy(() => import('@/pages/cobrancas/CobrancasPage'))
const PrestacaoContasPage = lazy(() => import('@/pages/relatorio/PrestacaoContasPage'))

const ADMIN_SINDICO = [ROLES.ADMIN, ROLES.SINDICO]
const HIERARQUIA_ROLES = [ROLES.ADMIN, ROLES.SINDICO, ROLES.PROPRIETARIO, ROLES.MORADOR]

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
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <CondominiosPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="hierarquia"
              element={
                <ProtectedRoute requiredRoles={HIERARQUIA_ROLES}>
                  <HierarquiaPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="hierarquia/apartamentos/:id"
              element={
                <ProtectedRoute requiredRoles={HIERARQUIA_ROLES}>
                  <ApartamentoDetailPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="usuarios"
              element={
                <ProtectedRoute requiredRole={ROLES.ADMIN}>
                  <UsuariosPage />
                </ProtectedRoute>
              }
            />
            <Route path="alterar-senha" element={<ChangePasswordPage />} />

            <Route
              path="financeiro/plano-contas"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <PlanoContasPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="financeiro/orcamento"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <OrcamentoAnualPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="financeiro/fundo-reserva"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <FundoReservaPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="financeiro/bancos"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <BancosPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="financeiro/contas"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <ContasBancariasPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="financeiro/conciliacao"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <ConciliacaoPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="rateio/grupos"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <GruposDespesaPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="rateio/coeficientes"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <CoeficientesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="rateio/simulacao"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <SimulacaoRateioPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="rateio/execucoes"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <ExecucoesRateioPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="cobrancas"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <CobrancasPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="relatorio/prestacao-contas"
              element={
                <ProtectedRoute requiredRoles={ADMIN_SINDICO}>
                  <PrestacaoContasPage />
                </ProtectedRoute>
              }
            />
            <Route path="comunicados" element={<PlaceholderPage titulo="Comunicados" />} />
            <Route path="reservas" element={<PlaceholderPage titulo="Reservas de Áreas Comuns" />} />
            <Route path="proprietario/meus-imoveis" element={<PlaceholderPage titulo="Meus Imóveis" />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
