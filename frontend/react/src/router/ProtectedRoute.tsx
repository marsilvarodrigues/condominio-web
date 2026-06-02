import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import type { ReactNode } from 'react'

interface Props {
  children: ReactNode
  requiredRole?: string
}

export function ProtectedRoute({ children, requiredRole }: Props) {
  const location = useLocation()
  const { isAuthenticated, hasRole } = useAuthStore()

  if (!isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (requiredRole && !hasRole(requiredRole)) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}
