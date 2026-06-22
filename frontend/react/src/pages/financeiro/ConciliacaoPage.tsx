import {
  Box,
  Chip,
  MenuItem,
  TextField,
  Typography,
  Paper,
  Stack,
} from '@mui/material'
import AccountBalanceIcon from '@mui/icons-material/AccountBalance'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PageHeader, DataTable, NoCondominioGuard, type Column } from '@/components/common'
import { conciliacaoApi } from '@/api/financeiro/conciliacao.api'
import { contasBancariasApi } from '@/api/financeiro/bancos.api'
import { formatCurrency, formatDate } from '@/utils/formatters'
import type { LancamentoBancarioDTO } from '@/types'

const STATUS_LABELS: Record<string, string> = {
  PENDENTE: 'Pendente',
  CONCILIADO: 'Conciliado',
}

const ORIGEM_LABELS: Record<string, string> = {
  COTA_CONDOMINIO: 'Cota',
  RESERVA: 'Reserva',
  DESPESA_ORDINARIA: 'Despesa Ordinária',
  DESPESA_EXTRAORDINARIA: 'Despesa Extra',
  TAXA_EXTRA: 'Taxa Extra',
  MULTA: 'Multa',
  JUROS: 'Juros',
  MANUAL: 'Manual',
  IMPORTACAO: 'Importação',
}

export default function ConciliacaoPage() {
  const [contaId, setContaId] = useState<number | ''>('')
  const [statusFiltro, setStatusFiltro] = useState<string>('TODOS')

  const { data: contas = [] } = useQuery({
    queryKey: ['contas-bancarias'],
    queryFn: () => contasBancariasApi.list(),
  })

  const { data: lancamentos = [], isLoading } = useQuery({
    queryKey: ['conciliacao-lancamentos', contaId],
    queryFn: () => conciliacaoApi.listarLancamentos(contaId as number),
    enabled: !!contaId,
  })

  const itensFiltrados = statusFiltro === 'TODOS'
    ? lancamentos
    : lancamentos.filter((l) => l.status === statusFiltro)

  const totalPendente = lancamentos.filter((l) => l.status === 'PENDENTE').length
  const totalConciliado = lancamentos.filter((l) => l.status === 'CONCILIADO').length

  const columns: Column<LancamentoBancarioDTO>[] = [
    { key: 'dataLancamento', header: 'Data', render: (r) => formatDate(r.dataLancamento) },
    { key: 'descricao', header: 'Descrição', render: (r) => r.descricao },
    {
      key: 'origem',
      header: 'Origem',
      render: (r) => (
        <Chip label={ORIGEM_LABELS[r.origem] ?? r.origem} size="small" variant="outlined" />
      ),
    },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (r) => (
        <Chip
          label={r.tipo === 'CREDITO' ? 'Crédito' : 'Débito'}
          size="small"
          color={r.tipo === 'CREDITO' ? 'success' : 'error'}
          variant="outlined"
        />
      ),
    },
    {
      key: 'valor',
      header: 'Valor',
      align: 'right',
      render: (r) => (
        <Typography variant="body2" fontWeight={600} color={r.tipo === 'CREDITO' ? 'success.main' : 'error.main'}>
          {formatCurrency(r.valor)}
        </Typography>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => (
        <Chip
          label={STATUS_LABELS[r.status] ?? r.status}
          size="small"
          color={r.status === 'CONCILIADO' ? 'success' : 'warning'}
        />
      ),
    },
  ]

  return (
    <Box>
      <NoCondominioGuard />
      <PageHeader
        title="Conciliação Bancária"
        subtitle="Visualize os lançamentos bancários e seu status de conciliação."
        actions={
          <Stack direction="row" spacing={1} alignItems="center">
            <TextField
              select
              label="Conta Bancária"
              size="small"
              value={contaId}
              onChange={(e) => setContaId(Number(e.target.value))}
              sx={{ minWidth: 220 }}
            >
              {contas.map((c) => (
                <MenuItem key={c.id} value={c.id}>
                  {c.bancoNome} — {c.agencia}/{c.conta}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Status"
              size="small"
              value={statusFiltro}
              onChange={(e) => setStatusFiltro(e.target.value)}
              sx={{ minWidth: 140 }}
              disabled={!contaId}
            >
              <MenuItem value="TODOS">Todos</MenuItem>
              <MenuItem value="PENDENTE">Pendente</MenuItem>
              <MenuItem value="CONCILIADO">Conciliado</MenuItem>
            </TextField>
          </Stack>
        }
      />

      {!contaId ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <AccountBalanceIcon sx={{ fontSize: 48, opacity: 0.3, mb: 1 }} />
          <Typography color="text.secondary">
            Selecione uma conta bancária para visualizar os lançamentos.
          </Typography>
        </Paper>
      ) : (
        <>
          {lancamentos.length > 0 && (
            <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
              <Paper sx={{ px: 3, py: 1.5, flex: 1, textAlign: 'center' }}>
                <Typography variant="h5" fontWeight={700} color="warning.main">
                  {totalPendente}
                </Typography>
                <Typography variant="caption" color="text.secondary">Pendentes</Typography>
              </Paper>
              <Paper sx={{ px: 3, py: 1.5, flex: 1, textAlign: 'center' }}>
                <Typography variant="h5" fontWeight={700} color="success.main">
                  {totalConciliado}
                </Typography>
                <Typography variant="caption" color="text.secondary">Conciliados</Typography>
              </Paper>
              <Paper sx={{ px: 3, py: 1.5, flex: 1, textAlign: 'center' }}>
                <Typography variant="h5" fontWeight={700}>
                  {lancamentos.length}
                </Typography>
                <Typography variant="caption" color="text.secondary">Total</Typography>
              </Paper>
            </Stack>
          )}
          <DataTable
            columns={columns}
            rows={itensFiltrados}
            keyField="id"
            loading={isLoading}
            emptyMessage="Nenhum lançamento encontrado para esta conta."
          />
        </>
      )}
    </Box>
  )
}
