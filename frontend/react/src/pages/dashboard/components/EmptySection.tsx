import { Box, Button, Typography } from '@mui/material'

interface EmptySectionProps {
  message: string
  icon?: React.ReactNode
  actionLabel?: string
  onAction?: () => void
}

export function EmptySection({ message, icon, actionLabel, onAction }: EmptySectionProps) {
  return (
    <Box sx={{ textAlign: 'center', py: 4 }}>
      {icon && (
        <Box sx={{ color: 'text.disabled', mb: 1 }}>{icon}</Box>
      )}
      <Typography variant="body2" color="text.secondary">
        {message}
      </Typography>
      {actionLabel && onAction && (
        <Button size="small" sx={{ mt: 1 }} onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </Box>
  )
}
