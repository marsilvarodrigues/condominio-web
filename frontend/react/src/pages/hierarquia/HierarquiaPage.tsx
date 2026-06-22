import {
  Box,
  Button,
  Chip,
  Grid,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Paper,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import OpenInNewIcon from '@mui/icons-material/OpenInNew'
import ApartmentIcon from '@mui/icons-material/Apartment'
import MeetingRoomIcon from '@mui/icons-material/MeetingRoom'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { PageHeader, DataTable, ConfirmDialog, FormDialog, NoCondominioGuard, type Column } from '@/components/common'
import { blocosApi } from '@/api/blocos.api'
import { apartamentosApi } from '@/api/apartamentos.api'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'
import type { BlocoDTO, ApartamentoDTO, CreateBlocoDTO, CreateApartamentoDTO } from '@/types'

export default function HierarquiaPage() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const hasRole = useAuthStore((s) => s.hasRole)
  const canWrite = hasRole(ROLES.ADMIN) || hasRole(ROLES.SINDICO)
  const [selectedBloco, setSelectedBloco] = useState<BlocoDTO | null>(null)
  const [blocoDialogOpen, setBlocoDialogOpen] = useState(false)
  const [aptDialogOpen, setAptDialogOpen] = useState(false)
  const [deleteBloco, setDeleteBloco] = useState<BlocoDTO | null>(null)
  const [deleteApt, setDeleteApt] = useState<ApartamentoDTO | null>(null)

  const { data: blocos = [], isLoading: loadingBlocos } = useQuery({
    queryKey: ['blocos'],
    queryFn: blocosApi.list,
  })

  const { data: apartamentos = [], isLoading: loadingApts } = useQuery({
    queryKey: ['apartamentos', selectedBloco?.id],
    queryFn: () => apartamentosApi.list(selectedBloco?.id),
    enabled: !!selectedBloco,
  })

  const blocoForm = useForm<CreateBlocoDTO>()
  const aptForm = useForm<CreateApartamentoDTO>()

  const createBloco = useMutation({
    mutationFn: blocosApi.create,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['blocos'] }); setBlocoDialogOpen(false) },
  })

  const removeBloco = useMutation({
    mutationFn: blocosApi.remove,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['blocos'] }); setDeleteBloco(null) },
  })

  const createApt = useMutation({
    mutationFn: aptForm.handleSubmit(async (v) => {
      await apartamentosApi.create({ ...v, blocoId: selectedBloco!.id })
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['apartamentos', selectedBloco?.id] }); setAptDialogOpen(false) },
  })

  const removeApt = useMutation({
    mutationFn: apartamentosApi.remove,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['apartamentos', selectedBloco?.id] }); setDeleteApt(null) },
  })

  const aptColumns: Column<ApartamentoDTO>[] = [
    { key: 'numero', header: 'Número', render: (r) => r.numero },
    { key: 'andar', header: 'Andar', render: (r) => r.andar ?? '—' },
    { key: 'fracaoIdeal', header: 'Fração Ideal', align: 'right', render: (r) => r.fracaoIdeal != null ? `${r.fracaoIdeal}%` : '—' },
    { key: 'areaConstruida', header: 'Metragem', align: 'right', render: (r) => r.areaConstruida != null ? `${r.areaConstruida} m²` : '—' },
    {
      key: 'ocupacao',
      header: 'Ocupação',
      render: (r) =>
        r.quantidadeMoradores > 0 ? (
          <Chip label={`${r.quantidadeMoradores} morador(es)`} size="small" color="success" />
        ) : (
          <Chip label="Vago" size="small" variant="outlined" />
        ),
    },
    {
      key: 'actions',
      header: '',
      width: 90,
      align: 'right',
      render: (r) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Tooltip title="Ver detalhes">
            <IconButton size="small" onClick={() => navigate(`/hierarquia/apartamentos/${r.id}`)}>
              <OpenInNewIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {canWrite && (
            <IconButton size="small" color="error" onClick={() => setDeleteApt(r)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          )}
        </Box>
      ),
    },
  ]

  return (
    <Box>
      <NoCondominioGuard />
      <PageHeader
        title="Hierarquia — Blocos e Apartamentos"
        subtitle="Selecione um bloco para gerenciar seus apartamentos."
      />

      <Grid container spacing={3}>
        {/* Lista de Blocos */}
        <Grid item xs={12} md={3}>
          <Paper sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle2">Blocos</Typography>
              {canWrite && (
                <Button size="small" startIcon={<AddIcon />} onClick={() => setBlocoDialogOpen(true)}>
                  Novo
                </Button>
              )}
            </Box>

            {loadingBlocos ? (
              <Typography variant="body2" color="text.secondary">
                Carregando...
              </Typography>
            ) : (
              <List disablePadding>
                {blocos.map((bloco) => (
                  <ListItemButton
                    key={bloco.id}
                    selected={selectedBloco?.id === bloco.id}
                    onClick={() => setSelectedBloco(bloco)}
                    sx={{ borderRadius: 1, mb: 0.5 }}
                  >
                    <ListItemIcon sx={{ minWidth: 32 }}>
                      <ApartmentIcon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText
                      primary={`Bloco ${bloco.bloco}`}
                      secondary={`Nº ${bloco.numero}`}
                      primaryTypographyProps={{ fontSize: 14 }}
                      secondaryTypographyProps={{ fontSize: 12 }}
                    />
                    {canWrite && (
                      <Tooltip title="Excluir">
                        <IconButton
                          size="small"
                          color="error"
                          onClick={(e) => { e.stopPropagation(); setDeleteBloco(bloco) }}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </ListItemButton>
                ))}
              </List>
            )}
          </Paper>
        </Grid>

        {/* Apartamentos do Bloco */}
        <Grid item xs={12} md={9}>
          {selectedBloco ? (
            <Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  Bloco {selectedBloco.bloco} — Apartamentos
                </Typography>
                {canWrite && (
                  <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAptDialogOpen(true)}>
                    Novo Apartamento
                  </Button>
                )}
              </Box>
              <DataTable
                columns={aptColumns}
                rows={apartamentos}
                keyField="id"
                loading={loadingApts}
                emptyMessage="Nenhum apartamento neste bloco."
              />
            </Box>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 8, color: 'text.secondary' }}>
              <MeetingRoomIcon sx={{ fontSize: 48, opacity: 0.3, mb: 1 }} />
              <Typography variant="body2">Selecione um bloco para ver os apartamentos.</Typography>
            </Box>
          )}
        </Grid>
      </Grid>

      {/* Dialog Bloco */}
      <FormDialog
        open={blocoDialogOpen}
        title="Novo Bloco"
        formId="bloco-form"
        loading={createBloco.isPending}
        onClose={() => setBlocoDialogOpen(false)}
        maxWidth="xs"
      >
        <Box
          component="form"
          id="bloco-form"
          onSubmit={blocoForm.handleSubmit((v) => createBloco.mutate(v))}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField label="Número" type="number" fullWidth {...blocoForm.register('numero', { valueAsNumber: true })} />
          <TextField label="Identificação (ex: A, B)" fullWidth {...blocoForm.register('bloco')} />
        </Box>
      </FormDialog>

      {/* Dialog Apartamento */}
      <FormDialog
        open={aptDialogOpen}
        title="Novo Apartamento"
        formId="apt-form"
        loading={createApt.isPending}
        onClose={() => setAptDialogOpen(false)}
      >
        <Box
          component="form"
          id="apt-form"
          onSubmit={aptForm.handleSubmit((v) =>
            apartamentosApi
              .create({ ...v, blocoId: selectedBloco!.id })
              .then(() => {
                qc.invalidateQueries({ queryKey: ['apartamentos', selectedBloco?.id] })
                setAptDialogOpen(false)
              }),
          )}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField label="Número" fullWidth {...aptForm.register('numero')} />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField label="Andar" type="number" sx={{ flex: 1 }} {...aptForm.register('andar', { valueAsNumber: true })} />
            <TextField label="Área Construída (m²)" type="number" sx={{ flex: 1 }} {...aptForm.register('areaConstruida', { valueAsNumber: true })} />
            <TextField label="Fração Ideal (%)" type="number" sx={{ flex: 1 }} {...aptForm.register('fracaoIdeal', { valueAsNumber: true })} />
          </Box>
        </Box>
      </FormDialog>

      <ConfirmDialog
        open={!!deleteBloco}
        title="Excluir Bloco"
        message={`Deseja excluir o Bloco ${deleteBloco?.bloco}? Todos os apartamentos serão removidos.`}
        destructive
        loading={removeBloco.isPending}
        onConfirm={() => removeBloco.mutate(deleteBloco!.id)}
        onCancel={() => setDeleteBloco(null)}
      />

      <ConfirmDialog
        open={!!deleteApt}
        title="Excluir Apartamento"
        message={`Deseja excluir o apartamento ${deleteApt?.numero}?`}
        destructive
        loading={removeApt.isPending}
        onConfirm={() => removeApt.mutate(deleteApt!.id)}
        onCancel={() => setDeleteApt(null)}
      />
    </Box>
  )
}
