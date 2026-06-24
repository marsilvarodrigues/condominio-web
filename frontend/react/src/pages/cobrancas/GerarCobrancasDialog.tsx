import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Snackbar,
  TextField,
  Typography,
} from '@mui/material'
import CreditCardIcon from '@mui/icons-material/CreditCard'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { rateioApi } from '@/api/financeiro/rateio.api'
import { useGerarCobrancas } from '@/hooks/useCobrancas'
import { formatCurrency, formatDate } from '@/utils/formatters'

const schema = z.object({
  execucaoId: z.number({ required_error: 'Selecione uma execução' }),
  vencimento: z.string().min(1, 'Informe o vencimento'),
})
type FormValues = z.infer<typeof schema>

interface Props {
  open: boolean
  onClose: () => void
}

export function GerarCobrancasDialog({ open, onClose }: Props) {
  const [snackbar, setSnackbar] = useState(false)
  const [apiError, setApiError] = useState<string | null>(null)

  const today = new Date().toISOString().slice(0, 10)

  const { data: execucoesPage, isLoading: loadingExecucoes } = useQuery({
    queryKey: ['rateio', 'execucoes', 'dialog'],
    queryFn: () => rateioApi.listarExecucoes({ page: 0, size: 50 }),
    enabled: open,
  })

  const execucoes = (execucoesPage?.content ?? []).filter((e) => e.status === 'SUCESSO')

  const { mutate: gerar, isPending } = useGerarCobrancas()

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { vencimento: '' },
  })

  const handleClose = () => {
    reset()
    setApiError(null)
    onClose()
  }

  const onSubmit = (values: FormValues) => {
    setApiError(null)
    gerar(
      { execucaoId: values.execucaoId, vencimento: values.vencimento },
      {
        onSuccess: () => {
          setSnackbar(true)
          handleClose()
        },
        onError: (err: unknown) => {
          const msg = err instanceof Error ? err.message : 'Erro ao gerar cobranças.'
          setApiError(msg)
        },
      },
    )
  }

  return (
    <>
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          💳 Gerar Cobranças
          <Typography variant="body2" color="text.secondary" mt={0.5}>
            Boleto e QR Code Pix serão emitidos para cada unidade com morador
          </Typography>
        </DialogTitle>

        <DialogContent dividers>
          <Controller
            name="execucaoId"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                select
                label="Execução de Rateio *"
                fullWidth
                margin="normal"
                error={!!errors.execucaoId}
                helperText={errors.execucaoId?.message}
                disabled={loadingExecucoes}
                value={field.value ?? ''}
                onChange={(e) => field.onChange(Number(e.target.value))}
              >
                {loadingExecucoes ? (
                  <MenuItem disabled>
                    <CircularProgress size={16} sx={{ mr: 1 }} /> Carregando…
                  </MenuItem>
                ) : execucoes.length === 0 ? (
                  <MenuItem disabled>Nenhuma execução disponível</MenuItem>
                ) : (
                  execucoes.map((e) => (
                    <MenuItem key={e.id} value={e.id}>
                      Execução #{e.id} — {formatDate(e.dataExecucao)} —{' '}
                      {formatCurrency(e.despesaTotal)}
                    </MenuItem>
                  ))
                )}
              </TextField>
            )}
          />

          <Controller
            name="vencimento"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Data de Vencimento *"
                type="date"
                fullWidth
                margin="normal"
                inputProps={{ min: today }}
                error={!!errors.vencimento}
                helperText={
                  errors.vencimento?.message ??
                  'Boleto e Pix expiram nesta data. Juros de 1% a.m. + multa 2% após.'
                }
                InputLabelProps={{ shrink: true }}
              />
            )}
          />

          <Alert severity="warning" sx={{ mt: 2 }}>
            Ao confirmar, os e-mails serão enviados imediatamente para todos os moradores.
          </Alert>

          {apiError && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {apiError}
            </Alert>
          )}
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose} disabled={isPending}>
            Cancelar
          </Button>
          <Button
            variant="contained"
            startIcon={isPending ? <CircularProgress size={16} color="inherit" /> : <CreditCardIcon />}
            onClick={handleSubmit(onSubmit)}
            disabled={isPending}
          >
            💳 Gerar e Enviar
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar}
        autoHideDuration={4000}
        onClose={() => setSnackbar(false)}
        message="Cobranças geradas com sucesso"
      />
    </>
  )
}
