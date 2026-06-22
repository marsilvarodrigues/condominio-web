import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import AcessoNegadoPage from '@/pages/AcessoNegadoPage'
import type { ReactNode } from 'react'

interface Props {
  children: ReactNode
  requiredRole?: string
  requiredRoles?: string[]
}

export function ProtectedRoute({ children, requiredRole, requiredRoles }: Props) {
  const location = useLocation()
  const { isAuthenticated, hasRole } = useAuthStore()

  if (!isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  const roles = requiredRoles ?? (requiredRole ? [requiredRole] : [])
  if (roles.length > 0 && !roles.some((r) => hasRole(r))) {
    return <AcessoNegadoPage />
  }

  return <>{children}</>
}
