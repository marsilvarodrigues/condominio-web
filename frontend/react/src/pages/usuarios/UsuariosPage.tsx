import {
  Box,
  Button,
  Checkbox,
  Chip,
  FormControl,
  IconButton,
  InputLabel,
  ListItemText,
  MenuItem,
  Select,
  TextField,
  Tooltip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import ToggleOnIcon from '@mui/icons-material/ToggleOn'
import ToggleOffIcon from '@mui/icons-material/ToggleOff'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, type Column } from '@/components/common'
import { usuariosApi } from '@/api/usuarios.api'
import { useCondominios } from '@/hooks/useCondominios'
import { useNotificationStore } from '@/store/notificationStore'
import type { UserDTO, CreateUserDTO } from '@/types'

// ── Roles disponíveis no sistema ──────────────────────────────────────────────

const ROLES: { value: string; label: string; color: 'primary' | 'secondary' | 'warning' | 'info' | 'default' }[] = [
  { value: 'ROLE_ADMIN',        label: 'Administrador', color: 'primary' },
  { value: 'ROLE_SINDICO',      label: 'Síndico',       color: 'secondary' },
  { value: 'ROLE_PROPRIETARIO', label: 'Proprietário',  color: 'warning' },
  { value: 'ROLE_MORADOR',      label: 'Morador',       color: 'info' },
  { value: 'ROLE_USER',         label: 'Usuário',        color: 'default' },
]

function roleLabel(role: string): string {
  return ROLES.find((r) => r.value === role)?.label ?? role
}

function roleColor(role: string) {
  return ROLES.find((r) => r.value === role)?.color ?? 'default'
}

// ── Componente ────────────────────────────────────────────────────────────────

export default function UsuariosPage() {
  const qc = useQueryClient()
  const { notifySuccess } = useNotificationStore()
  const invalidate = () => void qc.invalidateQueries({ queryKey: ['users'] })

  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)

  const { data, isLoading } = useQuery({
    queryKey: ['users', page, pageSize],
    queryFn: () => usuariosApi.list({ page, size: pageSize }),
  })
  const users = data?.content ?? []

  const { data: condominios = [] } = useCondominios()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<UserDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<UserDTO | null>(null)

  const { register, handleSubmit, reset, control } = useForm<CreateUserDTO>()

  const create = useMutation({
    mutationFn: usuariosApi.create,
    onSuccess: () => { invalidate(); setDialogOpen(false); notifySuccess('Usuário criado com sucesso') },
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<CreateUserDTO> }) =>
      usuariosApi.update(id, body),
    onSuccess: () => { invalidate(); setDialogOpen(false); notifySuccess('Usuário atualizado') },
  })

  const remove = useMutation({
    mutationFn: usuariosApi.remove,
    onSuccess: () => { invalidate(); setDeleteTarget(null); notifySuccess('Usuário excluído') },
  })

  const toggleEnable = useMutation({
    mutationFn: (u: UserDTO) => (u.enabled ? usuariosApi.disable(u.id) : usuariosApi.enable(u.id)),
    onSuccess: (_, u) => { invalidate(); notifySuccess(u.enabled ? 'Usuário desativado' : 'Usuário ativado') },
  })

  const openCreate = () => {
    setEditTarget(null)
    reset({ roles: ['ROLE_USER'], condominioIds: [] })
    setDialogOpen(true)
  }

  const openEdit = (u: UserDTO) => {
    setEditTarget(u)
    reset({ email: u.email, name: u.name, roles: u.roles, condominioIds: u.condominioIds })
    setDialogOpen(true)
  }

  const onSubmit = (values: CreateUserDTO) => {
    if (editTarget) update.mutate({ id: editTarget.id, body: values })
    else create.mutate(values)
  }

  const columns: Column<UserDTO>[] = [
    { key: 'name',  header: 'Nome',   render: (r) => r.name },
    { key: 'email', header: 'E-mail', render: (r) => r.email },
    {
      key: 'roles',
      header: 'Perfil',
      render: (r) =>
        r.roles.map((role) => (
          <Chip
            key={role}
            label={roleLabel(role)}
            size="small"
            color={roleColor(role)}
            sx={{ mr: 0.5, mb: 0.25 }}
          />
        )),
    },
    {
      key: 'condominios',
      header: 'Condomínios',
      render: (r) =>
        r.condominioIds.length === 0
          ? <Chip label="Global" size="small" variant="outlined" />
          : <Chip label={r.condominioIds.length} size="small" variant="outlined" />,
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => (
        <Chip
          label={r.enabled ? 'Ativo' : 'Inativo'}
          size="small"
          color={r.enabled ? 'success' : 'default'}
        />
      ),
    },
    {
      key: 'actions',
      header: '',
      width: 110,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Tooltip title={r.enabled ? 'Desativar' : 'Ativar'}>
            <IconButton
              size="small"
              color={r.enabled ? 'warning' : 'success'}
              onClick={() => toggleEnable.mutate(r)}
            >
              {r.enabled ? <ToggleOnIcon fontSize="small" /> : <ToggleOffIcon fontSize="small" />}
            </IconButton>
          </Tooltip>
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

  const isSaving = create.isPending || update.isPending

  return (
    <Box>
      <PageHeader
        title="Usuários"
        subtitle={`${data?.totalElements ?? 0} usuário(s) cadastrado(s)`}
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Novo Usuário
          </Button>
        }
      />

      <DataTable
        columns={columns}
        rows={users}
        keyField="id"
        loading={isLoading}
        emptyMessage="Nenhum usuário cadastrado."
        page={page}
        pageSize={pageSize}
        totalCount={data?.totalElements}
        onPageChange={setPage}
        onPageSizeChange={(s) => { setPageSize(s); setPage(0) }}
      />

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Usuário' : 'Novo Usuário'}
        formId="user-form"
        loading={isSaving}
        onClose={() => setDialogOpen(false)}
      >
        <Box
          component="form"
          id="user-form"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="Nome"
            fullWidth
            {...register('name', { required: true })}
          />
          <TextField
            label="E-mail"
            type="email"
            fullWidth
            disabled={!!editTarget}
            {...register('email', { required: true })}
          />

          {/* Perfis */}
          <Controller
            name="roles"
            control={control}
            defaultValue={['ROLE_USER']}
            render={({ field }) => (
              <FormControl fullWidth size="small">
                <InputLabel>Perfis</InputLabel>
                <Select
                  {...field}
                  multiple
                  label="Perfis"
                  renderValue={(selected) =>
                    (selected as string[]).map(roleLabel).join(', ')
                  }
                >
                  {ROLES.map((r) => (
                    <MenuItem key={r.value} value={r.value}>
                      <Checkbox checked={(field.value ?? []).includes(r.value)} />
                      <ListItemText primary={r.label} />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
          />

          {/* Condomínios */}
          <Controller
            name="condominioIds"
            control={control}
            defaultValue={[]}
            render={({ field }) => (
              <FormControl fullWidth size="small">
                <InputLabel>Condomínios (vazio = acesso global)</InputLabel>
                <Select
                  {...field}
                  multiple
                  label="Condomínios (vazio = acesso global)"
                  renderValue={(selected) => {
                    const ids = selected as number[]
                    if (ids.length === 0) return 'Acesso global'
                    return ids
                      .map((id) => condominios.find((c) => c.id === id)?.nome ?? String(id))
                      .join(', ')
                  }}
                >
                  {condominios.map((c) => (
                    <MenuItem key={c.id} value={c.id}>
                      <Checkbox checked={(field.value ?? []).includes(c.id)} />
                      <ListItemText primary={c.nome} />
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
          />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Usuário"
        message={`Deseja excluir o usuário "${deleteTarget?.email}"? Esta ação não pode ser desfeita.`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
