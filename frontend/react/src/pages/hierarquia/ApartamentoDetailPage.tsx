import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  IconButton,
  MenuItem,
  Paper,
  Tab,
  Tabs,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import PersonRemoveIcon from '@mui/icons-material/PersonRemove'
import EditIcon from '@mui/icons-material/Edit'
import AddIcon from '@mui/icons-material/Add'
import LinkOffIcon from '@mui/icons-material/LinkOff'
import EmailIcon from '@mui/icons-material/Email'
import CancelIcon from '@mui/icons-material/Cancel'
import CreditCardIcon from '@mui/icons-material/CreditCard'
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { format } from 'date-fns'
import { ptBR } from 'date-fns/locale'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, type Column } from '@/components/common'
import { apartamentosApi } from '@/api/apartamentos.api'
import {
  useMoradoresDeApartamento,
  useProprietariosDeApartamento,
} from '@/hooks/useMoradores'
import { useCobrancasDoApartamento, useCobrancaMutations } from '@/hooks/useCobrancas'
import type { PessoaDTO, ProprietarioDTO, CreateProprietarioDTO, CobrancaResumoDTO } from '@/types'

const moradorSchema = z.object({
  nome: z.string().min(2, 'Nome obrigatório'),
  email: z.string().email('E-mail inválido'),
  cpf: z.string().min(11, 'CPF obrigatório'),
  telefone: z.string().optional(),
})
type MoradorFormValues = z.infer<typeof moradorSchema>

const proprietarioSchema = z
  .object({
    nome: z.string().min(2, 'Nome obrigatório'),
    email: z.string().email('E-mail inválido'),
    tipo: z.enum(['PROP_PF', 'PROP_PJ'] as const),
    cpf: z.string().optional(),
    cnpj: z.string().optional(),
    razaoSocial: z.string().optional(),
    telefone: z.string().optional(),
  })
  .refine((v) => v.tipo !== 'PROP_PF' || (v.cpf && v.cpf.length >= 11), {
    message: 'CPF obrigatório', path: ['cpf'],
  })
  .refine((v) => v.tipo !== 'PROP_PJ' || (v.cnpj && v.cnpj.length >= 14), {
    message: 'CNPJ obrigatório', path: ['cnpj'],
  })
type ProprietarioFormValues = z.infer<typeof proprietarioSchema>

interface TabPanelProps { children?: React.ReactNode; value: number; index: number }
function TabPanel({ children, value, index }: TabPanelProps) {
  return value === index ? <Box sx={{ pt: 3 }}>{children}</Box> : null
}

export default function ApartamentoDetailPage() {
  const { id } = useParams<{ id: string }>()
  const aptId = Number(id)
  const navigate = useNavigate()
  const [tab, setTab] = useState(0)

  const [moradorDialog, setMoradorDialog] = useState<{ open: boolean; editing: PessoaDTO | null }>({
    open: false, editing: null,
  })
  const [removeTarget, setRemoveTarget] = useState<PessoaDTO | null>(null)
  const [propDialog, setPropDialog] = useState<{ open: boolean; editing: ProprietarioDTO | null }>({
    open: false, editing: null,
  })
  const [dissocTarget, setDissocTarget] = useState<{ propId: number; nome: string } | null>(null)
  const [cancelTarget, setCancelTarget] = useState<CobrancaResumoDTO | null>(null)

  const { data: apt } = useQuery({
    queryKey: ['apartamento', aptId],
    queryFn: () => apartamentosApi.getById(aptId),
    enabled: aptId > 0,
  })

  const { query: moradoresQuery, create: createMorador, update: updateMorador, remove: removeMorador } =
    useMoradoresDeApartamento(aptId)
  const moradoresLista = moradoresQuery.data ?? []

  const { query: propQuery, create: createProp, update: updateProp, desassociar } =
    useProprietariosDeApartamento(aptId)
  const proprietarios = propQuery.data ?? []

  const cobrancasQuery = useCobrancasDoApartamento(aptId)
  const cobrancas = cobrancasQuery.data?.content ?? []
  const { cancelar: cancelarCobranca, reenviarEmail } = useCobrancaMutations(aptId)

  const moradorForm = useForm<MoradorFormValues>({ resolver: zodResolver(moradorSchema) })
  const propForm = useForm<ProprietarioFormValues>({
    resolver: zodResolver(proprietarioSchema),
    defaultValues: { tipo: 'PROP_PF' },
  })
  const tipoWatch = propForm.watch('tipo')

  const aptLabel = apt
    ? `Apt ${apt.numero} — Bloco ${apt.blocoNome ?? ''}`
    : `Apartamento #${aptId}`

  const statusChipStyle = (status: string) => {
    const map: Record<string, { bg: string; color: string; fw?: string; td?: string }> = {
      PENDENTE:    { bg: '#E3F2FD', color: '#1565C0' },
      ENVIADA:     { bg: '#E8F5E9', color: '#2E7D32' },
      VISUALIZADA: { bg: '#F3E5F5', color: '#6A1B9A' },
      PAGA:        { bg: '#E8F5E9', color: '#1B5E20', fw: '700' },
      VENCIDA:     { bg: '#FCE4EC', color: '#C62828' },
      CANCELADA:   { bg: '#ECEFF1', color: '#546E7A', td: 'line-through' },
    }
    return map[status] ?? { bg: '#F5F5F5', color: '#555' }
  }

  const cobrancasColumns: Column<CobrancaResumoDTO>[] = [
    {
      key: 'criadaEm',
      header: 'Emitida em',
      render: (r) => format(new Date(r.criadaEm), 'dd/MM/yyyy', { locale: ptBR }),
    },
    {
      key: 'vencimento',
      header: 'Vencimento',
      render: (r) => {
        const isOverdue = r.status === 'VENCIDA'
        return (
          <Typography variant="body2" color={isOverdue ? 'error' : 'inherit'} fontWeight={isOverdue ? 700 : 400}>
            {format(new Date(r.vencimento), 'dd/MM/yyyy', { locale: ptBR })}
          </Typography>
        )
      },
    },
    {
      key: 'valor',
      header: 'Valor',
      render: (r) => `R$ ${r.valor.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => {
        const s = statusChipStyle(r.status)
        return (
          <Box component="span" sx={{
            px: 1.5, py: 0.5, borderRadius: 3, fontSize: 12,
            fontWeight: s.fw ?? 500, background: s.bg, color: s.color,
            textDecoration: s.td,
          }}>
            {r.status}
          </Box>
        )
      },
    },
    {
      key: 'actions',
      header: '',
      width: 100,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 0.5 }}>
          <Tooltip title="Reenviar e-mail">
            <IconButton size="small" onClick={() => reenviarEmail.mutate(r.id)}>
              <EmailIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {r.status !== 'CANCELADA' && r.status !== 'PAGA' && (
            <Tooltip title="Cancelar cobrança">
              <IconButton size="small" color="error" onClick={() => setCancelTarget(r)}>
                <CancelIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      ),
    },
  ]

  function openNewMorador() {
    moradorForm.reset({ nome: '', email: '', cpf: '', telefone: '' })
    setMoradorDialog({ open: true, editing: null })
  }

  function openEditMorador(r: PessoaDTO) {
    moradorForm.reset({
      nome: r.nome,
      email: r.email ?? '',
      cpf: r.cpf ?? '',
      telefone: r.telefone ?? '',
    })
    setMoradorDialog({ open: true, editing: r })
  }

  function openNewProp() {
    propForm.reset({ tipo: 'PROP_PF', nome: '', email: '', cpf: '', cnpj: '', razaoSocial: '', telefone: '' })
    setPropDialog({ open: true, editing: null })
  }

  function openEditProp(r: ProprietarioDTO) {
    propForm.reset({
      tipo: r.tipo,
      nome: r.nome,
      email: r.email ?? '',
      cpf: r.cpf ?? '',
      cnpj: r.cnpj ?? '',
      razaoSocial: r.razaoSocial ?? '',
      telefone: r.telefone ?? '',
    })
    setPropDialog({ open: true, editing: r })
  }

  const moradoresColumns: Column<PessoaDTO>[] = [
    { key: 'nome', header: 'Nome', render: (r) => r.nome },
    { key: 'email', header: 'E-mail', render: (r) => r.email ?? '—' },
    { key: 'cpf', header: 'CPF', render: (r) => r.cpf ?? '—' },
    {
      key: 'actions',
      header: '',
      width: 80,
      align: 'right',
      render: (r) => (
        <>
          <Tooltip title="Editar">
            <IconButton size="small" onClick={() => openEditMorador(r)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Excluir morador">
            <IconButton size="small" color="error" onClick={() => setRemoveTarget(r)}>
              <PersonRemoveIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </>
      ),
    },
  ]

  const proprietariosColumns: Column<ProprietarioDTO>[] = [
    { key: 'nome', header: 'Nome', render: (r) => r.nome },
    {
      key: 'tipo',
      header: 'Tipo',
      render: (r) => (
        <Chip
          label={r.tipo === 'PROP_PF' ? 'Pessoa Física' : 'Pessoa Jurídica'}
          size="small"
          variant="outlined"
        />
      ),
    },
    { key: 'doc', header: 'Documento', render: (r) => r.cpf ?? r.cnpj ?? '—' },
    { key: 'email', header: 'E-mail', render: (r) => r.email ?? '—' },
    {
      key: 'actions',
      header: '',
      width: 80,
      align: 'right',
      render: (r) => (
        <>
          <Tooltip title="Editar">
            <IconButton size="small" onClick={() => openEditProp(r)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Desassociar do apartamento">
            <IconButton
              size="small"
              color="warning"
              onClick={() => setDissocTarget({ propId: r.id, nome: r.nome })}
            >
              <LinkOffIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </>
      ),
    },
  ]

  return (
    <Box>
      <PageHeader
        title={aptLabel}
        subtitle="Moradores e proprietários do apartamento"
        actions={
          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/hierarquia')}>
            Voltar
          </Button>
        }
      />

      <Paper sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab label="Moradores" />
          <Tab label="Proprietários" />
          <Tab icon={<CreditCardIcon />} iconPosition="start" label="Cobranças" />
        </Tabs>
      </Paper>

      {/* Tab 0 — Moradores */}
      <TabPanel value={tab} index={0}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">
            Moradores
            <Chip label={moradoresLista.length} size="small" sx={{ ml: 1 }} />
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openNewMorador}>
            Novo Morador
          </Button>
        </Box>

        {moradoresLista.length === 0 ? (
          <Card variant="outlined" sx={{ textAlign: 'center', py: 4 }}>
            <CardContent>
              <Typography color="text.secondary">Apartamento sem moradores cadastrados.</Typography>
            </CardContent>
          </Card>
        ) : (
          <DataTable
            columns={moradoresColumns}
            rows={moradoresLista}
            keyField="id"
            emptyMessage="Sem moradores."
          />
        )}
      </TabPanel>

      {/* Tab 1 — Proprietários */}
      <TabPanel value={tab} index={1}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">
            Proprietários
            <Chip label={proprietarios.length} size="small" sx={{ ml: 1 }} />
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openNewProp}>
            Novo Proprietário
          </Button>
        </Box>

        {proprietarios.length === 0 ? (
          <Card variant="outlined" sx={{ textAlign: 'center', py: 4 }}>
            <CardContent>
              <Typography color="text.secondary">Nenhum proprietário associado.</Typography>
            </CardContent>
          </Card>
        ) : (
          <DataTable
            columns={proprietariosColumns}
            rows={proprietarios}
            keyField="id"
            emptyMessage="Sem proprietários."
          />
        )}
      </TabPanel>

      {/* Tab 2 — Cobranças */}
      <TabPanel value={tab} index={2}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">
            Cobranças
            <Chip label={cobrancas.length} size="small" sx={{ ml: 1 }} />
          </Typography>
        </Box>
        {cobrancas.length === 0 ? (
          <Card variant="outlined" sx={{ textAlign: 'center', py: 4 }}>
            <CardContent>
              <Typography color="text.secondary">Nenhuma cobrança emitida para este apartamento.</Typography>
            </CardContent>
          </Card>
        ) : (
          <DataTable
            columns={cobrancasColumns}
            rows={cobrancas}
            keyField="id"
            loading={cobrancasQuery.isLoading}
            emptyMessage="Sem cobranças."
          />
        )}
      </TabPanel>

      {/* Dialog Morador — criar ou editar */}
      <FormDialog
        open={moradorDialog.open}
        title={moradorDialog.editing ? 'Editar Morador' : 'Novo Morador'}
        formId="morador-form"
        loading={createMorador.isPending || updateMorador.isPending}
        onClose={() => setMoradorDialog({ open: false, editing: null })}
      >
        <Box
          component="form"
          id="morador-form"
          onSubmit={moradorForm.handleSubmit((values) => {
            if (moradorDialog.editing) {
              updateMorador.mutate(
                { id: moradorDialog.editing.id, body: values },
                { onSuccess: () => setMoradorDialog({ open: false, editing: null }) },
              )
            } else {
              createMorador.mutate(
                { apartamentoId: aptId, ...values },
                { onSuccess: () => setMoradorDialog({ open: false, editing: null }) },
              )
            }
          })}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="Nome"
            fullWidth
            {...moradorForm.register('nome')}
            error={!!moradorForm.formState.errors.nome}
            helperText={moradorForm.formState.errors.nome?.message}
          />
          <TextField
            label="E-mail"
            type="email"
            fullWidth
            {...moradorForm.register('email')}
            error={!!moradorForm.formState.errors.email}
            helperText={moradorForm.formState.errors.email?.message}
          />
          <TextField
            label="CPF"
            fullWidth
            {...moradorForm.register('cpf')}
            error={!!moradorForm.formState.errors.cpf}
            helperText={moradorForm.formState.errors.cpf?.message}
          />
          <TextField label="Telefone" fullWidth {...moradorForm.register('telefone')} />
        </Box>
      </FormDialog>

      {/* Dialog Proprietário — criar ou editar */}
      <FormDialog
        open={propDialog.open}
        title={propDialog.editing ? 'Editar Proprietário' : 'Novo Proprietário'}
        formId="prop-form"
        loading={createProp.isPending || updateProp.isPending}
        onClose={() => setPropDialog({ open: false, editing: null })}
      >
        <Box
          component="form"
          id="prop-form"
          onSubmit={propForm.handleSubmit((values) => {
            if (propDialog.editing) {
              updateProp.mutate(
                {
                  id: propDialog.editing.id,
                  body: {
                    nome: values.nome,
                    email: values.email,
                    telefone: values.telefone || undefined,
                    cpf: values.cpf || undefined,
                    cnpj: values.cnpj || undefined,
                    razaoSocial: values.razaoSocial || undefined,
                  },
                },
                { onSuccess: () => setPropDialog({ open: false, editing: null }) },
              )
            } else {
              const body: CreateProprietarioDTO = {
                nome: values.nome,
                email: values.email,
                tipo: values.tipo,
                telefone: values.telefone || undefined,
                cpf: values.cpf || undefined,
                cnpj: values.cnpj || undefined,
                razaoSocial: values.razaoSocial || undefined,
              }
              createProp.mutate(body, {
                onSuccess: () => setPropDialog({ open: false, editing: null }),
              })
            }
          })}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <Controller
            name="tipo"
            control={propForm.control}
            render={({ field }) => (
              <TextField select label="Tipo" {...field} disabled={!!propDialog.editing}>
                <MenuItem value="PROP_PF">Pessoa Física</MenuItem>
                <MenuItem value="PROP_PJ">Pessoa Jurídica</MenuItem>
              </TextField>
            )}
          />
          <TextField
            label="Nome"
            fullWidth
            {...propForm.register('nome')}
            error={!!propForm.formState.errors.nome}
            helperText={propForm.formState.errors.nome?.message}
          />
          <TextField
            label="E-mail"
            type="email"
            fullWidth
            {...propForm.register('email')}
            error={!!propForm.formState.errors.email}
            helperText={propForm.formState.errors.email?.message}
          />
          <TextField label="Telefone" fullWidth {...propForm.register('telefone')} />
          {tipoWatch === 'PROP_PF' && (
            <TextField
              label="CPF"
              fullWidth
              {...propForm.register('cpf')}
              error={!!propForm.formState.errors.cpf}
              helperText={propForm.formState.errors.cpf?.message}
            />
          )}
          {tipoWatch === 'PROP_PJ' && (
            <>
              <TextField
                label="CNPJ"
                fullWidth
                {...propForm.register('cnpj')}
                error={!!propForm.formState.errors.cnpj}
                helperText={propForm.formState.errors.cnpj?.message}
              />
              <TextField label="Razão Social" fullWidth {...propForm.register('razaoSocial')} />
            </>
          )}
        </Box>
      </FormDialog>

      {/* Confirm excluir morador */}
      <ConfirmDialog
        open={!!removeTarget}
        title="Excluir Morador"
        message={`Excluir ${removeTarget?.nome} do sistema?`}
        destructive
        loading={removeMorador.isPending}
        onConfirm={() =>
          removeMorador.mutate(removeTarget!.id, { onSuccess: () => setRemoveTarget(null) })
        }
        onCancel={() => setRemoveTarget(null)}
      />

      {/* Confirm desassociar proprietário */}
      <ConfirmDialog
        open={!!dissocTarget}
        title="Desassociar Proprietário"
        message={`Desassociar ${dissocTarget?.nome} deste apartamento?`}
        destructive={false}
        loading={desassociar.isPending}
        onConfirm={() =>
          desassociar.mutate(dissocTarget!.propId, { onSuccess: () => setDissocTarget(null) })
        }
        onCancel={() => setDissocTarget(null)}
      />

      {/* Confirm cancelar cobrança */}
      <ConfirmDialog
        open={!!cancelTarget}
        title="Cancelar Cobrança"
        message={`Cancelar cobrança de R$ ${cancelTarget?.valor.toLocaleString('pt-BR', { minimumFractionDigits: 2 })} com vencimento ${cancelTarget ? format(new Date(cancelTarget.vencimento), 'dd/MM/yyyy', { locale: ptBR }) : ''}?`}
        destructive
        loading={cancelarCobranca.isPending}
        onConfirm={() =>
          cancelarCobranca.mutate(
            { id: cancelTarget!.id, motivo: 'Cancelamento pelo administrador' },
            { onSuccess: () => setCancelTarget(null) },
          )
        }
        onCancel={() => setCancelTarget(null)}
      />
    </Box>
  )
}
