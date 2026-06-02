import { Chip, type ChipProps } from '@mui/material'

type Severity = 'success' | 'warning' | 'error' | 'info' | 'default'

const SEVERITY_COLORS: Record<string, Severity> = {
  // Rateio status
  RATEADA: 'success',
  PENDENTE: 'warning',
  ERRO: 'error',
  // Execução status
  SUCESSO: 'success',
  PARCIAL: 'warning',
  // Conciliação
  CONCILIADO: 'success',
  DIVERGENTE: 'error',
  // Tipo execução
  AUTOMATICO: 'info',
  MANUAL: 'default',
  RECALCULO: 'warning',
  // Conta
  ATIVA: 'success',
  INATIVA: 'default',
}

const MUI_COLORS: Record<Severity, ChipProps['color']> = {
  success: 'success',
  warning: 'warning',
  error: 'error',
  info: 'info',
  default: 'default',
}

interface Props {
  status: string
  label?: string
  size?: ChipProps['size']
}

export function StatusChip({ status, label, size = 'small' }: Props) {
  const severity = SEVERITY_COLORS[status] ?? 'default'
  return (
    <Chip
      label={label ?? status}
      color={MUI_COLORS[severity]}
      size={size}
      variant="filled"
    />
  )
}
