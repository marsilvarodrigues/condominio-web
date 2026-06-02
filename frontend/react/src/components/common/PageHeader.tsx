import { Box, Typography, Divider, type SxProps } from '@mui/material'
import type { ReactNode } from 'react'

interface Props {
  title: string
  subtitle?: string
  actions?: ReactNode
  sx?: SxProps
}

export function PageHeader({ title, subtitle, actions, sx }: Props) {
  return (
    <Box sx={{ mb: 3, ...sx }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography variant="h5" component="h1">
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="subtitle1" sx={{ mt: 0.5 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions && <Box sx={{ display: 'flex', gap: 1 }}>{actions}</Box>}
      </Box>
      <Divider sx={{ mt: 2, borderColor: 'rgba(21,101,192,0.12)' }} />
    </Box>
  )
}
