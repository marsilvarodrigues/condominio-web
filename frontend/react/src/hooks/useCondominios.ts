import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { condominiosApi } from '@/api/condominios.api'
import type { CreateCondominioDTO } from '@/types'

const QUERY_KEY = ['condominios'] as const

export function useCondominios() {
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: condominiosApi.list,
    staleTime: 5 * 60 * 1000,
  })
}

export function useCondominioMutations() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: QUERY_KEY })

  const create = useMutation({
    mutationFn: (body: CreateCondominioDTO) => condominiosApi.create(body),
    onSuccess: invalidate,
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<CreateCondominioDTO> }) =>
      condominiosApi.update(id, body),
    onSuccess: invalidate,
  })

  const remove = useMutation({
    mutationFn: (id: number) => condominiosApi.remove(id),
    onSuccess: invalidate,
  })

  return { create, update, remove }
}
