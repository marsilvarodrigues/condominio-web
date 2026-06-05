import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { pessoasApi, proprietariosApi } from '@/api/moradores.api'
import type {
  CreatePessoaDTO,
  UpdatePessoaDTO,
  PessoaFilterDTO,
  CreateProprietarioDTO,
  UpdateProprietarioDTO,
  ProprietarioFilterDTO,
} from '@/types'

const PESSOAS_KEY = ['pessoas'] as const
const PROPRIETARIOS_KEY = ['proprietarios'] as const
const moradoresAptKey = (aptId: number) => ['moradores-apt', aptId] as const
const proprietariosAptKey = (aptId: number) => ['proprietarios-apt', aptId] as const

// ── Apartment-scoped hooks (primary usage) ────────────────────────────────────

export function useMoradoresDeApartamento(aptId: number) {
  const qc = useQueryClient()
  const key = moradoresAptKey(aptId)
  const invalidate = () => qc.invalidateQueries({ queryKey: key })

  const query = useQuery({
    queryKey: key,
    queryFn: () =>
      pessoasApi
        .list({ apartamentoId: aptId } as PessoaFilterDTO & { apartamentoId?: number })
        .then((p) => p.content),
    enabled: aptId > 0,
  })

  const create = useMutation({
    mutationFn: (body: CreatePessoaDTO) => pessoasApi.create(body),
    onSuccess: invalidate,
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdatePessoaDTO }) =>
      pessoasApi.update(id, body),
    onSuccess: invalidate,
  })

  const remove = useMutation({
    mutationFn: (id: number) => pessoasApi.remove(id),
    onSuccess: invalidate,
  })

  return { query, create, update, remove }
}

export function useProprietariosDeApartamento(aptId: number) {
  const qc = useQueryClient()
  const key = proprietariosAptKey(aptId)
  const invalidate = () => qc.invalidateQueries({ queryKey: key })

  const query = useQuery({
    queryKey: key,
    queryFn: () => proprietariosApi.listByApartamento(aptId),
    enabled: aptId > 0,
  })

  const create = useMutation({
    mutationFn: async (body: CreateProprietarioDTO) => {
      const created = await proprietariosApi.create(body)
      await proprietariosApi.associarApartamento(created.id, aptId)
      return created
    },
    onSuccess: invalidate,
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateProprietarioDTO }) =>
      proprietariosApi.update(id, body),
    onSuccess: invalidate,
  })

  const desassociar = useMutation({
    mutationFn: (propId: number) => proprietariosApi.desassociarApartamento(propId, aptId),
    onSuccess: invalidate,
  })

  return { query, create, update, desassociar }
}

// ── Global hooks (used by admin queries / reports) ────────────────────────────

export function usePessoas(filter?: PessoaFilterDTO) {
  return useQuery({
    queryKey: [...PESSOAS_KEY, filter],
    queryFn: () => pessoasApi.list(filter),
  })
}

export function usePessoaMutations() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: PESSOAS_KEY })

  const create = useMutation({
    mutationFn: (body: CreatePessoaDTO) => pessoasApi.create(body),
    onSuccess: invalidate,
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdatePessoaDTO }) =>
      pessoasApi.update(id, body),
    onSuccess: invalidate,
  })

  const remove = useMutation({
    mutationFn: (id: number) => pessoasApi.remove(id),
    onSuccess: invalidate,
  })

  return { create, update, remove }
}

export function useProprietarios(filter?: ProprietarioFilterDTO) {
  return useQuery({
    queryKey: [...PROPRIETARIOS_KEY, filter],
    queryFn: () => proprietariosApi.list(filter),
  })
}
