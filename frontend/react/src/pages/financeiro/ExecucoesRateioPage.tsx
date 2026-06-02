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
  Collapse,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { PageHeader, DataTable, StatusChip, type Column } from '@/components/common'
import { rateioApi } from '@/api/financeiro/rateio.api'
import { formatCurrency, formatDateTime } from '@/utils/formatters'
import type { RateioExecucaoDTO, RateioLancamentoDTO } from '@/types'
import { STATUS_EXECUCAO_LABELS, TIPO_EXECUCAO_LABELS } from '@/utils/constants'

function ExecucaoDetail({ execucao }: { execucao: RateioExecucaoDTO }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <TableRow hover>
        <TableCell>
          <IconButton size="small" onClick={() => setOpen((v) => !v)}>
            {open ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
          </IconButton>
        </TableCell>
        <TableCell>{execucao.id}</TableCell>
        <TableCell>{formatDateTime(execucao.dataExecucao)}</TableCell>
        <TableCell>
          <Chip
            label={TIPO_EXECUCAO_LABELS[execucao.tipo] ?? execucao.tipo}
            size="small"
            variant="outlined"
          />
        </TableCell>
        <TableCell>
          <StatusChip status={execucao.status} label={STATUS_EXECUCAO_LABELS[execucao.status] ?? execucao.status} />
        </TableCell>
        <TableCell align="right">
          <Typography variant="body2" fontWeight={600}>{formatCurrency(execucao.totalRateado)}</Typography>
        </TableCell>
        <TableCell>{execucao.descricao ?? '—'}</TableCell>
      </TableRow>
      <TableRow>
        <TableCell colSpan={7} sx={{ p: 0 }}>
          <Collapse in={open} unmountOnExit>
            <Box sx={{ p: 2, bgcolor: 'grey.50' }}>
              <Typography variant="caption" fontWeight={600} sx={{ mb: 1, display: 'block' }}>
                Lançamentos desta execução
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Apartamento</TableCell>
                    <TableCell>Bloco</TableCell>
                    <TableCell align="right">Coeficiente</TableCell>
                    <TableCell align="right">Valor</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {execucao.lancamentos?.map((l: RateioLancamentoDTO) => (
                    <TableRow key={l.id}>
                      <TableCell>{l.apartamentoNumero}</TableCell>
                      <TableCell>{l.blocoNome ?? '—'}</TableCell>
                      <TableCell align="right">{l.coeficiente.toFixed(4)}</TableCell>
                      <TableCell align="right">{formatCurrency(l.valor)}</TableCell>
                      <TableCell>
                        <StatusChip status={l.status} label={l.status} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  )
}

export default function ExecucoesRateioPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [size] = useState(20)

  const { data, isLoading } = useQuery({
    queryKey: ['rateio-execucoes', page, size],
    queryFn: () => rateioApi.listarExecucoes({ page, size }),
  })

  const execucoes = data?.content ?? []
  const total = data?.totalElements ?? 0

  return (
    <Box>
      <PageHeader
        title="Execuções de Rateio"
        subtitle="Histórico de todos os rateios realizados."
        actions={
          <Tooltip title="Atualizar">
            <IconButton onClick={() => qc.invalidateQueries({ queryKey: ['rateio-execucoes'] })}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        }
      />

      {isLoading ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">Carregando execuções...</Typography>
        </Paper>
      ) : execucoes.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">Nenhuma execução registrada.</Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell width={40} />
                <TableCell>ID</TableCell>
                <TableCell>Data</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Total Rateado</TableCell>
                <TableCell>Descrição</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {execucoes.map((e) => (
                <ExecucaoDetail key={e.id} execucao={e} />
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {total > size && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2, gap: 1 }}>
          <Button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Anterior
          </Button>
          <Typography variant="body2" sx={{ alignSelf: 'center' }}>
            Página {page + 1} de {Math.ceil(total / size)}
          </Typography>
          <Button disabled={(page + 1) * size >= total} onClick={() => setPage((p) => p + 1)}>
            Próxima
          </Button>
        </Box>
      )}
    </Box>
  )
}
