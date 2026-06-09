import {
  AppBar,
  Avatar,
  Box,
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
import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth.api'
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

  const multiCondominio = (user?.condominioIds.length ?? 0) > 1

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

        {multiCondominio && (
          <Select
            value={activeCondominioId ?? ''}
            onChange={(e) => setActiveCondominioId(Number(e.target.value))}
            size="small"
            sx={{
              mr: 2,
              color: 'white',
              '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.4)' },
              '.MuiSvgIcon-root': { color: 'white' },
              minWidth: 160,
            }}
          >
            {user?.condominioIds.map((id) => (
              <MenuItem key={id} value={id}>
                Condomínio {id}
              </MenuItem>
            ))}
          </Select>
        )}

        <Tooltip title={user?.email ?? ''}>
          <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'rgba(255,255,255,0.2)', fontSize: 14 }}>
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
