import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { authApi } from '@/api/auth.api'
import { useAuthStore } from '@/store/authStore'
import type { LoginRequest } from '@/types'
import { jwtDecode } from 'jwt-decode'

interface JwtPayload {
  sub: string
  roles: string[]
  condominio_ids: number[]
  exp: number
}

export function useLogin() {
  const { setTokens, setUser } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (body: LoginRequest) => authApi.login(body),
    onSuccess: (data) => {
      setTokens(data.accessToken, data.refreshToken)
      try {
        const payload = jwtDecode<JwtPayload>(data.accessToken)
        setUser({
          email: payload.sub,
          roles: payload.roles,
          condominioIds: payload.condominio_ids ?? [],
        })
      } catch {
        // payload inválido — redireciona para login
        navigate('/login')
        return
      }
      navigate('/')
    },
  })
}

export function useChangePassword(userId: number) {
  return useMutation({
    mutationFn: (body: { currentPassword: string; newPassword: string; confirmPassword: string }) =>
      authApi.changePassword(userId, body),
  })
}
