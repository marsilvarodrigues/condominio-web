import { Box, Button, Chip, IconButton, MenuItem, Select, TextField, Tooltip } from '@mui/material'
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
import type { UserDTO, CreateUserDTO } from '@/types'

export default function UsuariosPage() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: ['users'] })

  const { data: users = [], isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: usuariosApi.list,
  })

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<UserDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<UserDTO | null>(null)

  const { register, handleSubmit, reset, control } = useForm<CreateUserDTO>()

  const create = useMutation({
    mutationFn: usuariosApi.create,
    onSuccess: () => { invalidate(); setDialogOpen(false) },
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<CreateUserDTO> }) =>
      usuariosApi.update(id, body),
    onSuccess: () => { invalidate(); setDialogOpen(false) },
  })

  const remove = useMutation({
    mutationFn: usuariosApi.remove,
    onSuccess: () => { invalidate(); setDeleteTarget(null) },
  })

  const toggleEnable = useMutation({
    mutationFn: (u: UserDTO) => (u.enabled ? usuariosApi.disable(u.id) : usuariosApi.enable(u.id)),
    onSuccess: invalidate,
  })

  const openCreate = () => { setEditTarget(null); reset({ roles: ['ROLE_USER'] }); setDialogOpen(true) }
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
    { key: 'name', header: 'Nome', render: (r) => r.name },
    { key: 'email', header: 'E-mail', render: (r) => r.email },
    {
      key: 'roles',
      header: 'Perfil',
      render: (r) =>
        r.roles.map((role) => (
          <Chip
            key={role}
            label={role === 'ROLE_ADMIN' ? 'Admin' : 'Usuário'}
            size="small"
            color={role === 'ROLE_ADMIN' ? 'primary' : 'default'}
            sx={{ mr: 0.5 }}
          />
        )),
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
      width: 100,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Tooltip title={r.enabled ? 'Desativar' : 'Ativar'}>
            <IconButton size="small" color={r.enabled ? 'warning' : 'success'} onClick={() => toggleEnable.mutate(r)}>
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

  return (
    <Box>
      <PageHeader
        title="Usuários"
        subtitle={`${users.length} usuário(s) cadastrado(s)`}
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Novo Usuário
          </Button>
        }
      />

      <DataTable columns={columns} rows={users} keyField="id" loading={isLoading} emptyMessage="Nenhum usuário cadastrado." />

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Usuário' : 'Novo Usuário'}
        formId="user-form"
        loading={create.isPending || update.isPending}
        onClose={() => setDialogOpen(false)}
      >
        <Box
          component="form"
          id="user-form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField label="Nome" fullWidth {...register('name', { required: true })} />
          <TextField label="E-mail" type="email" fullWidth {...register('email', { required: true })} />
          <Controller
            name="roles"
            control={control}
            defaultValue={['ROLE_USER']}
            render={({ field }) => (
              <Select {...field} multiple label="Perfis" size="small" fullWidth>
                <MenuItem value="ROLE_USER">Usuário</MenuItem>
                <MenuItem value="ROLE_ADMIN">Administrador</MenuItem>
              </Select>
            )}
          />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Usuário"
        message={`Deseja excluir o usuário "${deleteTarget?.email}"?`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
