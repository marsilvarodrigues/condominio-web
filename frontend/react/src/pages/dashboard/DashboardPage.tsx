import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  LinearProgress,
  Alert,
} from '@mui/material'
import ApartmentIcon from '@mui/icons-material/Apartment'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import CalculateIcon from '@mui/icons-material/Calculate'
import { PageHeader } from '@/components/common'
import { formatCurrency, formatPercent } from '@/utils/formatters'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'

interface StatCardProps {
  title: string
  value: string
  subtitle?: string
  icon: React.ReactNode
  color: string
  progress?: number
}

function StatCard({ title, value, subtitle, icon, color, progress }: StatCardProps) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography variant="caption" color="text.secondary" fontWeight={600} textTransform="uppercase" letterSpacing={0.5}>
              {title}
            </Typography>
            <Typography variant="h4" fontWeight={700} mt={0.5} color={color}>
              {value}
            </Typography>
            {subtitle && (
              <Typography variant="caption" color="text.secondary">
                {subtitle}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: 2,
              bgcolor: `${color}18`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color,
            }}
          >
            {icon}
          </Box>
        </Box>
        {progress !== undefined && (
          <Box mt={2}>
            <LinearProgress
              variant="determinate"
              value={progress}
              sx={{
                height: 6,
                borderRadius: 3,
                bgcolor: `${color}20`,
                '& .MuiLinearProgress-bar': { bgcolor: color, borderRadius: 3 },
              }}
            />
          </Box>
        )}
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const isAdmin = useAuthStore((s) => s.hasRole(ROLES.ADMIN))

  return (
    <Box>
      <PageHeader
        title={isAdmin ? 'Painel Administrativo' : 'Dashboard'}
        subtitle={`Bem-vindo, ${user?.email ?? 'usuário'}`}
      />

      <Alert severity="info" sx={{ mb: 3 }}>
        Conecte a API para exibir dados reais. Os valores abaixo são placeholders de exemplo.
      </Alert>

      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total de Apartamentos"
            value="120"
            subtitle="8 vagos"
            icon={<ApartmentIcon />}
            color="#1565C0"
            progress={93}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Saldo Bancário"
            value={formatCurrency(48_320.5)}
            subtitle="Atualizado hoje"
            icon={<AccountBalanceWalletIcon />}
            color="#2E7D32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Despesas do Mês"
            value={formatCurrency(12_400)}
            subtitle="vs R$ 11.800 mês anterior"
            icon={<TrendingDownIcon />}
            color="#E65100"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Inadimplência"
            value={formatPercent(4.2)}
            subtitle="5 unidades"
            icon={<WarningAmberIcon />}
            color="#C62828"
            progress={4.2}
          />
        </Grid>
      </Grid>

      <Grid container spacing={3} mt={1}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <CalculateIcon color="primary" />
                <Typography variant="h6">Último Rateio</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary">
                Rateio automático executado em 02/06/2026 às 02:00 — SUCESSO
              </Typography>
              <Typography variant="body2" color="text.secondary" mt={1}>
                120 unidades rateadas · Total: {formatCurrency(12_400)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" mb={2}>
                Resumo Financeiro
              </Typography>
              {[
                { label: 'Receita prevista', value: formatCurrency(15_000), color: '#2E7D32' },
                { label: 'Despesa realizada', value: formatCurrency(12_400), color: '#E65100' },
                { label: 'Fundo de reserva', value: formatCurrency(8_240), color: '#1565C0' },
              ].map((item) => (
                <Box
                  key={item.label}
                  sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}
                >
                  <Typography variant="body2" color="text.secondary">
                    {item.label}
                  </Typography>
                  <Typography variant="body2" fontWeight={600} color={item.color}>
                    {item.value}
                  </Typography>
                </Box>
              ))}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
