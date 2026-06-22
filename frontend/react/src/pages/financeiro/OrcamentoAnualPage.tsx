import {
  Box,
  Button,
  Chip,
  IconButton,
  TextField,
  Tooltip,
  Typography,
  Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import CalculateIcon from '@mui/icons-material/Calculate'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, NoCondominioGuard, type Column } from '@/components/common'
import { orcamentoApi } from '@/api/financeiro/orcamento.api'
import { formatCurrency } from '@/utils/formatters'
import type { OrcamentoItemDTO, AddOrcamentoItemDTO } from '@/types'

const CURRENT_YEAR = new Date().getFullYear()

export default function OrcamentoAnualPage() {
  const qc = useQueryClient()
  const [ano, setAno] = useState(CURRENT_YEAR)
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<OrcamentoItemDTO | null>(null)

  const { data: items = [], isLoading } = useQuery({
    queryKey: ['orcamento', ano],
    queryFn: () => orcamentoApi.listByAno(ano),
  })

  const { register, handleSubmit, reset } = useForm<AddOrcamentoItemDTO>()

  const addItem = useMutation({
    mutationFn: (body: AddOrcamentoItemDTO) => orcamentoApi.addItem(ano, body),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['orcamento', ano] }); setAddDialogOpen(false) },
  })

  const removeItem = useMutation({
    mutationFn: (itemId: number) => orcamentoApi.removeItem(ano, itemId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['orcamento', ano] }); setDeleteTarget(null) },
  })

  const recalcular = useMutation({
    mutationFn: () => orcamentoApi.recalcularRateio(ano),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['orcamento', ano] }),
  })

  const totalOrcado = items.reduce((sum, i) => sum + i.valorOrcado, 0)

  const columns: Column<OrcamentoItemDTO>[] = [
    { key: 'contaNome', header: 'Conta', render: (r) => r.contaNome },
    { key: 'grupoDespesaNome', header: 'Grupo', render: (r) => r.grupoDespesaNome ?? '—' },
    { key: 'valorOrcado', header: 'Valor Orçado', align: 'right', render: (r) => formatCurrency(r.valorOrcado) },
    {
      key: 'statusRateio',
      header: 'Rateio',
      render: (r) =>
        r.statusRateio ? (
          <Chip
            label={r.statusRateio}
            size="small"
            color={r.statusRateio === 'RATEADO' ? 'success' : 'warning'}
          />
        ) : (
          <Chip label="Pendente" size="small" variant="outlined" />
        ),
    },
    {
      key: 'actions',
      header: '',
      width: 60,
      align: 'right',
      render: (r) => (
        <Tooltip title="Remover">
          <IconButton size="small" color="error" onClick={() => setDeleteTarget(r)}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      ),
    },
  ]

  return (
    <Box>
      <NoCondominioGuard />
      <PageHeader
        title="Orçamento Anual"
        subtitle="Gerencie as previsões de despesa por ano."
        actions={
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <TextField
              label="Ano"
              type="number"
              size="small"
              value={ano}
              onChange={(e) => setAno(Number(e.target.value))}
              sx={{ width: 100 }}
            />
            <Button
              variant="outlined"
              startIcon={<CalculateIcon />}
              onClick={() => recalcular.mutate()}
              disabled={recalcular.isPending}
            >
              Recalcular Rateio
            </Button>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => { reset(); setAddDialogOpen(true) }}>
              Adicionar Item
            </Button>
          </Box>
        }
      />

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Total orçado para {ano}:
        </Typography>
        <Typography variant="h5" fontWeight={700} color="primary">
          {formatCurrency(totalOrcado)}
        </Typography>
      </Paper>

      <DataTable
        columns={columns}
        rows={items}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhum item orçado para este ano."
      />

      <FormDialog
        open={addDialogOpen}
        title="Adicionar Item ao Orçamento"
        formId="orcamento-item-form"
        loading={addItem.isPending}
        onClose={() => setAddDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="orcamento-item-form"
          onSubmit={handleSubmit((v) => addItem.mutate(v))}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="ID da Conta"
            type="number"
            fullWidth
            {...register('contaId', { valueAsNumber: true, required: true })}
          />
          <TextField
            label="ID do Grupo de Despesa"
            type="number"
            fullWidth
            {...register('grupoDespesaId', { valueAsNumber: true })}
          />
          <TextField
            label="Valor Orçado (R$)"
            type="number"
            fullWidth
            inputProps={{ step: '0.01' }}
            {...register('valorOrcado', { valueAsNumber: true, required: true })}
          />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Remover Item"
        message={`Remover "${deleteTarget?.contaNome}" do orçamento?`}
        destructive
        loading={removeItem.isPending}
        onConfirm={() => removeItem.mutate(deleteTarget!.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
