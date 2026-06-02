import { createTheme } from '@mui/material/styles'
import { ptBR } from '@mui/material/locale'

export const theme = createTheme(
  {
    palette: {
      primary: {
        light: '#42A5F5',
        main: '#1565C0',
        dark: '#0D47A1',
        contrastText: '#FFFFFF',
      },
      secondary: {
        light: '#CE93D8',
        main: '#7B1FA2',
        dark: '#4A148C',
        contrastText: '#FFFFFF',
      },
      success: { main: '#2E7D32', light: '#66BB6A', dark: '#1B5E20' },
      warning: { main: '#E65100', light: '#FFA726', dark: '#BF360C' },
      error: { main: '#C62828', light: '#EF5350', dark: '#B71C1C' },
      background: { default: '#F5F7FA', paper: '#FFFFFF' },
      text: { primary: '#1A237E', secondary: '#546E7A' },
    },
    typography: {
      fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
      h4: { fontWeight: 700, color: '#1A237E' },
      h5: { fontWeight: 600, color: '#1A237E' },
      h6: { fontWeight: 600, color: '#1A237E' },
      subtitle1: { color: '#546E7A' },
      subtitle2: { color: '#546E7A', fontWeight: 600 },
    },
    shape: { borderRadius: 8 },
    components: {
      MuiButton: {
        styleOverrides: {
          root: { textTransform: 'none', fontWeight: 600, borderRadius: 8 },
          containedPrimary: {
            background: 'linear-gradient(135deg, #1565C0 0%, #0D47A1 100%)',
            '&:hover': { background: 'linear-gradient(135deg, #1976D2 0%, #1565C0 100%)' },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 12,
            boxShadow: '0 2px 12px rgba(21,101,192,0.08)',
            border: '1px solid rgba(21,101,192,0.08)',
          },
        },
      },
      MuiTableHead: {
        styleOverrides: {
          root: {
            '& .MuiTableCell-head': {
              backgroundColor: '#EEF2FF',
              fontWeight: 700,
              color: '#1A237E',
              fontSize: '0.75rem',
              textTransform: 'uppercase',
              letterSpacing: '0.05em',
            },
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { fontWeight: 600, fontSize: '0.75rem' },
        },
      },
      MuiTextField: {
        defaultProps: { variant: 'outlined', size: 'small' },
        styleOverrides: {
          root: {
            '& .MuiOutlinedInput-root': {
              borderRadius: 8,
              backgroundColor: '#F8FAFC',
              '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#1565C0' },
            },
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            background: 'linear-gradient(135deg, #1565C0 0%, #0D47A1 100%)',
            boxShadow: '0 2px 8px rgba(21,101,192,0.24)',
          },
        },
      },
    },
  },
  ptBR,
)
