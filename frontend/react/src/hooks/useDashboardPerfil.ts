import { useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { ROLES, ROLE_PRECEDENCIA, type RoleValue } from '@/utils/constants'
import type { DashboardPerfil } from '@/types'

const ROLE_TO_PERFIL: Record<RoleValue, DashboardPerfil> = {
  [ROLES.ADMIN]:        'admin',
  [ROLES.SINDICO]:      'sindico',
  [ROLES.PROPRIETARIO]: 'proprietario',
  [ROLES.MORADOR]:      'morador',
  [ROLES.USER]:         'usuario',
}

/**
 * Resolve o perfil de dashboard com base nos roles do usuário.
 *
 * Precedência: ADMIN > SINDICO > PROPRIETARIO > MORADOR > USER
 * podeAlternar = true quando o usuário possui mais de um perfil relevante.
 */
export function useDashboardPerfil() {
  const hasRole = useAuthStore((s) => s.hasRole)

  const perfisDisponiveis: DashboardPerfil[] = ROLE_PRECEDENCIA
    .filter((role) => hasRole(role))
    .map((role) => ROLE_TO_PERFIL[role])

  if (perfisDisponiveis.length === 0) perfisDisponiveis.push('usuario')

  const [perfilAtivo, setPerfilAtivo] = useState<DashboardPerfil>(perfisDisponiveis[0])

  return {
    perfil: perfilAtivo,
    perfisDisponiveis,
    podeAlternar: perfisDisponiveis.length > 1,
    setPerfil: setPerfilAtivo,
  }
}
