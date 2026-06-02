import { Box, Button, Typography } from '@mui/material'
import type { ReactNode, SyntheticEvent } from 'react'

interface Props {
  icon?: ReactNode
  title: string
  description?: string
  actionLabel?: string
  onAction?: (e: SyntheticEvent) => void
}

export function EmptyState({ icon, title, description, actionLabel, onAction }: Props) {
  return (
    <Box
      sx={{
        py: 8,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 2,
        color: 'text.secondary',
      }}
    >
      {icon && <Box sx={{ fontSize: 56, opacity: 0.4, lineHeight: 1 }}>{icon}</Box>}
      <Typography variant="h6" color="text.secondary">
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" textAlign="center" maxWidth={320}>
          {description}
        </Typography>
      )}
      {actionLabel && onAction && (
        <Button variant="contained" onClick={onAction} sx={{ mt: 1 }}>
          {actionLabel}
        </Button>
      )}
    </Box>
  )
}
