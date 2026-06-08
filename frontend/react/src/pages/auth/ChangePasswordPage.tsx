import { Alert, Box, Button, CircularProgress, Paper, TextField } from '@mui/material'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { PageHeader } from '@/components/common'
import { useChangePassword } from '@/hooks/useAuth'
import { useAuthStore } from '@/store/authStore'
import { useNavigate } from 'react-router-dom'

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Senha atual obrigatória'),
    newPassword: z
      .string()
      .min(8, 'Mínimo 8 caracteres')
      .regex(/[A-Z]/, 'Deve conter maiúscula')
      .regex(/[0-9]/, 'Deve conter número')
      .regex(/[^A-Za-z0-9]/, 'Deve conter caractere especial'),
    confirmPassword: z.string().min(1, 'Confirmação obrigatória'),
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    message: 'Senhas não conferem',
    path: ['confirmPassword'],
  })

type FormValues = z.infer<typeof schema>

export default function ChangePasswordPage() {
  const navigate = useNavigate()
  const userId = useAuthStore((s) => s.user?.condominioIds[0] ?? 0)
  const mutation = useChangePassword(userId)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const onSubmit = (values: FormValues) =>
    mutation.mutate(values, {
      onSuccess: () => {
        reset()
        navigate('/')
      },
    })

  return (
    <Box maxWidth={480}>
      <PageHeader title="Alterar Senha" subtitle="Defina uma nova senha de acesso." />
      <Paper sx={{ p: 3 }}>
        {mutation.isSuccess && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Senha alterada com sucesso!
          </Alert>
        )}
        {mutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Senha atual inválida ou erro ao processar.
          </Alert>
        )}
        <Box
          component="form"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            label="Senha atual"
            type="password"
            fullWidth
            {...register('currentPassword')}
            error={!!errors.currentPassword}
            helperText={errors.currentPassword?.message}
          />
          <TextField
            label="Nova senha"
            type="password"
            fullWidth
            {...register('newPassword')}
            error={!!errors.newPassword}
            helperText={errors.newPassword?.message}
          />
          <TextField
            label="Confirmar nova senha"
            type="password"
            fullWidth
            {...register('confirmPassword')}
            error={!!errors.confirmPassword}
            helperText={errors.confirmPassword?.message}
          />
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 1 }}>
            <Button variant="outlined" onClick={() => navigate(-1)}>
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={mutation.isPending}
              startIcon={
                mutation.isPending ? <CircularProgress size={16} color="inherit" /> : undefined
              }
            >
              Salvar
            </Button>
          </Box>
        </Box>
      </Paper>
    </Box>
  )
}
