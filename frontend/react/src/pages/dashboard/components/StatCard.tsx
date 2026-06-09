import { Box, Card, CardContent, LinearProgress, Skeleton, Typography } from '@mui/material'

export interface StatCardProps {
  title: string
  value: string
  subtitle?: string
  icon: React.ReactNode
  color: string
  progress?: number
  loading?: boolean
  onClick?: () => void
}

export function StatCard({
  title,
  value,
  subtitle,
  icon,
  color,
  progress,
  loading,
  onClick,
}: StatCardProps) {
  if (loading) {
    return (
      <Card>
        <CardContent>
          <Skeleton variant="text" width="60%" height={20} />
          <Skeleton variant="text" width="40%" height={40} sx={{ mt: 0.5 }} />
          <Skeleton variant="text" width="50%" height={16} />
        </CardContent>
      </Card>
    )
  }

  return (
    <Card
      onClick={onClick}
      sx={{ cursor: onClick ? 'pointer' : 'default', '&:hover': onClick ? { boxShadow: 4 } : {} }}
    >
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography
              variant="caption"
              color="text.secondary"
              fontWeight={600}
              textTransform="uppercase"
              letterSpacing={0.5}
            >
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
              value={Math.min(progress, 100)}
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
