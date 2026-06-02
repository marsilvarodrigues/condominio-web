import {
  Box,
  Button,
  IconButton,
  MenuItem,
  TextField,
  Tooltip,
  Typography,
  Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useState } from 'react'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, type Column } from '@/components/common'
import {
  useGruposDespesa,
  useCoeficientesGrupo,
  useCoeficientesMutations,
} from '@/hooks/useRateio'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import type { CoeficienteRateioDTO, CreateCoeficienteRateioDTO } from '@/types'

const schema = z.object({
  apartamentoId: z.number({ coerce: true }).positive('Selecione um apartamento'),
  coeficiente: z.number({ coerce: true }).positive('Coeficiente deve ser positivo'),
})
type FormValues = z.infer<typeof schema>

export default function CoeficientesPage() {
  const { data: grupos = [] } = useGruposDespesa()
  const [grupoId, setGrupoId] = useState<number | ''>('')
  const { data: coeficientes = [], isLoading } = useCoeficientesGrupo(grupoId as number)
  const { create, update, remove } = useCoeficientesMutations(grupoId as number)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<CoeficienteRateioDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CoeficienteRateioDTO | null>(null)

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  })

  const openCreate = () => { setEditTarget(null); reset({}); setDialogOpen(true) }
  const openEdit = (c: CoeficienteRateioDTO) => {
    setEditTarget(c)
    reset({ apartamentoId: c.apartamentoId, coeficiente: c.coeficiente })
    setDialogOpen(true)
  }

  const onSubmit = (values: FormValues) => {
    if (editTarget) {
      update.mutate({ id: editTarget.id, body: values }, { onSuccess: () => setDialogOpen(false) })
    } else {
      create.mutate(values as CreateCoeficienteRateioDTO, { onSuccess: () => setDialogOpen(false) })
    }
  }

  const columns: Column<CoeficienteRateioDTO>[] = [
    { key: 'apartamentoNumero', header: 'Apartamento', render: (r) => r.apartamentoNumero },
    { key: 'blocoNome', header: 'Bloco', render: (r) => r.blocoNome ?? '—' },
    {
      key: 'coeficiente',
      header: 'Coeficiente',
      align: 'right',
      render: (r) => (
        <Typography variant="body2" fontWeight={600}>
          {r.coeficiente.toFixed(4)}
        </Typography>
      ),
    },
    {
      key: 'actions',
      header: '',
      width: 80,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Tooltip title="Editar">
            <IconButton size="small" onClick={() => openEdit(r)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Excluir">
            <IconButton size="small" color="error" onClick={() => setDeleteTarget(r)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      ),
    },
  ]

  const selectedGrupo = grupos.find((g) => g.id === grupoId)

  return (
    <Box>
      <PageHeader
        title="Coeficientes de Rateio"
        subtitle="Defina os pesos de cada unidade por grupo de despesa."
        actions={
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <TextField
              select
              label="Grupo de Despesa"
              size="small"
              value={grupoId}
              onChange={(e) => setGrupoId(Number(e.target.value))}
              sx={{ minWidth: 220 }}
            >
              {grupos.map((g) => (
                <MenuItem key={g.id} value={g.id}>{g.nome}</MenuItem>
              ))}
            </TextField>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              disabled={!grupoId}
              onClick={openCreate}
            >
              Novo Coeficiente
            </Button>
          </Box>
        }
      />

      {!grupoId ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">Selecione um grupo de despesa para gerenciar os coeficientes.</Typography>
        </Paper>
      ) : (
        <>
          {selectedGrupo && (
            <Paper sx={{ p: 2, mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Grupo: <strong>{selectedGrupo.nome}</strong> — Tipo de rateio: <strong>{selectedGrupo.tipoRateio}</strong>
              </Typography>
            </Paper>
          )}
          <DataTable
            columns={columns}
            rows={coeficientes}
            keyField="id"
            loading={isLoading}
            emptyMessage="Nenhum coeficiente definido para este grupo."
          />
        </>
      )}

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Coeficiente' : 'Novo Coeficiente'}
        formId="coeficiente-form"
        loading={create.isPending || update.isPending}
        onClose={() => setDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="coeficiente-form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="ID do Apartamento"
            type="number"
            fullWidth
            {...register('apartamentoId')}
            error={!!errors.apartamentoId}
            helperText={errors.apartamentoId?.message}
          />
          <TextField
            label="Coeficiente"
            type="number"
            fullWidth
            inputProps={{ step: '0.0001' }}
            {...register('coeficiente')}
            error={!!errors.coeficiente}
            helperText={errors.coeficiente?.message}
          />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Coeficiente"
        message={`Excluir o coeficiente do apartamento ${deleteTarget?.apartamentoNumero}?`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id, { onSuccess: () => setDeleteTarget(null) })}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
