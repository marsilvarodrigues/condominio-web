import { Paper, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material'
import type { DashboardPerfil } from '@/types'

const PERFIL_LABELS: Record<DashboardPerfil, string> = {
  admin:        '⚙️ Administrador',
  sindico:      '🔑 Síndico',
  proprietario: '🏢 Proprietário',
  morador:      '👤 Morador',
  usuario:      '👤 Usuário',
}

interface ProfileSwitcherProps {
  perfisDisponiveis: DashboardPerfil[]
  perfilAtivo: DashboardPerfil
  onChange: (perfil: DashboardPerfil) => void
}

export default function ProfileSwitcher({
  perfisDisponiveis,
  perfilAtivo,
  onChange,
}: ProfileSwitcherProps) {
  return (
    <Paper elevation={0} sx={{ p: 1.5, mb: 2, bgcolor: 'grey.50', display: 'flex', alignItems: 'center', gap: 2 }}>
      <Typography variant="body2" color="text.secondary" noWrap>
        Ver como:
      </Typography>
      <ToggleButtonGroup
        value={perfilAtivo}
        exclusive
        onChange={(_, v) => v && onChange(v as DashboardPerfil)}
        size="small"
      >
        {perfisDisponiveis.map((p) => (
          <ToggleButton key={p} value={p}>
            {PERFIL_LABELS[p]}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>
    </Paper>
  )
}
