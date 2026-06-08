import {
  Box,
  Button,
  IconButton,
  TextField,
  Tooltip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import { useState } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  PageHeader,
  DataTable,
  ConfirmDialog,
  FormDialog,
  EstadoAutocomplete,
  type Column,
} from '@/components/common'
import { useCondominios, useCondominioMutations } from '@/hooks/useCondominios'
import type { CondominioDTO, CreateCondominioDTO, EstadoDTO } from '@/types'
import { formatCnpj } from '@/utils/formatters'

const estadoSchema = z.object({ id: z.number(), nome: z.string(), uf: z.string() })

const schema = z.object({
  nome: z.string().min(3, 'Mínimo 3 caracteres'),
  cnpj: z.string().min(14, 'CNPJ inválido'),
  email: z.string().email('E-mail inválido'),
  endereco: z.object({
    logradouro: z.string().min(3),
    cep: z.string().min(8),
    cidade: z.string().min(2),
    estado: estadoSchema.nullable().refine((v) => v !== null, 'Selecione um estado'),
  }),
})

type FormValues = z.infer<typeof schema>

export default function CondominiosPage() {
  const { data: condominios = [], isLoading } = useCondominios()
  const { create, update, remove } = useCondominioMutations()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<CondominioDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CondominioDTO | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const openCreate = () => {
    setEditTarget(null)
    reset({ endereco: { estado: undefined } })
    setDialogOpen(true)
  }

  const openEdit = (row: CondominioDTO) => {
    setEditTarget(row)
    const estadoObj: EstadoDTO | null =
      row.endereco.estadoId
        ? { id: row.endereco.estadoId, nome: row.endereco.estadoNome ?? '', uf: row.endereco.estadoUf ?? '' }
        : null
    reset({
      nome: row.nome,
      cnpj: row.cnpj,
      email: row.email,
      endereco: {
        logradouro: row.endereco.logradouro,
        cep: row.endereco.cep,
        cidade: row.endereco.cidade,
        estado: estadoObj ?? undefined,
      },
    })
    setDialogOpen(true)
  }

  const onSubmit = (values: FormValues) => {
    const body: CreateCondominioDTO = {
      nome: values.nome,
      cnpj: values.cnpj,
      email: values.email,
      endereco: {
        logradouro: values.endereco.logradouro,
        cep: values.endereco.cep,
        cidade: values.endereco.cidade,
        estadoId: values.endereco.estado!.id,
      },
    }
    if (editTarget) {
      update.mutate({ id: editTarget.id, body }, { onSuccess: () => setDialogOpen(false) })
    } else {
      create.mutate(body, { onSuccess: () => setDialogOpen(false) })
    }
  }

  const columns: Column<CondominioDTO>[] = [
    { key: 'nome', header: 'Nome', render: (r) => r.nome },
    { key: 'cnpj', header: 'CNPJ', render: (r) => formatCnpj(r.cnpj) },
    { key: 'email', header: 'E-mail', render: (r) => r.email },
    {
      key: 'cidade',
      header: 'Cidade / UF',
      render: (r) => `${r.endereco.cidade} / ${r.endereco.estadoUf ?? ''}`,
    },
    {
      key: 'actions',
      header: '',
      width: 80,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Tooltip title="Editar">
            <IconButton size="small" onClick={(e) => { e.stopPropagation(); openEdit(r) }}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Excluir">
            <IconButton size="small" color="error" onClick={(e) => { e.stopPropagation(); setDeleteTarget(r) }}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      ),
    },
  ]

  const isSaving = create.isPending || update.isPending

  return (
    <Box>
      <PageHeader
        title="Condomínios"
        subtitle={`${condominios.length} condomínio(s) cadastrado(s)`}
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Novo Condomínio
          </Button>
        }
      />

      <DataTable
        columns={columns}
        rows={condominios}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhum condomínio cadastrado."
      />

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Condomínio' : 'Novo Condomínio'}
        formId="condominio-form"
        loading={isSaving}
        onClose={() => setDialogOpen(false)}
      >
        <Box
          component="form"
          id="condominio-form"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField label="Nome" fullWidth {...register('nome')} error={!!errors.nome} helperText={errors.nome?.message} />
          <TextField label="CNPJ" fullWidth {...register('cnpj')} error={!!errors.cnpj} helperText={errors.cnpj?.message} />
          <TextField label="E-mail" type="email" fullWidth {...register('email')} error={!!errors.email} helperText={errors.email?.message} />
          <TextField label="Logradouro" fullWidth {...register('endereco.logradouro')} error={!!errors.endereco?.logradouro} helperText={errors.endereco?.logradouro?.message} />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="CEP" {...register('endereco.cep')} error={!!errors.endereco?.cep} helperText={errors.endereco?.cep?.message} sx={{ flex: 1 }} />
            <TextField label="Cidade" {...register('endereco.cidade')} error={!!errors.endereco?.cidade} helperText={errors.endereco?.cidade?.message} sx={{ flex: 2 }} />
            <Controller
              name="endereco.estado"
              control={control}
              render={({ field, fieldState }) => (
                <EstadoAutocomplete
                  value={field.value as EstadoDTO | null}
                  onChange={field.onChange}
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message}
                  sx={{ flex: 1 }}
                />
              )}
            />
          </Box>
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Condomínio"
        message={`Deseja excluir o condomínio "${deleteTarget?.nome}"? Esta ação não pode ser desfeita.`}
        destructive
        loading={remove.isPending}
        onConfirm={() =>
          remove.mutate(deleteTarget!.id, { onSuccess: () => setDeleteTarget(null) })
        }
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
