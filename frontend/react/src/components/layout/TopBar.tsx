import {
  AppBar,
  Avatar,
  Box,
  CircularProgress,
  IconButton,
  Menu,
  MenuItem,
  Select,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import LogoutIcon from '@mui/icons-material/Logout'
import LockIcon from '@mui/icons-material/Lock'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth.api'
import { condominiosApi } from '@/api/condominios.api'
import { ROLES, ROLE_PRECEDENCIA } from '@/utils/constants'
import { DRAWER_WIDTH } from './Sidebar'

const ROLE_LABELS: Record<string, string> = {
  [ROLES.ADMIN]:        'Administrador',
  [ROLES.SINDICO]:      'Síndico',
  [ROLES.PROPRIETARIO]: 'Proprietário',
  [ROLES.MORADOR]:      'Morador',
  [ROLES.USER]:         'Usuário',
}

interface Props {
  onMenuClick: () => void
}

export function TopBar({ onMenuClick }: Props) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user, activeCondominioId, setActiveCondominioId, logout } = useAuthStore()
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } finally {
      logout()
      navigate('/login')
    }
  }

  const condominioCount = user?.condominioIds.length ?? 0
  // Show selector when user has no fixed condominio (global admin = 0) or manages multiple (> 1).
  // Users with exactly 1 condominio are auto-selected at login — no selector needed.
  const needsSelector = condominioCount !== 1

  const { data: allCondominios = [], isLoading } = useQuery({
    queryKey: ['condominios', 'selector'],
    queryFn: condominiosApi.listForSelector,
    enabled: needsSelector,
    staleTime: 5 * 60 * 1000,
  })

  // Global admin (0 condominios) → all from API.
  // Multi-condominio user → restrict to their assigned ids.
  const selectableCondominios =
    condominioCount > 1
      ? allCondominios.filter((c) => user!.condominioIds.includes(c.id))
      : allCondominios

  const handleCondominioChange = (val: string | number) => {
    const newId = val === '' ? null : Number(val)
    setActiveCondominioId(newId)
    // Invalidate everything so every mounted component re-fetches with the new X-Condominio-Id.
    queryClient.invalidateQueries()
  }

  return (
    <AppBar
      position="fixed"
      sx={{ ml: { md: `${DRAWER_WIDTH}px` }, width: { md: `calc(100% - ${DRAWER_WIDTH}px)` } }}
    >
      <Toolbar>
        <IconButton
          color="inherit"
          edge="start"
          onClick={onMenuClick}
          sx={{ mr: 2, display: { md: 'none' } }}
        >
          <MenuIcon />
        </IconButton>

        <Box sx={{ flex: 1 }} />

        {needsSelector && (
          <Select
            value={activeCondominioId ?? ''}
            displayEmpty
            onChange={(e) => handleCondominioChange(e.target.value)}
            disabled={isLoading}
            size="small"
            renderValue={(val) => {
              if (isLoading)
                return (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <CircularProgress size={12} sx={{ color: 'rgba(255,255,255,0.7)' }} />
                    Carregando...
                  </Box>
                )
              if (!val) return 'Todos os condomínios'
              const found = selectableCondominios.find((c) => c.id === Number(val))
              return found?.nome ?? `Condomínio ${val}`
            }}
            sx={{
              mr: 2,
              color: 'white',
              '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.4)' },
              '&:hover .MuiOutlinedInput-notchedOutline': {
                borderColor: 'rgba(255,255,255,0.7)',
              },
              '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: 'white' },
              '.MuiSvgIcon-root': { color: 'white' },
              minWidth: 200,
            }}
          >
            {isLoading ? (
              <MenuItem disabled>
                <CircularProgress size={14} sx={{ mr: 1 }} />
                Carregando...
              </MenuItem>
            ) : (
              [
                <MenuItem key="__all__" value="">
                  Todos os condomínios
                </MenuItem>,
                ...selectableCondominios.map((c) => (
                  <MenuItem key={c.id} value={c.id}>
                    {c.nome}
                  </MenuItem>
                )),
              ]
            )}
          </Select>
        )}

        <Tooltip title={user?.email ?? ''}>
          <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)}>
            <Avatar
              sx={{ width: 32, height: 32, bgcolor: 'rgba(255,255,255,0.2)', fontSize: 14 }}
            >
              {user?.email?.[0]?.toUpperCase() ?? <AccountCircleIcon />}
            </Avatar>
          </IconButton>
        </Tooltip>

        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={() => setAnchorEl(null)}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        >
          <Box sx={{ px: 2, py: 1, minWidth: 180 }}>
            <Typography variant="subtitle2">{user?.email}</Typography>
            <Typography variant="caption" color="text.secondary">
              {ROLE_LABELS[
                ROLE_PRECEDENCIA.find((r) => user?.roles.includes(r)) ?? ROLES.USER
              ] ?? 'Usuário'}
            </Typography>
          </Box>
          <MenuItem
            onClick={() => {
              setAnchorEl(null)
              navigate('/alterar-senha')
            }}
          >
            <LockIcon fontSize="small" sx={{ mr: 1 }} />
            Alterar senha
          </MenuItem>
          <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
            <LogoutIcon fontSize="small" sx={{ mr: 1 }} />
            Sair
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  )
}
