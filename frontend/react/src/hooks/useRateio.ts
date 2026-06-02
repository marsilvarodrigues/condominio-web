import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { gruposDespesaApi, coeficientesApi, rateioApi } from '@/api/financeiro/rateio.api'
import type {
  CreateGrupoDespesaDTO,
  GrupoDespesaDTO,
  CreateCoeficienteRateioDTO,
  SimularRateioRequest,
  RecalcularRateioRequest,
} from '@/types'

const GRUPOS_KEY = ['grupos-despesa'] as const
const COEF_KEY = (grupoDespesaId: number) => ['coeficientes', grupoDespesaId] as const
const EXEC_KEY = ['rateio-execucoes'] as const

export function useGruposDespesa() {
  return useQuery({ queryKey: GRUPOS_KEY, queryFn: gruposDespesaApi.list })
}

export function useGruposDespesaMutations() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: GRUPOS_KEY })

  const create = useMutation({
    mutationFn: (body: CreateGrupoDespesaDTO) => gruposDespesaApi.create(body),
    onSuccess: invalidate,
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<GrupoDespesaDTO> }) =>
      gruposDespesaApi.update(id, body),
    onSuccess: invalidate,
  })

  const remove = useMutation({
    mutationFn: (id: number) => gruposDespesaApi.remove(id),
    onSuccess: invalidate,
  })

  return { create, update, remove }
}

export function useCoeficientesGrupo(grupoDespesaId: number) {
  return useQuery({
    queryKey: COEF_KEY(grupoDespesaId),
    queryFn: () => coeficientesApi.listByGrupo(grupoDespesaId),
    enabled: grupoDespesaId > 0,
  })
}

export function useCoeficientesMutations(grupoDespesaId: number) {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: COEF_KEY(grupoDespesaId) })

  const create = useMutation({
    mutationFn: (body: CreateCoeficienteRateioDTO) =>
      coeficientesApi.create(grupoDespesaId, body),
    onSuccess: invalidate,
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<CreateCoeficienteRateioDTO> }) =>
      coeficientesApi.update(grupoDespesaId, id, body),
    onSuccess: invalidate,
  })

  const remove = useMutation({
    mutationFn: (id: number) => coeficientesApi.remove(grupoDespesaId, id),
    onSuccess: invalidate,
  })

  return { create, update, remove }
}

export function useSimularRateio() {
  return useMutation({
    mutationFn: (body: SimularRateioRequest) => rateioApi.simular(body),
  })
}

export function useRecalcularRateio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: RecalcularRateioRequest) => rateioApi.recalcular(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: EXEC_KEY }),
  })
}

export function useExecucoesRateio(page = 0, size = 20) {
  return useQuery({
    queryKey: [...EXEC_KEY, page, size],
    queryFn: () => rateioApi.listarExecucoes({ page, size }),
  })
}
