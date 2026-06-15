import { Alert, Box, Grid, Typography } from '@mui/material'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import ApartmentIcon from '@mui/icons-material/Apartment'
import SavingsIcon from '@mui/icons-material/Savings'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/common'
import { formatCurrency, formatPercent, formatDate } from '@/utils/formatters'
import { useAuthStore } from '@/store/authStore'
import { useDashboardSindico } from '@/hooks/useDashboardSindico'
import { StatCard } from './components/StatCard'
import { SectionCard } from './components/SectionCard'

export default function DashboardSindico() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const { condominioId, saldo, fundo, orcamento, rateio, cobrancas, apartamentos } =
    useDashboardSindico()
  const ano = new Date().getFullYear()

  const semCondominio = !condominioId && (user?.condominioIds.length ?? 0) > 1

  return (
    <>
      <PageHeader
        title="Painel do Condomínio"
        subtitle={condominioId ? `Condomínio #${condominioId}` : 'Selecione um condomínio'}
      />

      {semCondominio && (
        <Alert severity="info" sx={{ mb: 3 }}>
          Selecione um condomínio no menu superior para ver os dados.
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Saldo Bancário"
            value={saldo.data ? formatCurrency(saldo.data.saldoTotal) : '—'}
            subtitle={saldo.data ? `${saldo.data.quantidadeContas} contas` : undefined}
            icon={<AccountBalanceWalletIcon />}
            color="#2E7D32"
            loading={saldo.isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title={`Orçamento ${ano}`}
            value={orcamento.data ? formatPercent(orcamento.data.percentualExecucao) : '—'}
            subtitle={
              orcamento.data
                ? `${formatCurrency(orcamento.data.totalRealizado)} de ${formatCurrency(orcamento.data.totalPrevisto)}`
                : undefined
            }
            icon={<TrendingUpIcon />}
            color="#1565C0"
            progress={orcamento.data?.percentualExecucao}
            loading={orcamento.isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Apartamentos"
            value={String(apartamentos.data?.total ?? '—')}
            subtitle={
              apartamentos.data
                ? `${apartamentos.data.ocupados} ocup. · ${apartamentos.data.vagos} vagos`
                : undefined
            }
            icon={<ApartmentIcon />}
            color="#E65100"
            progress={apartamentos.data?.taxaOcupacao}
            loading={apartamentos.isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Fundo de Reserva"
            value={fundo.data ? formatCurrency(fundo.data.saldoAtual) : '—'}
            subtitle={
              fundo.data
                ? `Mínimo: ${formatCurrency(fundo.data.saldoMinimo)}`
                : undefined
            }
            icon={<SavingsIcon />}
            color="#6A1B9A"
            loading={fundo.isLoading}
          />
        </Grid>
      </Grid>

      <Grid container spacing={3} mt={1}>
        <Grid item xs={12} md={8}>
          <SectionCard
            title="Resumo Financeiro"
            loading={orcamento.isLoading || fundo.isLoading}
            empty={!orcamento.data && !fundo.data && !orcamento.isLoading}
            emptyMessage="Sem dados financeiros para este condomínio."
          >
            {[
              {
                label: 'Receita prevista',
                value: orcamento.data ? formatCurrency(orcamento.data.totalPrevisto) : '—',
                color: '#2E7D32',
              },
              {
                label: 'Despesa realizada',
                value: orcamento.data ? formatCurrency(orcamento.data.totalRealizado) : '—',
                color: '#E65100',
              },
              {
                label: 'Fundo de reserva',
                value: fundo.data ? formatCurrency(fundo.data.saldoAtual) : '—',
                color: '#1565C0',
              },
            ].map((item) => (
              <Box key={item.label} sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2" color="text.secondary">
                  {item.label}
                </Typography>
                <Typography variant="body2" fontWeight={600} color={item.color}>
                  {item.value}
                </Typography>
              </Box>
            ))}
          </SectionCard>
        </Grid>
        <Grid item xs={12} md={4}>
          <SectionCard
            title="Último Rateio"
            loading={rateio.isLoading}
            empty={!rateio.data && !rateio.isLoading}
            emptyMessage="Nenhuma execução registrada."
          >
            {rateio.data && (
              <>
                <Typography variant="body2" color="text.secondary">
                  {formatDate(rateio.data.dataExecucao)}
                </Typography>
                <Typography variant="body2" mt={1}>
                  Total: <strong>{formatCurrency(rateio.data.totalRateado)}</strong>
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Status: {rateio.data.status}
                </Typography>
                <Typography
                  variant="body2"
                  mt={1}
                  sx={{ cursor: 'pointer', color: 'primary.main' }}
                  onClick={() => navigate('/rateio/execucoes')}
                >
                  Ver execuções →
                </Typography>
              </>
            )}
          </SectionCard>
        </Grid>
      </Grid>

      {cobrancas.data && (
        <Grid container spacing={3} mt={0}>
          <Grid item xs={12}>
            <SectionCard title="Resumo de Cobranças">
              <Box sx={{ display: 'flex', gap: 4 }}>
                <Box
                  onClick={() => navigate('/cobrancas')}
                  sx={{ cursor: 'pointer', '&:hover': { opacity: 0.8 } }}
                >
                  <Typography variant="caption" color="text.secondary">
                    Pendentes
                  </Typography>
                  <Typography variant="h6" fontWeight={700} color="#1565C0">
                    {formatCurrency(cobrancas.data.totalPendente)}
                  </Typography>
                  <Typography variant="caption">{cobrancas.data.quantidadePendente} cobranças</Typography>
                </Box>
                <Box
                  onClick={() => navigate('/cobrancas')}
                  sx={{ cursor: 'pointer', '&:hover': { opacity: 0.8 } }}
                >
                  <Typography variant="caption" color="text.secondary">
                    Vencidas
                  </Typography>
                  <Typography variant="h6" fontWeight={700} color="#C62828">
                    {formatCurrency(cobrancas.data.totalVencido)}
                  </Typography>
                  <Typography variant="caption">{cobrancas.data.quantidadeVencida} cobranças</Typography>
                </Box>
              </Box>
            </SectionCard>
          </Grid>
        </Grid>
      )}
    </>
  )
}
