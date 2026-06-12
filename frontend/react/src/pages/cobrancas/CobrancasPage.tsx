import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Paper,
  Snackbar,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import CreditCardIcon from '@mui/icons-material/CreditCard'
import EmailIcon from '@mui/icons-material/Email'
import LinkIcon from '@mui/icons-material/Link'
import CancelIcon from '@mui/icons-material/Cancel'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader, DataTable, type Column } from '@/components/common'
import { StatusChipCobranca } from '@/pages/dashboard/components/StatusChipCobranca'
import { useCobrancas, useCancelarCobranca } from '@/hooks/useCobrancas'
import { cobrancasApi } from '@/api/cobrancas.api'
import { dashboardApi } from '@/api/dashboard.api'
import { GerarCobrancasDialog } from './GerarCobrancasDialog'
import { formatCurrency, formatDate } from '@/utils/formatters'
import type { CobrancaDTO, CobrancaFilterDTO, StatusCobranca } from '@/types'

const STATUS_OPTIONS: { value: StatusCobranca | ''; label: string }[] = [
  { value: '', label: 'Todos' },
  { value: 'PENDENTE', label: 'Pendente' },
  { value: 'ENVIADA', label: 'Enviada' },
  { value: 'PAGA', label: 'Paga' },
  { value: 'VENCIDA', label: 'Vencida' },
  { value: 'CANCELADA', label: 'Cancelada' },
]

export default function CobrancasPage() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState<Partial<CobrancaFilterDTO>>({})
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [cancelTarget, setCancelTarget] = useState<CobrancaDTO | null>(null)
  const [motivoCancelamento, setMotivoCancelamento] = useState('')
  const [snackbar, setSnackbar] = useState('')

  const { data, isLoading } = useCobrancas(filter, page)
  const rows = data?.content ?? []

  const { data: resumo } = useQuery({
    queryKey: ['dash', 'admin', 'cobrancas'],
    queryFn: dashboardApi.resumoCobrancas,
    staleTime: 2 * 60_000,
  })

  const { mutate: cancelar, isPending: cancelando } = useCancelarCobranca()

  const reenviarEmail = useMutation({
    mutationFn: (id: number) => cobrancasApi.reenviarEmail(id),
    onSuccess: () => {
      setSnackbar('E-mail reenviado com sucesso')
      qc.invalidateQueries({ queryKey: ['cobrancas'] })
    },
  })

  const handleCancelar = () => {
    if (!cancelTarget) return
    cancelar(
      { id: cancelTarget.id, motivo: motivoCancelamento },
      {
        onSuccess: () => {
          setCancelTarget(null)
          setMotivoCancelamento('')
          setSnackbar('Cobrança cancelada')
        },
      },
    )
  }

  const columns: Column<CobrancaDTO>[] = [
    {
      key: 'apartamento',
      header: 'APARTAMENTO',
      render: (row) => (
        <Box>
          <Typography variant="body2" fontWeight={500}>
            {row.blocoNome ? `${row.blocoNome} — ` : ''}{row.apartamentoNumero}
          </Typography>
        </Box>
      ),
    },
    {
      key: 'morador',
      header: 'MORADOR',
      render: (row) => (
        <Typography variant="body2" color={row.moradorNome ? 'text.primary' : 'text.disabled'}>
          {row.moradorNome ?? '—'}
        </Typography>
      ),
    },
    {
      key: 'criadaEm',
      header: 'EMITIDA EM',
      render: (row) => <Typography variant="body2">{formatDate(row.criadaEm)}</Typography>,
    },
    {
      key: 'vencimento',
      header: 'VENCIMENTO',
      render: (row) => (
        <Typography
          variant="body2"
          color={row.status === 'VENCIDA' ? '#C62828' : 'text.primary'}
          fontWeight={row.status === 'VENCIDA' ? 600 : 400}
        >
          {formatDate(row.vencimento)}
        </Typography>
      ),
    },
    {
      key: 'valor',
      header: 'VALOR',
      align: 'right',
      render: (row) => (
        <Typography variant="body2" fontWeight={500}>
          {formatCurrency(row.valor)}
        </Typography>
      ),
    },
    {
      key: 'status',
      header: 'STATUS',
      render: (row) => <StatusChipCobranca status={row.status} />,
    },
    {
      key: 'acoes',
      header: 'AÇÕES',
      align: 'center',
      render: (row) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Tooltip title="Reenviar e-mail">
            <span>
              <IconButton
                size="small"
                disabled={row.status === 'PAGA' || row.status === 'CANCELADA' || reenviarEmail.isPending}
                onClick={() => reenviarEmail.mutate(row.id)}
              >
                <EmailIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Abrir boleto">
            <span>
              <IconButton
                size="small"
                disabled={!row.boletoUrl}
                onClick={() => row.boletoUrl && window.open(row.boletoUrl, '_blank')}
              >
                <LinkIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Cancelar">
            <span>
              <IconButton
                size="small"
                color="error"
                disabled={row.status !== 'PENDENTE' && row.status !== 'ENVIADA'}
                onClick={() => { setCancelTarget(row); setMotivoCancelamento('') }}
              >
                <CancelIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </Box>
      ),
    },
  ]

  return (
    <Box>
      <PageHeader
        title="Cobranças"
        subtitle="Financeiro / Cobranças"
        actions={
          <Button
            variant="contained"
            startIcon={<CreditCardIcon />}
            onClick={() => setDialogOpen(true)}
          >
            Gerar Cobranças
          </Button>
        }
      />

      {/* Toolbar */}
      <Paper variant="outlined" sx={{ p: 2, mb: 2, display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
        <TextField
          size="small"
          placeholder="Buscar por apartamento…"
          sx={{ minWidth: 200 }}
          onChange={(e) => {
            const v = e.target.value
            setFilter((f) => ({ ...f, apartamentoId: v ? Number(v) || undefined : undefined }))
            setPage(0)
          }}
        />
        <TextField
          select
          size="small"
          label="Status"
          sx={{ minWidth: 140 }}
          value={filter.status ?? ''}
          onChange={(e) => {
            const v = e.target.value as StatusCobranca | ''
            setFilter((f) => ({ ...f, status: v || undefined }))
            setPage(0)
          }}
        >
          {STATUS_OPTIONS.map((o) => (
            <MenuItem key={o.value} value={o.value}>
              {o.label}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          size="small"
          label="Vencimento de"
          type="date"
          InputLabelProps={{ shrink: true }}
          sx={{ minWidth: 160 }}
          onChange={(e) => { setFilter((f) => ({ ...f, vencimentoDe: e.target.value || undefined })); setPage(0) }}
        />
        <TextField
          size="small"
          label="Vencimento até"
          type="date"
          InputLabelProps={{ shrink: true }}
          sx={{ minWidth: 160 }}
          onChange={(e) => { setFilter((f) => ({ ...f, vencimentoAte: e.target.value || undefined })); setPage(0) }}
        />

        {resumo && resumo.totalPendente > 0 && (
          <Chip
            label={`${formatCurrency(resumo.totalPendente)} pendente`}
            size="small"
            sx={{ bgcolor: '#E3F2FD', color: '#1565C0', fontWeight: 600 }}
          />
        )}
        {resumo && resumo.quantidadeVencida > 0 && (
          <Chip
            label={`${resumo.quantidadeVencida} vencidas`}
            size="small"
            sx={{ bgcolor: '#FCE4EC', color: '#C62828', fontWeight: 600 }}
          />
        )}
      </Paper>

      <DataTable
        columns={columns}
        rows={rows}
        keyField="id"
        loading={isLoading}
        page={page}
        pageSize={20}
        totalCount={data?.totalElements}
        onPageChange={setPage}
        emptyMessage="Nenhuma cobrança encontrada."
        sx={{
          '& .MuiTableRow-root[data-vencida="true"]': { bgcolor: '#FFF5F5' },
        }}
      />

      <GerarCobrancasDialog open={dialogOpen} onClose={() => setDialogOpen(false)} />

      {/* Cancel dialog */}
      <Dialog
        open={!!cancelTarget}
        onClose={() => { setCancelTarget(null); setMotivoCancelamento('') }}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Cancelar Cobrança</DialogTitle>
        <DialogContent>
          <Typography variant="body2" mb={2}>
            Confirma o cancelamento da cobrança de{' '}
            <strong>{cancelTarget ? formatCurrency(cancelTarget.valor) : ''}</strong>?
          </Typography>
          <TextField
            label="Motivo *"
            fullWidth
            size="small"
            value={motivoCancelamento}
            onChange={(e) => setMotivoCancelamento(e.target.value)}
            multiline
            rows={2}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setCancelTarget(null); setMotivoCancelamento('') }} disabled={cancelando}>
            Voltar
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleCancelar}
            disabled={cancelando || !motivoCancelamento.trim()}
          >
            {cancelando ? 'Cancelando…' : 'Confirmar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={!!snackbar}
        autoHideDuration={3000}
        onClose={() => setSnackbar('')}
        message={snackbar}
      />
    </Box>
  )
}
