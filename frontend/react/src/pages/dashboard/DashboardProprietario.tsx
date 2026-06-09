import {
  Box,
  Button,
  Chip,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material'
import HomeWorkIcon from '@mui/icons-material/HomeWork'
import LocationCityIcon from '@mui/icons-material/LocationCity'
import PersonIcon from '@mui/icons-material/Person'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/common'
import { useDashboardProprietario } from '@/hooks/useDashboardProprietario'
import { StatCard } from './components/StatCard'
import { SectionCard } from './components/SectionCard'
import { EmptySection } from './components/EmptySection'

const CONDO_COLORS = [
  { bg: '#E3F2FD', color: '#1565C0' },
  { bg: '#F3E5F5', color: '#6A1B9A' },
  { bg: '#E8F5E9', color: '#2E7D32' },
  { bg: '#FFF8E1', color: '#F57F17' },
]

export default function DashboardProprietario() {
  const navigate = useNavigate()
  const { proprietario, metricas } = useDashboardProprietario()

  const apts = proprietario.data?.apartamentos ?? []
  const condIds = [...new Set(apts.map((a) => a.condominioId))]
  const condColorMap = Object.fromEntries(
    condIds.map((id, i) => [id, CONDO_COLORS[i % CONDO_COLORS.length]]),
  )

  return (
    <>
      <PageHeader
        title="Meus Imóveis"
        subtitle={`${metricas.totalImoveis} imóvel(is) em ${metricas.totalCondominios} condomínio(s)`}
      />

      <Grid container spacing={3}>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="Total de Imóveis"
            value={String(metricas.totalImoveis)}
            icon={<HomeWorkIcon />}
            color="#1565C0"
            loading={proprietario.isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="Condomínios"
            value={String(metricas.totalCondominios)}
            icon={<LocationCityIcon />}
            color="#6A1B9A"
            loading={proprietario.isLoading}
          />
        </Grid>
        <Grid item xs={12} sm={4}>
          <StatCard
            title="Seu Cadastro"
            value={proprietario.data?.nome ?? '—'}
            subtitle={
              proprietario.data?.cpf ??
              proprietario.data?.cnpj ??
              proprietario.data?.email ??
              undefined
            }
            icon={<PersonIcon />}
            color="#2E7D32"
            loading={proprietario.isLoading}
          />
        </Grid>
      </Grid>

      <Grid container spacing={3} mt={1}>
        <Grid item xs={12}>
          <SectionCard
            title="Portfólio de Imóveis"
            subtitle={metricas.totalCondominios > 1 ? '🌐 cross-condomínio' : undefined}
            loading={proprietario.isLoading}
            empty={!proprietario.isLoading && apts.length === 0}
            emptyMessage="Nenhum imóvel cadastrado."
          >
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Condomínio</TableCell>
                  <TableCell>Apartamento</TableCell>
                  <TableCell>Bloco</TableCell>
                  <TableCell align="right">Ação</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {apts.map((apt) => {
                  const style = condColorMap[apt.condominioId] ?? CONDO_COLORS[0]
                  return (
                    <TableRow key={apt.id}>
                      <TableCell>
                        <Chip
                          label={`#${apt.condominioId}`}
                          size="small"
                          sx={{ bgcolor: style.bg, color: style.color, fontWeight: 600 }}
                        />
                      </TableCell>
                      <TableCell>{apt.numero}</TableCell>
                      <TableCell>{apt.blocoNome ?? '—'}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          onClick={() => navigate(`/hierarquia/apartamentos/${apt.id}`)}
                        >
                          ↗ Ver
                        </Button>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </SectionCard>
        </Grid>
      </Grid>

      {!proprietario.isLoading && apts.length === 0 && (
        <Box mt={2}>
          <EmptySection message="Nenhum imóvel associado ao seu cadastro." />
        </Box>
      )}
    </>
  )
}
