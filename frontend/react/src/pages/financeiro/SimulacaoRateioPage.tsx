import {
  Box,
  Button,
  Chip,
  MenuItem,
  TextField,
  Typography,
  Paper,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material'
import CalculateIcon from '@mui/icons-material/Calculate'
import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { PageHeader } from '@/components/common'
import { rateioApi } from '@/api/financeiro/rateio.api'
import { useGruposDespesa } from '@/hooks/useRateio'
import { formatCurrency } from '@/utils/formatters'
import type { SimulacaoRateioDTO } from '@/types'

const CURRENT_YEAR = new Date().getFullYear()

export default function SimulacaoRateioPage() {
  const { data: grupos = [] } = useGruposDespesa()
  const [grupoId, setGrupoId] = useState<number | ''>('')
  const [ano, setAno] = useState(CURRENT_YEAR)
  const [resultado, setResultado] = useState<SimulacaoRateioDTO | null>(null)

  const simular = useMutation({
    mutationFn: () => rateioApi.simular({ grupoId: grupoId as number, ano }),
    onSuccess: (data) => setResultado(data),
  })

  const totalRateado = resultado?.linhas.reduce((sum, l) => sum + l.valorRateado, 0) ?? 0

  return (
    <Box>
      <PageHeader
        title="Simulação de Rateio"
        subtitle="Visualize como as despesas serão distribuídas antes de efetivar."
      />

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle2" sx={{ mb: 2 }}>Parâmetros da Simulação</Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <TextField
            select
            label="Grupo de Despesa"
            size="small"
            value={grupoId}
            onChange={(e) => { setGrupoId(Number(e.target.value)); setResultado(null) }}
            sx={{ minWidth: 220 }}
          >
            {grupos.map((g) => (
              <MenuItem key={g.id} value={g.id}>{g.nome}</MenuItem>
            ))}
          </TextField>
          <TextField
            label="Ano"
            type="number"
            size="small"
            value={ano}
            onChange={(e) => { setAno(Number(e.target.value)); setResultado(null) }}
            sx={{ width: 100 }}
          />
          <Button
            variant="contained"
            startIcon={<CalculateIcon />}
            disabled={!grupoId || simular.isPending}
            onClick={() => simular.mutate()}
          >
            Simular
          </Button>
        </Box>
      </Paper>

      {simular.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Erro ao simular rateio. Verifique os parâmetros e tente novamente.
        </Alert>
      )}

      {resultado && (
        <Box>
          <Paper sx={{ p: 2, mb: 2 }}>
            <Box sx={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
              <Box>
                <Typography variant="caption" color="text.secondary">Total a Ratear</Typography>
                <Typography variant="h5" fontWeight={700} color="primary">
                  {formatCurrency(resultado.totalDespesas)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Total Rateado</Typography>
                <Typography variant="h5" fontWeight={700} color="success.main">
                  {formatCurrency(totalRateado)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Unidades</Typography>
                <Typography variant="h5" fontWeight={700}>
                  {resultado.linhas.length}
                </Typography>
              </Box>
            </Box>
          </Paper>

          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Bloco</TableCell>
                  <TableCell>Apartamento</TableCell>
                  <TableCell align="right">Coeficiente</TableCell>
                  <TableCell align="right">Valor Rateado</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {resultado.linhas.map((linha) => (
                  <TableRow key={linha.apartamentoId} hover>
                    <TableCell>{linha.blocoNome ?? '—'}</TableCell>
                    <TableCell>{linha.apartamentoNumero}</TableCell>
                    <TableCell align="right">{linha.coeficiente.toFixed(4)}</TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" fontWeight={600}>
                        {formatCurrency(linha.valorRateado)}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}

      {!resultado && !simular.isPending && (
        <Paper sx={{ p: 6, textAlign: 'center' }}>
          <CalculateIcon sx={{ fontSize: 48, opacity: 0.3, mb: 1 }} />
          <Typography color="text.secondary">
            Configure os parâmetros acima e clique em Simular para visualizar a distribuição.
          </Typography>
        </Paper>
      )}
    </Box>
  )
}
