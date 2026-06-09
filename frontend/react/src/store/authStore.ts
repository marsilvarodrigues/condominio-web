import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
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
      // sessionStorage: scoped to the browser tab; cleared when the tab closes.
      // accessToken is NOT persisted (memory only) — the 401 interceptor in client.ts
      // will silently re-issue it from refreshToken on the next page load.
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
        activeCondominioId: state.activeCondominioId,
      }),
    },
  ),
)
