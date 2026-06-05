import { useQuery } from '@tanstack/react-query'
import { estadosApi } from '@/api/estados.api'

/**
 * Searches estados from the API only when `input` has at least 2 characters.
 * Uses `uf` param for 1-2 char queries (exact match) and `nome` for longer (LIKE).
 */
export function useEstadosSearch(input: string) {
  const q = input.trim()
  const enabled = q.length >= 2
  const params = q.length <= 2 ? { uf: q } : { nome: q }

  return useQuery({
    queryKey: ['estados', params],
    queryFn: () => estadosApi.search(params),
    enabled,
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  })
}
