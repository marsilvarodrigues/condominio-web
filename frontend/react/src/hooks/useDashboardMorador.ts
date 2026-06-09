import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { cobrancasApi } from '@/api/cobrancas.api'
import { dashboardApi } from '@/api/dashboard.api'
import { useMoradorApartamentoId } from './useJwtClaims'

export function useDashboardMorador() {
  const apartamentoId = useMoradorApartamentoId()

  const cobrancas = useQuery({
    queryKey: ['dash', 'mor', 'cobrancas', apartamentoId],
    queryFn: () => cobrancasApi.porApartamento(apartamentoId!, 0, 3),
    enabled: !!apartamentoId,
    staleTime: 2 * 60_000,
  })

  const orcamento = useQuery({
    queryKey: ['dash', 'mor', 'orcamento'],
    queryFn: dashboardApi.orcamentoMorador,
    staleTime: 5 * 60_000,
  })

  const proximoVencimento = useMemo(() => {
    const lista = cobrancas.data?.content ?? []
    return (
      lista
        .filter((c) => c.status === 'PENDENTE' || c.status === 'VENCIDA')
        .sort(
          (a, b) => new Date(a.vencimento).getTime() - new Date(b.vencimento).getTime(),
        )[0] ?? null
    )
  }, [cobrancas.data])

  return { apartamentoId, cobrancas, orcamento, proximoVencimento }
}
