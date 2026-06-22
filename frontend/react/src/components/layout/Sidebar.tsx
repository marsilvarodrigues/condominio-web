import {
  Box,
  Collapse,
  Divider,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import ApartmentIcon from '@mui/icons-material/Apartment'
import PeopleIcon from '@mui/icons-material/People'
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet'
import AccountBalanceIcon from '@mui/icons-material/AccountBalance'
import CalculateIcon from '@mui/icons-material/Calculate'
import ExpandLess from '@mui/icons-material/ExpandLess'
import ExpandMore from '@mui/icons-material/ExpandMore'
import LocationCityIcon from '@mui/icons-material/LocationCity'
import NotificationsIcon from '@mui/icons-material/Notifications'
import EventIcon from '@mui/icons-material/Event'
import BusinessIcon from '@mui/icons-material/Business'
import CreditCardIcon from '@mui/icons-material/CreditCard'
import AssignmentIcon from '@mui/icons-material/Assignment'
import { useLocation, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { ROLES } from '@/utils/constants'

export const DRAWER_WIDTH = 240

interface NavItem {
  label: string
  icon: React.ReactNode
  path?: string
  roles: string[]
  children?: NavItem[]
}

const ALL_ROLES = [ROLES.ADMIN, ROLES.SINDICO, ROLES.PROPRIETARIO, ROLES.MORADOR, ROLES.USER]
const ADMIN_SINDICO = [ROLES.ADMIN, ROLES.SINDICO]
const HIERARQUIA_ROLES = [ROLES.ADMIN, ROLES.SINDICO, ROLES.PROPRIETARIO, ROLES.MORADOR]

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', icon: <DashboardIcon />, path: '/', roles: ALL_ROLES },

  { label: 'Condomínios', icon: <LocationCityIcon />, path: '/condominios',  roles: ADMIN_SINDICO },
  { label: 'Hierarquia',  icon: <ApartmentIcon />,    path: '/hierarquia',   roles: HIERARQUIA_ROLES },
  { label: 'Usuários',    icon: <PeopleIcon />,        path: '/usuarios',     roles: [ROLES.ADMIN] },

  {
    label: 'Financeiro',
    icon: <AccountBalanceWalletIcon />,
    roles: ADMIN_SINDICO,
    children: [
      { label: 'Plano de Contas',  icon: <AccountBalanceWalletIcon />, path: '/financeiro/plano-contas',  roles: ADMIN_SINDICO },
      { label: 'Orçamento Anual',  icon: <AccountBalanceWalletIcon />, path: '/financeiro/orcamento',     roles: ADMIN_SINDICO },
      { label: 'Fundo de Reserva', icon: <AccountBalanceWalletIcon />, path: '/financeiro/fundo-reserva', roles: ADMIN_SINDICO },
    ],
  },
  {
    label: 'Bancos',
    icon: <AccountBalanceIcon />,
    roles: ADMIN_SINDICO,
    children: [
      { label: 'Bancos',           icon: <AccountBalanceIcon />, path: '/financeiro/bancos',      roles: ADMIN_SINDICO },
      { label: 'Contas Bancárias', icon: <AccountBalanceIcon />, path: '/financeiro/contas',       roles: ADMIN_SINDICO },
      { label: 'Conciliação',      icon: <AccountBalanceIcon />, path: '/financeiro/conciliacao',  roles: ADMIN_SINDICO },
    ],
  },
  { label: 'Cobranças', icon: <CreditCardIcon />, path: '/cobrancas', roles: ADMIN_SINDICO },
  {
    label: 'Relatórios',
    icon: <AssignmentIcon />,
    roles: ADMIN_SINDICO,
    children: [
      { label: 'Prestação de Contas', icon: <AssignmentIcon />, path: '/relatorio/prestacao-contas', roles: ADMIN_SINDICO },
    ],
  },
  {
    label: 'Rateio',
    icon: <CalculateIcon />,
    roles: ADMIN_SINDICO,
    children: [
      { label: 'Grupos de Despesa', icon: <CalculateIcon />, path: '/rateio/grupos',       roles: ADMIN_SINDICO },
      { label: 'Coeficientes',      icon: <CalculateIcon />, path: '/rateio/coeficientes', roles: ADMIN_SINDICO },
      { label: 'Simulação',         icon: <CalculateIcon />, path: '/rateio/simulacao',    roles: ADMIN_SINDICO },
      { label: 'Execuções',         icon: <CalculateIcon />, path: '/rateio/execucoes',    roles: ADMIN_SINDICO },
    ],
  },

  { label: 'Comunicados', icon: <NotificationsIcon />, path: '/comunicados',                 roles: [ROLES.MORADOR] },
  { label: 'Reservas',    icon: <EventIcon />,          path: '/reservas',                    roles: [ROLES.MORADOR] },
  { label: 'Meus Imóveis', icon: <BusinessIcon />,      path: '/proprietario/meus-imoveis',  roles: [ROLES.PROPRIETARIO] },
]

interface Props {
  mobileOpen: boolean
  onMobileClose: () => void
}

export function Sidebar({ mobileOpen, onMobileClose }: Props) {
  const location = useLocation()
  const navigate = useNavigate()
  const hasRole = useAuthStore((s) => s.hasRole)
  const [expanded, setExpanded] = useState<string | null>(null)

  const toggle = (label: string) =>
    setExpanded((prev) => (prev === label ? null : label))

  const isActive = (path?: string) =>
    path ? location.pathname === path || location.pathname.startsWith(path + '/') : false

  const renderItem = (item: NavItem, depth = 0) => {
    if (!item.roles.some((r) => hasRole(r))) return null

    if (item.children) {
      const visibleChildren = item.children.filter((c) => c.roles.some((r) => hasRole(r)))
      if (visibleChildren.length === 0) return null
      const anyChildActive = visibleChildren.some((c) => isActive(c.path))
      return (
        <Box key={item.label}>
          <ListItemButton
            onClick={() => toggle(item.label)}
            sx={{ pl: 2 + depth * 2, borderRadius: 1 }}
            selected={anyChildActive}
          >
            <ListItemIcon sx={{ minWidth: 36, color: anyChildActive ? 'primary.main' : 'inherit' }}>
              {item.icon}
            </ListItemIcon>
            <ListItemText
              primary={item.label}
              primaryTypographyProps={{ fontSize: 14, fontWeight: anyChildActive ? 600 : 400 }}
            />
            {expanded === item.label ? <ExpandLess /> : <ExpandMore />}
          </ListItemButton>
          <Collapse in={expanded === item.label || anyChildActive} unmountOnExit>
            <List disablePadding>
              {visibleChildren.map((child) => renderItem(child, depth + 1))}
            </List>
          </Collapse>
        </Box>
      )
    }

    const active = isActive(item.path)
    return (
      <ListItemButton
        key={item.label}
        selected={active}
        onClick={() => {
          navigate(item.path!)
          onMobileClose()
        }}
        sx={{
          pl: 2 + depth * 2,
          borderRadius: 1,
          mx: 1,
          mb: 0.5,
          '&.Mui-selected': {
            backgroundColor: 'rgba(21,101,192,0.12)',
            color: 'primary.main',
            '& .MuiListItemIcon-root': { color: 'primary.main' },
          },
        }}
      >
        <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
        <ListItemText
          primary={item.label}
          primaryTypographyProps={{ fontSize: 14, fontWeight: active ? 600 : 400 }}
        />
      </ListItemButton>
    )
  }

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ background: 'linear-gradient(135deg, #1565C0 0%, #0D47A1 100%)' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <ApartmentIcon sx={{ color: 'white', fontSize: 28 }} />
          <Typography variant="h6" sx={{ color: 'white', fontWeight: 700, letterSpacing: '-0.5px' }}>
            CondoGest
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <Box sx={{ flex: 1, overflowY: 'auto', py: 1 }}>
        <List disablePadding>{NAV_ITEMS.map((item) => renderItem(item))}</List>
      </Box>
      <Divider />
      <Box sx={{ p: 2 }}>
        <Typography variant="caption" color="text.secondary">
          CondoGest v1.0
        </Typography>
      </Box>
    </Box>
  )

  return (
    <>
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onMobileClose}
        ModalProps={{ keepMounted: true }}
        sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}
      >
        {drawerContent}
      </Drawer>
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
        open
      >
        {drawerContent}
      </Drawer>
    </>
  )
}
