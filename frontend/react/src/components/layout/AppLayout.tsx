import { Box, Toolbar } from '@mui/material'
import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar, DRAWER_WIDTH } from './Sidebar'
import { TopBar } from './TopBar'

export function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <TopBar onMenuClick={() => setMobileOpen(true)} />
      <Sidebar mobileOpen={mobileOpen} onMobileClose={() => setMobileOpen(false)} />

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          marginLeft: { md: `${DRAWER_WIDTH}px` },
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Toolbar />
        <Box sx={{ flex: 1, p: { xs: 2, sm: 3 } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}
