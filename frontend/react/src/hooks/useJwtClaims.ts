import { jwtDecode } from 'jwt-decode'
import { useAuthStore } from '@/store/authStore'
import type { MoradorJwtClaims, ProprietarioJwtClaims } from '@/types'

/** Returns the apartamento_id from the JWT claims, or null if absent. */
export function useMoradorApartamentoId(): number | null {
  const token = useAuthStore((s) => s.accessToken)
  if (!token) return null
  try {
    return jwtDecode<MoradorJwtClaims>(token).apartamento_id ?? null
  } catch {
    return null
  }
}

/** Returns user_id (used as proprietario id) and apartamentos_ids_proprietario from the JWT claims. */
export function useProprietarioClaims(): {
  proprietarioId: number | null
  apartamentoIds: number[]
} {
  const token = useAuthStore((s) => s.accessToken)
  if (!token) return { proprietarioId: null, apartamentoIds: [] }
  try {
    const decoded = jwtDecode<ProprietarioJwtClaims>(token)
    return {
      proprietarioId: decoded.user_id ?? null,
      apartamentoIds: decoded.apartamentos_ids_proprietario ?? [],
    }
  } catch {
    return { proprietarioId: null, apartamentoIds: [] }
  }
}
