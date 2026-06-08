import {
  Box,
  Button,
  Chip,
  MenuItem,
  TextField,
  Typography,
  Paper,
  Grid,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AccountBalanceIcon from '@mui/icons-material/AccountBalance'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { PageHeader, DataTable, FormDialog, type Column } from '@/components/common'
import { formatCurrency, formatDate } from '@/utils/formatters'
import type { MovimentacaoFundoDTO, CreateMovimentacaoFundoDTO } from '@/types'
import { TIPO_MOVIMENTACAO_LABELS } from '@/utils/constants'
import { apiClient } from '@/api/client'

const schema = z.object({
  tipo: z.enum(['ENTRADA', 'SAIDA', 'RENDIMENTO', 'APLICACAO', 'RESGATE'] as const),
  valor: z.number({ coerce: true }).positive('Valor deve ser positivo'),
  descricao: z.string().min(3, 'Mínimo 3 caracteres'),
  data: z.string().min(1, 'Data obrigatória'),
})
type FormValues = z.infer<typeof schema>

async function listMovimentacoes(): Promise<MovimentacaoFundoDTO[]> {
  const res = await apiClient.get('/fundo-reserva/movimentacoes')
  return res.data.data ?? res.data
}

async function getSaldo(): Promise<number> {
  const res = await apiClient.get('/fundo-reserva/saldo')
  return res.data.data ?? 0
}

async function createMovimentacao(body: CreateMovimentacaoFundoDTO): Promise<MovimentacaoFundoDTO> {
  const res = await apiClient.post('/fundo-reserva/movimentacoes', body)
  return res.data.data ?? res.data
}

export default function FundoReservaPage() {
  const qc = useQueryClient()
  const [dialogOpen, setDialogOpen] = useState(false)

  const { data: movimentacoes = [], isLoading } = useQuery({
    queryKey: ['fundo-reserva-movimentacoes'],
    queryFn: listMovimentacoes,
  })

  const { data: saldo = 0 } = useQuery({
    queryKey: ['fundo-reserva-saldo'],
    queryFn: getSaldo,
  })

  const { register, handleSubmit, reset, control, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { tipo: 'ENTRADA' },
  })

  const create = useMutation({
    mutationFn: createMovimentacao,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['fundo-reserva-movimentacoes'] })
      qc.invalidateQueries({ queryKey: ['fundo-reserva-saldo'] })
      setDialogOpen(false)
    },
  })

  const columns: Column<MovimentacaoFundoDTO>[] = [
    { key: 'data', header: 'Data', render: (r) => formatDate(r.data) },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (r) => {
        const isEntrada = ['ENTRADA', 'RENDIMENTO', 'RESGATE'].includes(r.tipo)
        return (
          <Chip
            icon={isEntrada ? <TrendingUpIcon fontSize="small" /> : <TrendingDownIcon fontSize="small" />}
            label={TIPO_MOVIMENTACAO_LABELS[r.tipo] ?? r.tipo}
            size="small"
            color={isEntrada ? 'success' : 'error'}
          />
        )
      },
    },
    { key: 'descricao', header: 'Descrição', render: (r) => r.descricao },
    {
      key: 'valor',
      header: 'Valor',
      align: 'right',
      render: (r) => (
        <Typography
          variant="body2"
          fontWeight={600}
          color={['ENTRADA', 'RENDIMENTO', 'RESGATE'].includes(r.tipo) ? 'success.main' : 'error.main'}
        >
          {formatCurrency(r.valor)}
        </Typography>
      ),
    },
  ]

  return (
    <Box>
      <PageHeader
        title="Fundo de Reserva"
        subtitle="Controle entradas, saídas e rendimentos do fundo."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => { reset(); setDialogOpen(true) }}>
            Nova Movimentação
          </Button>
        }
      />

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={4}>
          <Paper sx={{ p: 2, borderLeft: 4, borderColor: 'primary.main' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <AccountBalanceIcon color="primary" />
              <Typography variant="caption" color="text.secondary" fontWeight={600} textTransform="uppercase">
                Saldo Atual
              </Typography>
            </Box>
            <Typography variant="h4" fontWeight={700} color="primary" mt={0.5}>
              {formatCurrency(saldo)}
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      <DataTable
        columns={columns}
        rows={movimentacoes}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhuma movimentação registrada."
      />

      <FormDialog
        open={dialogOpen}
        title="Nova Movimentação"
        formId="fundo-form"
        loading={create.isPending}
        onClose={() => setDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="fundo-form"
          onSubmit={handleSubmit((v) => create.mutate(v as CreateMovimentacaoFundoDTO))}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <Controller
            name="tipo"
            control={control}
            render={({ field }) => (
              <TextField select label="Tipo" fullWidth {...field}>
                {Object.entries(TIPO_MOVIMENTACAO_LABELS).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField
            label="Data"
            type="date"
            fullWidth
            InputLabelProps={{ shrink: true }}
            {...register('data')}
            error={!!errors.data}
            helperText={errors.data?.message}
          />
          <TextField
            label="Valor (R$)"
            type="number"
            fullWidth
            inputProps={{ step: '0.01' }}
            {...register('valor')}
            error={!!errors.valor}
            helperText={errors.valor?.message}
          />
          <TextField
            label="Descrição"
            fullWidth
            multiline
            rows={2}
            {...register('descricao')}
            error={!!errors.descricao}
            helperText={errors.descricao?.message}
          />
        </Box>
      </FormDialog>
    </Box>
  )
}
