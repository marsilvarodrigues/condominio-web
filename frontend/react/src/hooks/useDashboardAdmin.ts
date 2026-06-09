import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '@/api/dashboard.api'

export function useDashboardAdmin() {
  const totalCondominios = useQuery({
    queryKey: ['dash', 'admin', 'condominios'],
    queryFn: dashboardApi.totalCondominios,
    staleTime: 10 * 60_000,
  })
  const totalUsuarios = useQuery({
    queryKey: ['dash', 'admin', 'usuarios'],
    queryFn: dashboardApi.totalUsuarios,
    staleTime: 10 * 60_000,
  })
  const cobrancas = useQuery({
    queryKey: ['dash', 'admin', 'cobrancas'],
    queryFn: dashboardApi.resumoCobrancas,
    staleTime: 2 * 60_000,
  })
  const rateio = useQuery({
    queryKey: ['dash', 'admin', 'rateio'],
    queryFn: dashboardApi.ultimaExecucao,
    staleTime: 2 * 60_000,
  })

  return { totalCondominios, totalUsuarios, cobrancas, rateio }
}
