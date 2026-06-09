import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'
import { dashboardApi } from '@/api/dashboard.api'

export function useDashboardSindico() {
  const condominioId = useAuthStore((s) => s.activeCondominioId)
  const ano = new Date().getFullYear()

  const saldo = useQuery({
    queryKey: ['dash', 'sind', 'saldo', condominioId],
    queryFn: dashboardApi.saldoBancario,
    staleTime: 5 * 60_000,
    enabled: !!condominioId,
  })
  const fundo = useQuery({
    queryKey: ['dash', 'sind', 'fundo', condominioId],
    queryFn: dashboardApi.fundoReserva,
    staleTime: 5 * 60_000,
    enabled: !!condominioId,
  })
  const orcamento = useQuery({
    queryKey: ['dash', 'sind', 'orcamento', condominioId, ano],
    queryFn: () => dashboardApi.orcamentoAno(ano),
    staleTime: 5 * 60_000,
    enabled: !!condominioId,
  })
  const rateio = useQuery({
    queryKey: ['dash', 'sind', 'rateio', condominioId],
    queryFn: dashboardApi.ultimaExecucao,
    staleTime: 2 * 60_000,
    enabled: !!condominioId,
  })
  const cobrancas = useQuery({
    queryKey: ['dash', 'sind', 'cobrancas', condominioId],
    queryFn: dashboardApi.resumoCobrancas,
    staleTime: 2 * 60_000,
    enabled: !!condominioId,
  })
  const apartamentos = useQuery({
    queryKey: ['dash', 'sind', 'apts', condominioId],
    queryFn: dashboardApi.resumoApartamentos,
    staleTime: 10 * 60_000,
    enabled: !!condominioId,
  })

  return { condominioId, saldo, fundo, orcamento, rateio, cobrancas, apartamentos }
}
