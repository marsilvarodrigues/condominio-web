import {
  Box,
  Button,
  Chip,
  IconButton,
  MenuItem,
  TextField,
  Tooltip,
  Typography,
  Paper,
  Alert,
  Stack,
} from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import LinkIcon from '@mui/icons-material/Link'
import LinkOffIcon from '@mui/icons-material/LinkOff'
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh'
import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader, DataTable, type Column } from '@/components/common'
import { conciliacaoApi } from '@/api/financeiro/conciliacao.api'
import { contasBancariasApi } from '@/api/financeiro/bancos.api'
import { formatCurrency, formatDate } from '@/utils/formatters'
import { STATUS_CONCILIACAO_LABELS } from '@/utils/constants'
import type { ConciliacaoItemDTO } from '@/types'

export default function ConciliacaoPage() {
  const qc = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [contaId, setContaId] = useState<number | ''>('')
  const [importError, setImportError] = useState<string | null>(null)
  const [importSuccess, setImportSuccess] = useState(false)

  const { data: contas = [] } = useQuery({
    queryKey: ['contas-bancarias'],
    queryFn: () => contasBancariasApi.list(),
  })

  const { data: itens = [], isLoading } = useQuery({
    queryKey: ['conciliacao', contaId],
    queryFn: () => conciliacaoApi.listarItens(contaId as number),
    enabled: !!contaId,
  })

  const importar = useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => conciliacaoApi.importarExtrato(id, file),
    onSuccess: () => {
      setImportSuccess(true)
      setImportError(null)
      qc.invalidateQueries({ queryKey: ['conciliacao', contaId] })
    },
    onError: () => setImportError('Erro ao importar o extrato. Verifique o formato do arquivo.'),
  })

  const associar = useMutation({
    mutationFn: ({ itemId, lancamentoId }: { itemId: number; lancamentoId: number }) =>
      conciliacaoApi.associar(itemId, lancamentoId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['conciliacao', contaId] }),
  })

  const desassociar = useMutation({
    mutationFn: (itemId: number) => conciliacaoApi.desassociar(itemId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['conciliacao', contaId] }),
  })

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !contaId) return
    setImportSuccess(false)
    importar.mutate({ id: contaId as number, file })
    e.target.value = ''
  }

  const statusColor = (status: string) => {
    const map: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
      CONCILIADO: 'success',
      PENDENTE: 'warning',
      DIVERGENTE: 'error',
    }
    return map[status] ?? 'default'
  }

  const columns: Column<ConciliacaoItemDTO>[] = [
    { key: 'data', header: 'Data', render: (r) => formatDate(r.data) },
    { key: 'descricao', header: 'Descrição', render: (r) => r.descricao },
    {
      key: 'valor',
      header: 'Valor',
      align: 'right',
      render: (r) => (
        <Typography variant="body2" fontWeight={600} color={r.valor >= 0 ? 'success.main' : 'error.main'}>
          {formatCurrency(r.valor)}
        </Typography>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => (
        <Chip
          label={STATUS_CONCILIACAO_LABELS[r.status] ?? r.status}
          size="small"
          color={statusColor(r.status)}
        />
      ),
    },
    { key: 'lancamentoDescricao', header: 'Lançamento Associado', render: (r) => r.lancamentoDescricao ?? '—' },
    {
      key: 'actions',
      header: '',
      width: 80,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          {r.status !== 'CONCILIADO' ? (
            <Tooltip title="Associar sugestão">
              <IconButton
                size="small"
                color="primary"
                onClick={() => associar.mutate({ itemId: r.id, lancamentoId: r.sugestaoLancamentoId! })}
                disabled={!r.sugestaoLancamentoId}
              >
                <AutoFixHighIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <Tooltip title="Desassociar">
              <IconButton size="small" color="warning" onClick={() => desassociar.mutate(r.id)}>
                <LinkOffIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      ),
    },
  ]

  return (
    <Box>
      <PageHeader
        title="Conciliação Bancária"
        subtitle="Importe extratos e reconcilie com os lançamentos do sistema."
        actions={
          <Stack direction="row" spacing={1} alignItems="center">
            <TextField
              select
              label="Conta"
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
            <input
              ref={fileInputRef}
              type="file"
              accept=".ofx,.csv"
              hidden
              onChange={handleFileChange}
            />
            <Button
              variant="contained"
              startIcon={<UploadFileIcon />}
              disabled={!contaId || importar.isPending}
              onClick={() => fileInputRef.current?.click()}
            >
              Importar Extrato
            </Button>
          </Stack>
        }
      />

      {importSuccess && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setImportSuccess(false)}>
          Extrato importado com sucesso.
        </Alert>
      )}
      {importError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setImportError(null)}>
          {importError}
        </Alert>
      )}

      {!contaId ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <LinkIcon sx={{ fontSize: 48, opacity: 0.3, mb: 1 }} />
          <Typography color="text.secondary">Selecione uma conta bancária para iniciar a conciliação.</Typography>
        </Paper>
      ) : (
        <DataTable
          columns={columns}
          rows={itens}
          keyField="id"
          loading={isLoading}
          emptyMessage="Nenhum item importado. Faça o upload de um extrato."
        />
      )}
    </Box>
  )
}
