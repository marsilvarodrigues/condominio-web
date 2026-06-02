import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthUser } from '@/types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: AuthUser | null
  activeCondominioId: number | null

  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (user: AuthUser) => void
  setActiveCondominioId: (id: number | null) => void
  logout: () => void
  isAuthenticated: () => boolean
  hasRole: (role: string) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      activeCondominioId: null,

      setTokens: (accessToken, refreshToken) => set({ accessToken, refreshToken }),

      setUser: (user) => {
        const activeCondominioId =
          user.condominioIds.length === 1 ? user.condominioIds[0] : null
        set({ user, activeCondominioId })
      },

      setActiveCondominioId: (id) => set({ activeCondominioId: id }),

      logout: () =>
        set({ accessToken: null, refreshToken: null, user: null, activeCondominioId: null }),

      isAuthenticated: () => !!get().accessToken,

      hasRole: (role) => get().user?.roles.includes(role) ?? false,
    }),
    {
      name: 'condogest-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        activeCondominioId: state.activeCondominioId,
      }),
    },
  ),
)
