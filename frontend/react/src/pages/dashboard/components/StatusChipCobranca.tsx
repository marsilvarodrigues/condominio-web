import { Box, Typography } from '@mui/material'
import type { StatusCobranca } from '@/types'

const STATUS_STYLE: Record<StatusCobranca, { bg: string; color: string; bold?: boolean }> = {
  PENDENTE:    { bg: '#E3F2FD', color: '#1565C0' },
  ENVIADA:     { bg: '#E8F5E9', color: '#2E7D32' },
  VISUALIZADA: { bg: '#F3E5F5', color: '#6A1B9A' },
  PAGA:        { bg: '#E8F5E9', color: '#1B5E20', bold: true },
  VENCIDA:     { bg: '#FCE4EC', color: '#C62828' },
  CANCELADA:   { bg: '#ECEFF1', color: '#546E7A' },
}

const STATUS_LABEL: Record<StatusCobranca, string> = {
  PENDENTE:    'Pendente',
  ENVIADA:     'Enviada',
  VISUALIZADA: 'Visualizada',
  PAGA:        'Paga',
  VENCIDA:     'Vencida',
  CANCELADA:   'Cancelada',
}

interface Props {
  status: StatusCobranca
}

export function StatusChipCobranca({ status }: Props) {
  const style = STATUS_STYLE[status] ?? { bg: '#ECEFF1', color: '#546E7A' }
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        px: 1,
        py: 0.25,
        borderRadius: 1,
        bgcolor: style.bg,
      }}
    >
      <Typography
        variant="caption"
        fontWeight={style.bold ? 700 : 500}
        sx={{ color: style.color }}
      >
        {STATUS_LABEL[status] ?? status}
      </Typography>
    </Box>
  )
}
