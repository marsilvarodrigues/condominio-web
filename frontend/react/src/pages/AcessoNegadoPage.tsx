import { Box, Button, Typography } from '@mui/material'
import LockIcon from '@mui/icons-material/Lock'
import { useNavigate } from 'react-router-dom'

export default function AcessoNegadoPage() {
  const navigate = useNavigate()
  return (
    <Box sx={{ textAlign: 'center', py: 8 }}>
      <LockIcon sx={{ fontSize: 64, color: 'text.disabled' }} />
      <Typography variant="h5" mt={2}>
        Acesso não autorizado
      </Typography>
      <Typography color="text.secondary" mt={1}>
        Seu perfil não tem permissão para acessar esta página.
      </Typography>
      <Button sx={{ mt: 3 }} variant="contained" onClick={() => navigate('/')}>
        Voltar ao Dashboard
      </Button>
    </Box>
  )
}
