import {
  Box,
  Button,
  Chip,
  IconButton,
  MenuItem,
  TextField,
  Tooltip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import PersonIcon from '@mui/icons-material/Person'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, type Column } from '@/components/common'
import { usePessoas, usePessoaMutations } from '@/hooks/useMoradores'
import type { PessoaDTO, CreatePessoaDTO, UpdatePessoaDTO, TipoPessoa } from '@/types'

const TIPO_LABELS: Record<TipoPessoa, string> = {
  MORADOR: 'Morador',
  PROP_PF: 'Proprietário PF',
  PROP_PJ: 'Proprietário PJ',
}

const TIPO_COLORS: Record<TipoPessoa, 'primary' | 'success' | 'warning'> = {
  MORADOR: 'primary',
  PROP_PF: 'success',
  PROP_PJ: 'warning',
}

const schema = z.object({
  nome: z.string().min(2, 'Nome obrigatório'),
  email: z.string().email('E-mail inválido'),
  cpf: z.string().min(11, 'CPF obrigatório'),
  telefone: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

export default function PessoasPage() {
  const [filterNome, setFilterNome] = useState('')
  const [filterTipo, setFilterTipo] = useState<TipoPessoa | ''>('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<PessoaDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<PessoaDTO | null>(null)

  const { data, isLoading } = usePessoas({
    nome: filterNome || undefined,
    tipo: filterTipo as TipoPessoa || undefined,
  })

  const pessoas = data?.content ?? []

  const { create, update, remove } = usePessoaMutations()

  const { register, handleSubmit, reset, formState: { errors } } =
    useForm<FormValues>({
      resolver: zodResolver(schema),
      defaultValues: { nome: '', email: '', cpf: '', telefone: '' },
    })

  const openCreate = () => {
    setEditTarget(null)
    reset({ nome: '', email: '', cpf: '', telefone: '' })
    setDialogOpen(true)
  }

  const openEdit = (p: PessoaDTO) => {
    setEditTarget(p)
    reset({
      nome: p.nome,
      email: p.email ?? '',
      cpf: p.cpf ?? '',
      telefone: p.telefone ?? '',
    })
    setDialogOpen(true)
  }

  const onSubmit = handleSubmit((values) => {
    if (editTarget) {
      const body: UpdatePessoaDTO = {
        nome: values.nome,
        email: values.email,
        telefone: values.telefone || undefined,
        cpf: values.cpf || undefined,
      }
      update.mutate({ id: editTarget.id, body }, { onSuccess: () => setDialogOpen(false) })
    } else {
      const body: CreatePessoaDTO = {
        nome: values.nome,
        email: values.email,
        cpf: values.cpf,
        telefone: values.telefone || undefined,
      }
      create.mutate(body, { onSuccess: () => setDialogOpen(false) })
    }
  })

  const columns: Column<PessoaDTO>[] = [
    {
      key: 'nome',
      header: 'Nome',
      render: (r) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <PersonIcon fontSize="small" color="action" />
          {r.nome}
        </Box>
      ),
    },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (r) => (
        <Chip
          label={TIPO_LABELS[r.tipo] ?? r.tipo}
          size="small"
          color={TIPO_COLORS[r.tipo] ?? 'default'}
          variant="outlined"
        />
      ),
    },
    {
      key: 'cpf',
      header: 'CPF',
      render: (r) => r.cpf ?? '—',
    },
    { key: 'email', header: 'E-mail', render: (r) => r.email ?? '—' },
    {
      key: 'apartamento',
      header: 'Apartamento',
      render: (r) =>
        r.apartamentoNumero ? (
          <Chip label={`Apt ${r.apartamentoNumero}`} size="small" color="info" />
        ) : (
          <Chip label="Sem apt." size="small" variant="outlined" />
        ),
    },
    {
      key: 'actions',
      header: '',
      width: 90,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
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
        title="Moradores"
        subtitle={`${pessoas.length} morador${pessoas.length !== 1 ? 'es' : ''} encontrado${pessoas.length !== 1 ? 's' : ''}`}
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Novo Morador
          </Button>
        }
      />

      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
        <TextField
          label="Nome"
          size="small"
          value={filterNome}
          onChange={(e) => setFilterNome(e.target.value)}
          sx={{ width: 280 }}
        />
        <TextField
          select
          label="Tipo"
          size="small"
          value={filterTipo}
          onChange={(e) => setFilterTipo(e.target.value as TipoPessoa | '')}
          sx={{ width: 200 }}
        >
          <MenuItem value="">Todos</MenuItem>
          <MenuItem value="MORADOR">Morador</MenuItem>
          <MenuItem value="PROP_PF">Proprietário PF</MenuItem>
          <MenuItem value="PROP_PJ">Proprietário PJ</MenuItem>
        </TextField>
      </Box>

      <DataTable
        columns={columns}
        rows={pessoas}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhum morador cadastrado."
      />

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Morador' : 'Novo Morador'}
        formId="morador-form"
        loading={create.isPending || update.isPending}
        onClose={() => setDialogOpen(false)}
      >
        <Box
          component="form"
          id="morador-form"
          onSubmit={onSubmit}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="Nome"
            fullWidth
            {...register('nome')}
            error={!!errors.nome}
            helperText={errors.nome?.message}
          />
          <TextField
            label="E-mail"
            type="email"
            fullWidth
            {...register('email')}
            error={!!errors.email}
            helperText={errors.email?.message}
          />
          <TextField
            label="Telefone"
            fullWidth
            {...register('telefone')}
          />
          <TextField
            label="CPF"
            fullWidth
            {...register('cpf')}
            error={!!errors.cpf}
            helperText={errors.cpf?.message}
            inputProps={{ maxLength: 14 }}
          />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Morador"
        message={`Deseja excluir ${deleteTarget?.nome}? Esta ação não pode ser desfeita.`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id, { onSuccess: () => setDeleteTarget(null) })}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
