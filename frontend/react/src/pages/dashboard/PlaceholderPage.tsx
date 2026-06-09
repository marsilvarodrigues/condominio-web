import { Box, Button, Card, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'

interface Props {
  titulo: string
}

export default function PlaceholderPage({ titulo }: Props) {
  const navigate = useNavigate()

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <Card sx={{ maxWidth: 440, textAlign: 'center', p: 4 }}>
        <Typography fontSize={48}>🚧</Typography>
        <Typography variant="h5" fontWeight={700} mt={2} gutterBottom>
          {titulo}
        </Typography>
        <Typography variant="body2" color="text.secondary" mb={3}>
          Esta seção estará disponível em breve.
        </Typography>
        <Button variant="outlined" onClick={() => navigate('/')}>
          ← Voltar ao Dashboard
        </Button>
      </Card>
    </Box>
  )
}
