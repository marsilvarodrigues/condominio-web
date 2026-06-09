import {
  Box,
  Button,
  Grid,
  LinearProgress,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import HomeIcon from '@mui/icons-material/Home'
import CreditCardIcon from '@mui/icons-material/CreditCard'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/common'
import { formatCurrency, formatDate, formatPercent } from '@/utils/formatters'
import { useAuthStore } from '@/store/authStore'
import { useDashboardMorador } from '@/hooks/useDashboardMorador'
import { StatCard } from './components/StatCard'
import { SectionCard } from './components/SectionCard'
import { EmptySection } from './components/EmptySection'
import { StatusChipCobranca } from './components/StatusChipCobranca'

function situacaoColor(status: string | undefined) {
  if (status === 'VENCIDA') return '#C62828'
  if (status === 'PENDENTE') return '#E65100'
  return '#2E7D32'
}

function situacaoLabel(status: string | undefined, total: number) {
  if (!total) return 'Em dia'
  if (status === 'VENCIDA') return 'Em atraso'
  return 'Pendente'
}

export default function DashboardMorador() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const { apartamentoId, cobrancas, orcamento, proximoVencimento } = useDashboardMorador()
  const ano = new Date().getFullYear()

  if (!apartamentoId && !cobrancas.isLoading) {
    return (
      <EmptySection
        icon={<HomeIcon fontSize="large" />}
        message="Sua unidade não está vinculada ao sistema."
        actionLabel="Voltar ao início"
        onAction={() => navigate('/')}
      />
    )
  }

  const cobrancasLista = cobrancas.data?.content ?? []
  const totalAberto =
    cobrancasLista
      .filter((c) => c.status === 'PENDENTE' || c.status === 'VENCIDA')
      .reduce((s, c) => s + Number(c.valor), 0)

  const vencColor =
    proximoVencimento?.status === 'VENCIDA'
      ? '#C62828'
      : proximoVencimento &&
          new Date(proximoVencimento.vencimento).getTime() - Date.now() <= 3 * 86_400_000
        ? '#E65100'
        : '#1565C0'

  return (
    <>
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1565C0 0%, #1976D2 100%)',
          color: 'white',
          borderRadius: 2,
          p: 2,
          mb: 3,
        }}
      >
        <Typography variant="body2">
          Bem-vindo! · {user?.email}
        </Typography>
      </Box>

      <PageHeader title="Minha Unidade" subtitle={`Apartamento #${apartamentoId}`} />

      <Grid container spacing={3}>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="Próximo Vencimento"
            value={proximoVencimento ? formatDate(proximoVencimento.vencimento) : '—'}
            subtitle={proximoVencimento ? formatCurrency(proximoVencimento.valor) : 'Sem cobranças abertas'}
            icon={<CreditCardIcon />}
            color={vencColor}
            loading={cobrancas.isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="Total em Aberto"
            value={formatCurrency(totalAberto)}
            subtitle="pendente + vencido"
            icon={<CreditCardIcon />}
            color="#E65100"
            loading={cobrancas.isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="Situação"
            value={situacaoLabel(proximoVencimento?.status, totalAberto)}
            icon={<CheckCircleIcon />}
            color={situacaoColor(proximoVencimento?.status)}
            loading={cobrancas.isLoading}
          />
        </Grid>
      </Grid>

      <Grid container spacing={3} mt={1}>
        <Grid item xs={12}>
          <SectionCard
            title="Minhas Cobranças"
            action={
              <Button size="small" onClick={() => navigate('/cobrancas')}>
                Ver todas →
              </Button>
            }
            loading={cobrancas.isLoading}
            empty={!cobrancas.isLoading && cobrancasLista.length === 0}
            emptyMessage="Nenhuma cobrança emitida ainda."
          >
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Emitida em</TableCell>
                  <TableCell>Vencimento</TableCell>
                  <TableCell>Valor</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {cobrancasLista.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell>{formatDate(c.criadaEm)}</TableCell>
                    <TableCell sx={{ color: c.status === 'VENCIDA' ? '#C62828' : 'inherit' }}>
                      {formatDate(c.vencimento)}
                    </TableCell>
                    <TableCell>{formatCurrency(c.valor)}</TableCell>
                    <TableCell>
                      <StatusChipCobranca status={c.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </SectionCard>
        </Grid>
      </Grid>

      <Grid container spacing={3} mt={0}>
        <Grid item xs={12}>
          <SectionCard
            title={`Orçamento do Condomínio ${ano}`}
            loading={orcamento.isLoading}
            empty={!orcamento.isLoading && !orcamento.data}
            emptyMessage="Orçamento não disponível para este exercício."
          >
            {orcamento.data && (
              <>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2" color="text.secondary">Total previsto</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {formatCurrency(orcamento.data.totalPrevisto)}
                  </Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2" color="text.secondary">Executado</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {formatPercent(orcamento.data.percentualExecucao)}
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={Math.min(orcamento.data.percentualExecucao, 100)}
                  sx={{ height: 6, borderRadius: 3, mt: 1 }}
                />
                <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                  Valores referentes ao condomínio como um todo.
                </Typography>
              </>
            )}
          </SectionCard>
        </Grid>
      </Grid>
    </>
  )
}
