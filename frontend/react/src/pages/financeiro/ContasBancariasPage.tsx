import {
  Box,
  Button,
  Chip,
  IconButton,
  MenuItem,
  TextField,
  Tooltip,
  Typography,
  Paper,
  Grid,
  Divider,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import ListAltIcon from '@mui/icons-material/ListAlt'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, type Column } from '@/components/common'
import { contasBancariasApi, bancosApi } from '@/api/financeiro/bancos.api'
import { formatCurrency, formatDate } from '@/utils/formatters'
import { TIPO_CONTA_LABELS } from '@/utils/constants'
import type { ContaBancariaDTO, CreateContaBancariaDTO, LancamentoBancarioDTO } from '@/types'

const schema = z.object({
  bancoId: z.number({ coerce: true }).positive('Selecione um banco'),
  agencia: z.string().min(1, 'Agência obrigatória'),
  conta: z.string().min(1, 'Conta obrigatória'),
  tipo: z.enum(['CORRENTE', 'POUPANCA', 'INVESTIMENTO'] as const),
  descricao: z.string().optional(),
})
type FormValues = z.infer<typeof schema>

export default function ContasBancariasPage() {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: ['contas-bancarias'] })

  const { data: contas = [], isLoading } = useQuery({
    queryKey: ['contas-bancarias'],
    queryFn: () => contasBancariasApi.list(),
  })

  const { data: bancos = [] } = useQuery({
    queryKey: ['bancos'],
    queryFn: bancosApi.list,
  })

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<ContaBancariaDTO | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ContaBancariaDTO | null>(null)
  const [extrato, setExtrato] = useState<{ conta: ContaBancariaDTO; lancamentos: LancamentoBancarioDTO[] } | null>(null)

  const { register, handleSubmit, reset, control, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { tipo: 'CORRENTE' },
  })

  const create = useMutation({
    mutationFn: contasBancariasApi.create,
    onSuccess: () => { invalidate(); setDialogOpen(false) },
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<CreateContaBancariaDTO> }) =>
      contasBancariasApi.update(id, body),
    onSuccess: () => { invalidate(); setDialogOpen(false) },
  })

  const remove = useMutation({
    mutationFn: contasBancariasApi.remove,
    onSuccess: () => { invalidate(); setDeleteTarget(null) },
  })

  const openCreate = () => { setEditTarget(null); reset({ tipo: 'CORRENTE' }); setDialogOpen(true) }
  const openEdit = (c: ContaBancariaDTO) => {
    setEditTarget(c)
    reset({ bancoId: c.bancoId, agencia: c.agencia, conta: c.conta, tipo: c.tipo as 'CORRENTE' | 'POUPANCA' | 'INVESTIMENTO' })
    setDialogOpen(true)
  }

  const openExtrato = async (c: ContaBancariaDTO) => {
    const lancamentos = await contasBancariasApi.lancamentos(c.id)
    setExtrato({ conta: c, lancamentos })
  }

  const onSubmit = (values: FormValues) => {
    if (editTarget) update.mutate({ id: editTarget.id, body: values })
    else create.mutate(values as CreateContaBancariaDTO)
  }

  const lancamentoColumns: Column<LancamentoBancarioDTO>[] = [
    { key: 'dataLancamento', header: 'Data', render: (r) => formatDate(r.dataLancamento) },
    { key: 'descricao', header: 'Descrição', render: (r) => r.descricao },
    { key: 'valor', header: 'Valor', align: 'right', render: (r) => (
      <Typography variant="body2" fontWeight={600} color={r.tipo === 'CREDITO' ? 'success.main' : 'error.main'}>
        {formatCurrency(r.valor)}
      </Typography>
    )},
    { key: 'status', header: 'Status', render: (r) => (
      <Chip
        label={r.status === 'CONCILIADO' ? 'Conciliado' : 'Pendente'}
        size="small"
        color={r.status === 'CONCILIADO' ? 'success' : 'warning'}
      />
    )},
  ]

  const columns: Column<ContaBancariaDTO>[] = [
    { key: 'bancoNome', header: 'Banco', render: (r) => r.bancoNome },
    { key: 'agencia', header: 'Agência', render: (r) => r.agencia },
    { key: 'conta', header: 'Conta', render: (r) => r.conta },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (r) => <Chip label={TIPO_CONTA_LABELS[r.tipo] ?? r.tipo} size="small" />,
    },
    {
      key: 'saldo',
      header: 'Saldo',
      align: 'right',
      render: (r) => (
        <Typography variant="body2" fontWeight={600} color={r.saldo >= 0 ? 'success.main' : 'error.main'}>
          {formatCurrency(r.saldo)}
        </Typography>
      ),
    },
    {
      key: 'actions',
      header: '',
      width: 120,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Tooltip title="Ver extrato">
            <IconButton size="small" onClick={() => openExtrato(r)}>
              <ListAltIcon fontSize="small" />
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
        title="Contas Bancárias"
        subtitle="Gerencie as contas bancárias do condomínio."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Nova Conta
          </Button>
        }
      />

      <Grid container spacing={3}>
        <Grid item xs={12} md={extrato ? 7 : 12}>
          <DataTable
            columns={columns}
            rows={contas}
            keyField="id"
            loading={isLoading}
            emptyMessage="Nenhuma conta bancária cadastrada."
          />
        </Grid>

        {extrato && (
          <Grid item xs={12} md={5}>
            <Paper sx={{ p: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <AccountBalanceWalletIcon color="primary" />
                <Typography variant="h6">
                  Extrato — {extrato.conta.bancoNome} {extrato.conta.agencia}/{extrato.conta.conta}
                </Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />
              <DataTable
                columns={lancamentoColumns}
                rows={extrato.lancamentos}
                keyField="id"
                emptyMessage="Sem lançamentos."
              />
            </Paper>
          </Grid>
        )}
      </Grid>

      <FormDialog
        open={dialogOpen}
        title={editTarget ? 'Editar Conta' : 'Nova Conta Bancária'}
        formId="conta-bancaria-form"
        loading={create.isPending || update.isPending}
        onClose={() => setDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="conta-bancaria-form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <Controller
            name="bancoId"
            control={control}
            render={({ field }) => (
              <TextField select label="Banco" fullWidth {...field} error={!!errors.bancoId} helperText={errors.bancoId?.message}>
                {bancos.map((b) => (
                  <MenuItem key={b.id} value={b.id}>{b.codigo} — {b.nome}</MenuItem>
                ))}
              </TextField>
            )}
          />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              label="Agência"
              sx={{ flex: 1 }}
              {...register('agencia')}
              error={!!errors.agencia}
              helperText={errors.agencia?.message}
            />
            <TextField
              label="Conta"
              sx={{ flex: 1 }}
              {...register('conta')}
              error={!!errors.conta}
              helperText={errors.conta?.message}
            />
          </Box>
          <Controller
            name="tipo"
            control={control}
            render={({ field }) => (
              <TextField select label="Tipo" fullWidth {...field}>
                {Object.entries(TIPO_CONTA_LABELS).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField label="Descrição (opcional)" fullWidth {...register('descricao')} />
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteTarget}
        title="Excluir Conta"
        message={`Excluir a conta ${deleteTarget?.agencia}/${deleteTarget?.conta}?`}
        destructive
        loading={remove.isPending}
        onConfirm={() => remove.mutate(deleteTarget!.id, { onSuccess: () => setDeleteTarget(null) })}
        onCancel={() => setDeleteTarget(null)}
      />
    </Box>
  )
}
