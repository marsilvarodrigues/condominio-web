import { Component, type ReactNode } from 'react'
import { Box, Button, Typography } from '@mui/material'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  message: string
}

/**
 * Catches unhandled render errors and shows a recovery UI instead of a blank screen.
 * Does NOT redirect to login — network errors are handled by the axios interceptors.
 */
export class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: '' }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message }
  }

  componentDidCatch(error: Error) {
    console.error('[AppErrorBoundary]', error)
  }

  private recover = () => this.setState({ hasError: false, message: '' })

  render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '60vh',
            gap: 2,
            px: 4,
            textAlign: 'center',
          }}
        >
          <ErrorOutlineIcon sx={{ fontSize: 48, color: 'error.main' }} />
          <Typography variant="h6">Algo deu errado</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 400 }}>
            {this.state.message || 'Ocorreu um erro inesperado. Tente novamente.'}
          </Typography>
          <Button variant="contained" onClick={this.recover}>
            Tentar novamente
          </Button>
        </Box>
      )
    }
    return this.props.children
  }
}
