import { Box, Card, Typography } from '@mui/material'
import { useAuthStore } from '@/store/authStore'

export default function DashboardUsuario() {
  const user = useAuthStore((s) => s.user)

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <Card sx={{ maxWidth: 480, textAlign: 'center', p: 4 }}>
        <Typography fontSize={56} mb={2}>🏢</Typography>
        <Typography variant="h5" fontWeight={700} gutterBottom>
          Bem-vindo ao CondoGest
        </Typography>
        <Typography color="text.secondary" mb={3}>
          Olá, <strong>{user?.email}</strong>.<br />
          Seu perfil ainda não está associado a nenhuma unidade ou condomínio.
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Entre em contato com o síndico ou administrador do seu condomínio para configurar seu acesso.
        </Typography>
      </Card>
    </Box>
  )
}
