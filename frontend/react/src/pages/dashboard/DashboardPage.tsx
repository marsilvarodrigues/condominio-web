import { lazy, Suspense } from 'react'
import { Box, Grid, Skeleton } from '@mui/material'
import { useDashboardPerfil } from '@/hooks/useDashboardPerfil'

const DashboardAdmin        = lazy(() => import('./DashboardAdmin'))
const DashboardSindico      = lazy(() => import('./DashboardSindico'))
const DashboardProprietario = lazy(() => import('./DashboardProprietario'))
const DashboardMorador      = lazy(() => import('./DashboardMorador'))
const DashboardUsuario      = lazy(() => import('./DashboardUsuario'))
const ProfileSwitcher       = lazy(() => import('./ProfileSwitcher'))

function DashboardSkeleton() {
  return (
    <Grid container spacing={3}>
      {[1, 2, 3, 4].map((i) => (
        <Grid item xs={12} sm={6} md={3} key={i}>
          <Skeleton variant="rounded" height={120} />
        </Grid>
      ))}
      <Grid item xs={12} md={8}>
        <Skeleton variant="rounded" height={240} />
      </Grid>
      <Grid item xs={12} md={4}>
        <Skeleton variant="rounded" height={240} />
      </Grid>
    </Grid>
  )
}

export default function DashboardPage() {
  const { perfil, podeAlternar, perfisDisponiveis, setPerfil } = useDashboardPerfil()

  return (
    <Box>
      <Suspense fallback={<Skeleton height={48} sx={{ mb: 2 }} />}>
        {podeAlternar && (
          <ProfileSwitcher
            perfisDisponiveis={perfisDisponiveis}
            perfilAtivo={perfil}
            onChange={setPerfil}
          />
        )}
      </Suspense>
      <Suspense fallback={<DashboardSkeleton />}>
        {perfil === 'admin'        && <DashboardAdmin />}
        {perfil === 'sindico'      && <DashboardSindico />}
        {perfil === 'proprietario' && <DashboardProprietario />}
        {perfil === 'morador'      && <DashboardMorador />}
        {perfil === 'usuario'      && <DashboardUsuario />}
      </Suspense>
    </Box>
  )
}
