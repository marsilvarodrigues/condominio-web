import { Box, Card, CardContent, Skeleton, Typography } from '@mui/material'
import { EmptySection } from './EmptySection'

interface SectionCardProps {
  title: string
  subtitle?: string
  action?: React.ReactNode
  loading?: boolean
  empty?: boolean
  emptyMessage?: string
  children: React.ReactNode
}

export function SectionCard({
  title,
  subtitle,
  action,
  loading,
  empty,
  emptyMessage = 'Nenhum dado disponível.',
  children,
}: SectionCardProps) {
  return (
    <Card>
      <CardContent>
        <Box
          sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}
        >
          <Box>
            <Typography variant="h6" fontWeight={600}>
              {title}
            </Typography>
            {subtitle && (
              <Typography variant="caption" color="text.secondary">
                {subtitle}
              </Typography>
            )}
          </Box>
          {action && <Box>{action}</Box>}
        </Box>

        {loading ? (
          <>
            <Skeleton variant="text" height={24} />
            <Skeleton variant="text" height={24} sx={{ mt: 1 }} />
            <Skeleton variant="text" height={24} sx={{ mt: 1 }} />
          </>
        ) : empty ? (
          <EmptySection message={emptyMessage} />
        ) : (
          children
        )}
      </CardContent>
    </Card>
  )
}
