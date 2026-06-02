import {
  Alert,
  Box,
  Button,
  CircularProgress,
  IconButton,
  InputAdornment,
  Link,
  Paper,
  TextField,
  Typography,
} from '@mui/material'
import ApartmentIcon from '@mui/icons-material/Apartment'
import VisibilityIcon from '@mui/icons-material/Visibility'
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useLogin } from '@/hooks/useAuth'
import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

const schema = z.object({
  email: z.string().email('E-mail inválido'),
  password: z.string().min(1, 'Senha obrigatória'),
})
type FormValues = z.infer<typeof schema>

export default function LoginPage() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())
  const [showPassword, setShowPassword] = useState(false)
  const login = useLogin()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  if (isAuthenticated) return <Navigate to="/" replace />

  const onSubmit = (values: FormValues) => login.mutate(values)

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #1565C0 0%, #0D47A1 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
        position: 'relative',
        overflow: 'hidden',
        '&::before, &::after': {
          content: '""',
          position: 'absolute',
          borderRadius: '50%',
          background: 'rgba(255,255,255,0.04)',
        },
        '&::before': { width: 400, height: 400, top: -100, left: -100 },
        '&::after': { width: 560, height: 560, bottom: -200, right: -150 },
      }}
    >
      <Paper
        elevation={0}
        sx={{
          width: '100%',
          maxWidth: 440,
          p: 4,
          borderRadius: 3,
          position: 'relative',
          zIndex: 1,
          boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
          '&::before': {
            content: '""',
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            height: 4,
            background: 'linear-gradient(90deg, #1565C0, #42A5F5)',
            borderRadius: '12px 12px 0 0',
          },
        }}
      >
        {/* Logo */}
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}>
          <Box
            sx={{
              width: 72,
              height: 72,
              borderRadius: 2.5,
              bgcolor: '#1565C0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              mb: 2,
              boxShadow: '0 4px 14px rgba(21,101,192,0.4)',
            }}
          >
            <ApartmentIcon sx={{ color: 'white', fontSize: 36 }} />
          </Box>
          <Typography variant="h5" fontWeight={700} color="#1A237E">
            CondoGest
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Sistema de Gestão de Condomínios
          </Typography>
        </Box>

        <Box sx={{ my: 2, borderBottom: '1px solid', borderColor: 'divider' }} />

        {/* Erro de autenticação */}
        {login.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            E-mail ou senha inválidos.
          </Alert>
        )}

        {/* Formulário */}
        <Box
          component="form"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="E-mail"
            type="email"
            autoComplete="email"
            autoFocus
            fullWidth
            {...register('email')}
            error={!!errors.email}
            helperText={errors.email?.message}
          />

          <TextField
            label="Senha"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            fullWidth
            {...register('password')}
            error={!!errors.password}
            helperText={errors.password?.message}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton size="small" onClick={() => setShowPassword((v) => !v)} edge="end">
                    {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          <Box sx={{ textAlign: 'right' }}>
            <Link href="#" variant="caption" color="primary" underline="hover">
              Esqueci minha senha
            </Link>
          </Box>

          <Button
            type="submit"
            variant="contained"
            fullWidth
            size="large"
            disabled={login.isPending}
            startIcon={login.isPending ? <CircularProgress size={18} color="inherit" /> : undefined}
            sx={{ mt: 1, py: 1.5, fontWeight: 700, letterSpacing: 1 }}
          >
            ENTRAR
          </Button>
        </Box>

        <Typography
          variant="caption"
          color="text.secondary"
          display="block"
          textAlign="center"
          mt={3}
        >
          © 2025 CondoGest · API v1 · Acesso seguro com JWT
        </Typography>
      </Paper>
    </Box>
  )
}
