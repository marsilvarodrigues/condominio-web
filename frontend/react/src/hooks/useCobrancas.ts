import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { cobrancasApi } from '@/api/cobrancas.api'
import type { CobrancaFilterDTO, GerarCobrancasDTO } from '@/types'

/**
 * Returns the list of charge summaries for the given apartment.
 * Data is cached for 2 minutes and only fetched when apartamentoId is truthy.
 */
export function useCobrancasDoApartamento(apartamentoId: number) {
  return useQuery({
    queryKey: ['cobrancas', 'apartamento', apartamentoId],
    queryFn: () => cobrancasApi.porApartamento(apartamentoId),
    staleTime: 2 * 60 * 1000,
    enabled: !!apartamentoId,
  })
}

/**
 * Returns cancellation and email-resend mutations scoped to the given apartment.
 * Both mutations automatically invalidate the apartment charge list on success.
 */
export function useCobrancaMutations(apartamentoId: number) {
  const qc = useQueryClient()
  const key = ['cobrancas', 'apartamento', apartamentoId]

  const cancelar = useMutation({
    mutationFn: ({ id, motivo }: { id: number; motivo: string }) =>
      cobrancasApi.cancelar(id, motivo),
    onSuccess: () => qc.invalidateQueries({ queryKey: key }),
  })

  const reenviarEmail = useMutation({
    mutationFn: (id: number) => cobrancasApi.reenviarEmail(id),
  })

  return { cancelar, reenviarEmail }
}

/**
 * Paginated list of charges for the active condomínio with optional filters.
 */
export function useCobrancas(filter: Partial<CobrancaFilterDTO> = {}, page = 0) {
  return useQuery({
    queryKey: ['cobrancas', 'lista', filter, page],
    queryFn: () => cobrancasApi.list(filter, page),
    staleTime: 2 * 60_000,
  })
}

/**
 * Mutation to generate charges for all apartments of a rateio execution.
 */
export function useGerarCobrancas() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: GerarCobrancasDTO) => cobrancasApi.gerar(dto),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cobrancas'] }),
  })
}

/**
 * Mutation to cancel a charge with a human-readable reason.
 */
export function useCancelarCobranca() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, motivo }: { id: number; motivo: string }) =>
      cobrancasApi.cancelar(id, motivo),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['cobrancas'] }),
  })
}
