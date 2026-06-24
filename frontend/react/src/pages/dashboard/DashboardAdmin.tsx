import { Grid } from '@mui/material'
import LocationCityIcon from '@mui/icons-material/LocationCity'
import PeopleIcon from '@mui/icons-material/People'
import CreditCardIcon from '@mui/icons-material/CreditCard'
import CalculateIcon from '@mui/icons-material/Calculate'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/common'
import { formatCurrency, formatDate } from '@/utils/formatters'
import { useDashboardAdmin } from '@/hooks/useDashboardAdmin'
import { StatCard } from './components/StatCard'
import { SectionCard } from './components/SectionCard'
import { EmptySection } from './components/EmptySection'

export default function DashboardAdmin() {
  const navigate = useNavigate()
  const { totalCondominios, totalUsuarios, cobrancas, rateio } = useDashboardAdmin()

  return (
    <>
      <PageHeader
        title="Administração da Plataforma"
        subtitle="CondoGest — visão global"
      />

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Condomínios"
            value={String(totalCondominios.data ?? '—')}
            subtitle="na plataforma"
            icon={<LocationCityIcon />}
            color="#1565C0"
            loading={totalCondominios.isLoading}
            onClick={() => navigate('/condominios')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Usuários Ativos"
            value={String(totalUsuarios.data ?? '—')}
            subtitle="na plataforma"
            icon={<PeopleIcon />}
            color="#2E7D32"
            loading={totalUsuarios.isLoading}
            onClick={() => navigate('/usuarios')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Cobranças Pendentes"
            value={cobrancas.data ? formatCurrency(cobrancas.data.totalPendente) : '—'}
            subtitle={
              cobrancas.data
                ? `${cobrancas.data.quantidadePendente} unid. · vencidas: ${cobrancas.data.quantidadeVencida}`
                : undefined
            }
            icon={<CreditCardIcon />}
            color="#E65100"
            loading={cobrancas.isLoading}
            onClick={() => navigate('/cobrancas')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Último Rateio"
            value={rateio.data ? formatDate(rateio.data.dataExecucao) : '—'}
            subtitle={rateio.data?.status}
            icon={<CalculateIcon />}
            color="#6A1B9A"
            loading={rateio.isLoading}
          />
        </Grid>
      </Grid>

      <Grid container spacing={3} mt={1}>
        <Grid item xs={12} md={6}>
          <SectionCard
            title="Atividade do Sistema"
            loading={rateio.isLoading && cobrancas.isLoading}
            empty={!rateio.data && !cobrancas.data && !rateio.isLoading}
            emptyMessage="Nenhuma atividade registrada."
          >
            {rateio.data && (
              <>
                <StatCard
                  title="Último rateio"
                  value={formatDate(rateio.data.dataExecucao)}
                  subtitle={`Total: ${formatCurrency(rateio.data.despesaTotal)} · ${rateio.data.status}`}
                  icon={<CalculateIcon />}
                  color="#6A1B9A"
                />
              </>
            )}
            {cobrancas.data && (
              <StatCard
                title="Cobranças vencidas"
                value={String(cobrancas.data.quantidadeVencida)}
                subtitle={formatCurrency(cobrancas.data.totalVencido)}
                icon={<CreditCardIcon />}
                color="#C62828"
              />
            )}
            {!rateio.data && !cobrancas.data && !rateio.isLoading && (
              <EmptySection message="Nenhuma atividade registrada." />
            )}
          </SectionCard>
        </Grid>
        <Grid item xs={12} md={6}>
          <SectionCard title="Links Rápidos">
            <Grid container spacing={1}>
              {[
                { label: 'Gerenciar Condomínios', path: '/condominios' },
                { label: 'Gerenciar Usuários', path: '/usuarios' },
                { label: 'Execuções de Rateio', path: '/rateio/execucoes' },
                { label: 'Orçamento Anual', path: '/financeiro/orcamento' },
              ].map((item) => (
                <Grid item xs={12} key={item.path}>
                  <StatCard
                    title={item.label}
                    value="→"
                    icon={<LocationCityIcon />}
                    color="#1565C0"
                    onClick={() => navigate(item.path)}
                  />
                </Grid>
              ))}
            </Grid>
          </SectionCard>
        </Grid>
      </Grid>
    </>
  )
}
