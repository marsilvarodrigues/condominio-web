import { Box, Button, IconButton, TextField, Tooltip } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, NoCondominioGuard, type Column } from '@/components/common'
import { bancosApi } from '@/api/financeiro/bancos.api'
import type { BancoDTO, CreateBancoDTO } from '@/types'

const schema = z.object({
  nome: z.string().min(2, 'Mínimo 2 caracteres'),
  codigo: z.string().min(1, 'Código obrigatório'),
})
type FormValues = z.infer<typeof schema>

export default function BancosPage() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: ['bancos'] })

  const { data: bancos = [], isLoading } = useQuery({
    queryKey: ['bancos'],
    queryFn: bancosApi.list,
  })

  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<BancoDTO | null>(null)

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  })

  const create = useMutation({
    mutationFn: bancosApi.create,
    onSuccess: () => { invalidate(); setDialogOpen(false) },
  })

  const remove = useMutation({
    mutationFn: bancosApi.remove,
    onSuccess: () => { invalidate(); setDeleteTarget(null) },
  })

  const columns: Column<BancoDTO>[] = [
    { key: 'codigo', header: 'Código', render: (r) => r.codigo },
    { key: 'nome', header: 'Nome', render: (r) => r.nome },
    {
      key: 'actions',
      header: '',
      width: 60,
      align: 'right',
      render: (r) => (
        <Tooltip title="Excluir">
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
        title="Bancos"
        subtitle="Cadastre os bancos utilizados pelo condomínio."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => { reset(); setDialogOpen(true) }}>
            Novo Banco
          </Button>
        }
      />

      <DataTable
        columns={columns}
        rows={bancos}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhum banco cadastrado."
      />

      <FormDialog
        open={dialogOpen}
        title="Novo Banco"
        formId="banco-form"
        loading={create.isPending}
        onClose={() => setDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="banco-form"
          onSubmit={handleSubmit((v) => create.mutate(v as CreateBancoDTO))}
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
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Banco"
        message={`Excluir o banco "${deleteTarget?.nome}"?`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id, { onSuccess: () => setDeleteTarget(null) })}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
