import { Box, Button, IconButton, MenuItem, TextField, Tooltip } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, StatusChip, NoCondominioGuard, type Column } from '@/components/common'
import { useGruposDespesa, useGruposDespesaMutations } from '@/hooks/useRateio'
import type { GrupoDespesaDTO, CreateGrupoDespesaDTO } from '@/types'
import { TIPO_RATEIO_LABELS, ESCOPO_RATEIO_LABELS } from '@/utils/constants'

const schema = z.object({
  nome: z.string().min(2),
  tipoRateio: z.enum(['IGUALITARIO', 'FRACAO_IDEAL', 'METRAGEM', 'CONSUMO'] as const),
  escopo: z.enum(['TODOS', 'BLOCO'] as const),
})
type FormValues = z.infer<typeof schema>

export default function GruposDespesaPage() {
  const { data: grupos = [], isLoading } = useGruposDespesa()
  const { create, update, remove } = useGruposDespesaMutations()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<GrupoDespesaDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<GrupoDespesaDTO | null>(null)

  const { register, handleSubmit, reset, control, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { tipoRateio: 'IGUALITARIO', escopo: 'TODOS' },
  })

  const openCreate = () => { setEditTarget(null); reset({ tipoRateio: 'IGUALITARIO', escopo: 'TODOS' }); setDialogOpen(true) }
  const openEdit = (g: GrupoDespesaDTO) => {
    setEditTarget(g)
    reset({ nome: g.nome, tipoRateio: g.tipoRateio, escopo: g.escopo })
    setDialogOpen(true)
  }

  const onSubmit = (values: FormValues) => {
    if (editTarget) {
      update.mutate({ id: editTarget.id, body: values }, { onSuccess: () => setDialogOpen(false) })
    } else {
      create.mutate(values as CreateGrupoDespesaDTO, { onSuccess: () => setDialogOpen(false) })
    }
  }

  const columns: Column<GrupoDespesaDTO>[] = [
    { key: 'nome', header: 'Nome', render: (r) => r.nome },
    {
      key: 'tipoRateio',
      header: 'Tipo de Rateio',
      render: (r) => <StatusChip status={r.tipoRateio} label={TIPO_RATEIO_LABELS[r.tipoRateio]} />,
    },
    {
      key: 'escopo',
      header: 'Escopo',
      render: (r) => ESCOPO_RATEIO_LABELS[r.escopo],
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

  return (
    <Box>
      <NoCondominioGuard />
      <PageHeader
        title="Grupos de Despesa"
        subtitle="Defina grupos e estratégias de rateio para as despesas condominiais."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Novo Grupo
          </Button>
        }
      />

      <DataTable
        columns={columns}
        rows={grupos}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhum grupo cadastrado."
      />

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Grupo' : 'Novo Grupo de Despesa'}
        formId="grupo-form"
        loading={create.isPending || update.isPending}
        onClose={() => setDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="grupo-form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="Nome"
            fullWidth
            {...register('nome')}
            error={!!errors.nome}
            helperText={errors.nome?.message}
          />
          <Controller
            name="tipoRateio"
            control={control}
            render={({ field }) => (
              <TextField select label="Tipo de Rateio" fullWidth {...field}>
                {Object.entries(TIPO_RATEIO_LABELS).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </TextField>
            )}
          />
          <Controller
            name="escopo"
            control={control}
            render={({ field }) => (
              <TextField select label="Escopo" fullWidth {...field}>
                {Object.entries(ESCOPO_RATEIO_LABELS).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </TextField>
            )}
          />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Grupo"
        message={`Excluir o grupo "${deleteTarget?.nome}"? Os coeficientes associados serão removidos.`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id, { onSuccess: () => setDeleteTarget(null) })}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
