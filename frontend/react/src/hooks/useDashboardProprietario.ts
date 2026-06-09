import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { proprietariosApi } from '@/api/moradores.api'
import { useProprietarioClaims } from './useJwtClaims'

export function useDashboardProprietario() {
  const { proprietarioId } = useProprietarioClaims()

  const proprietario = useQuery({
    queryKey: ['dash', 'prop', 'dados'],
    queryFn: () => proprietariosApi.meusImoveis(),
    staleTime: 5 * 60_000,
  })

  const metricas = useMemo(() => {
    const apts = proprietario.data?.apartamentos ?? []
    const condIds = [...new Set(apts.map((a) => a.condominioId))]
    return {
      totalImoveis: apts.length,
      totalCondominios: condIds.length,
    }
  }, [proprietario.data])

  return { proprietario, metricas, proprietarioId }
}
