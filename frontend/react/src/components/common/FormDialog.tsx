import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  type SxProps,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'
import type { ReactNode } from 'react'

interface Props {
  open: boolean
  title: string
  children: ReactNode
  submitLabel?: string
  loading?: boolean
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg'
  formId?: string
  sx?: SxProps
  onClose: () => void
  onSubmit?: () => void
}

export function FormDialog({
  open,
  title,
  children,
  submitLabel = 'Salvar',
  loading = false,
  maxWidth = 'sm',
  formId,
  sx,
  onClose,
  onSubmit,
}: Props) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth={maxWidth} fullWidth PaperProps={{ sx }}>
      <DialogTitle
        sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
      >
        {title}
        <IconButton size="small" onClick={onClose} disabled={loading}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ pt: 2 }}>
        {children}
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} disabled={loading} variant="outlined">
          Cancelar
        </Button>
        <Button
          type={formId ? 'submit' : 'button'}
          form={formId}
          onClick={formId ? undefined : onSubmit}
          disabled={loading}
          variant="contained"
          startIcon={loading ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          {submitLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
