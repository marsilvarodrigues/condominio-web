import { Box, Button, Chip, IconButton, MenuItem, TextField, Tooltip } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, type Column } from '@/components/common'
import { planoContasApi } from '@/api/financeiro/planoContas.api'
import type { PlanoContaDTO, CreatePlanoContaDTO } from '@/types'

import { TIPO_PLANO_CONTA_LABELS } from '@/utils/constants'

const TIPO_LABELS = TIPO_PLANO_CONTA_LABELS

const schema = z.object({
  codigo: z.string().min(1, 'Código obrigatório'),
  nome: z.string().min(2, 'Mínimo 2 caracteres'),
  tipo: z.enum(['RECEITA', 'DESPESA', 'TRANSFERENCIA'] as const),
  contaParenteId: z.number({ coerce: true }).nullable().optional(),
})
type FormValues = z.infer<typeof schema>

export default function PlanoContasPage() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: ['plano-contas'] })

  const { data: contas = [], isLoading } = useQuery({
    queryKey: ['plano-contas'],
    queryFn: planoContasApi.list,
  })

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<PlanoContaDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<PlanoContaDTO | null>(null)

  const { register, handleSubmit, reset, control, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { tipo: 'DESPESA' },
  })

  const create = useMutation({
    mutationFn: planoContasApi.create,
    onSuccess: () => { invalidate(); setDialogOpen(false) },
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<CreatePlanoContaDTO> }) =>
      planoContasApi.update(id, body),
    onSuccess: () => { invalidate(); setDialogOpen(false) },
  })

  const remove = useMutation({
    mutationFn: planoContasApi.remove,
    onSuccess: () => { invalidate(); setDeleteTarget(null) },
  })

  const openCreate = () => { setEditTarget(null); reset({ tipo: 'DESPESA' }); setDialogOpen(true) }
  const openEdit = (c: PlanoContaDTO) => {
    setEditTarget(c)
    reset({ codigo: c.codigo, nome: c.nome, tipo: c.tipo as 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA' })
    setDialogOpen(true)
  }

  const onSubmit = (values: FormValues) => {
    const body = { ...values, contaParenteId: values.contaParenteId || undefined }
    if (editTarget) update.mutate({ id: editTarget.id, body })
    else create.mutate(body as CreatePlanoContaDTO)
  }

  const columns: Column<PlanoContaDTO>[] = [
    { key: 'codigo', header: 'Código', render: (r) => r.codigo },
    { key: 'nome', header: 'Nome', render: (r) => r.nome },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (r) => (
        <Chip
          label={TIPO_LABELS[r.tipo] ?? r.tipo}
          size="small"
          color={r.tipo === 'RECEITA' ? 'success' : r.tipo === 'DESPESA' ? 'error' : 'default'}
        />
      ),
    },
    { key: 'contaParente', header: 'Conta Pai', render: (r) => r.contaParenteNome ?? '—' },
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
      <PageHeader
        title="Plano de Contas"
        subtitle="Estruture as contas contábeis do condomínio."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Nova Conta
          </Button>
        }
      />

      <DataTable
        columns={columns}
        rows={contas}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhuma conta cadastrada."
      />

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Conta' : 'Nova Conta'}
        formId="plano-conta-form"
        loading={create.isPending || update.isPending}
        onClose={() => setDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="plano-conta-form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="Código"
            fullWidth
            {...register('codigo')}
            error={!!errors.codigo}
            helperText={errors.codigo?.message}
          />
          <TextField
            label="Nome"
            fullWidth
            {...register('nome')}
            error={!!errors.nome}
            helperText={errors.nome?.message}
          />
          <Controller
            name="tipo"
            control={control}
            render={({ field }) => (
              <TextField select label="Tipo" fullWidth {...field}>
                {Object.entries(TIPO_LABELS).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField
            label="ID Conta Pai (opcional)"
            type="number"
            fullWidth
            {...register('contaParenteId')}
          />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Conta"
        message={`Excluir a conta "${deleteTarget?.nome}"?`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id, { onSuccess: () => setDeleteTarget(null) })}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
