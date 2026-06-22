import { Alert, AlertTitle } from '@mui/material'
import { useAuthStore } from '@/store/authStore'

export function NoCondominioGuard() {
  const activeCondominioId = useAuthStore((s) => s.activeCondominioId)
  const user = useAuthStore((s) => s.user)

  const needsSelection = activeCondominioId === null && (user?.condominioIds.length ?? 0) > 1

  if (!needsSelection) return null

  return (
    <Alert severity="warning" sx={{ mb: 3 }}>
      <AlertTitle>Condomínio não selecionado</AlertTitle>
      Selecione um condomínio na barra superior para operar nesta página.
    </Alert>
  )
}
