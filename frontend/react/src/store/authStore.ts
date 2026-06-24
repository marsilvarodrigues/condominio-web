import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import axios from 'axios'
import type { AuthUser } from '@/types'

// Raw axios, not the configured apiClient — bootstrap runs before any token exists,
// and apiClient's request interceptor would otherwise need the very token we're fetching.
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api'

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
  /**
   * Re-acquires an accessToken from the persisted refreshToken on app startup.
   * accessToken is intentionally memory-only (not persisted) so every fresh page load
   * starts with it null — without this, ProtectedRoute sees "not authenticated" and
   * redirects to /login before any request ever gets a chance to use the refreshToken.
   */
  bootstrap: () => Promise<void>
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

      bootstrap: async () => {
        const { accessToken, refreshToken } = get()
        if (accessToken || !refreshToken) return

        try {
          const { data } = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken })
          set({ accessToken: data.accessToken, refreshToken: data.refreshToken })
        } catch {
          set({ accessToken: null, refreshToken: null, user: null, activeCondominioId: null })
        }
      },
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
