import { Autocomplete, CircularProgress, TextField } from '@mui/material'
import type { SxProps, Theme } from '@mui/material'
import { useMemo, useState } from 'react'
import { useEstadosSearch } from '@/hooks/useEstados'
import type { EstadoDTO } from '@/types'

interface EstadoAutocompleteProps {
  /** Currently selected state, or null when empty. */
  value: EstadoDTO | null
  onChange: (estado: EstadoDTO | null) => void
  error?: boolean
  helperText?: string
  label?: string
  sx?: SxProps<Theme>
}

/**
 * Searchable autocomplete for Brazilian states.
 * Calls the API only when the user has typed at least 2 characters.
 * Accepts search by UF abbreviation (≤ 2 chars) or by state name (> 2 chars).
 */
export function EstadoAutocomplete({
  value,
  onChange,
  error,
  helperText,
  label = 'Estado',
  sx,
}: EstadoAutocompleteProps) {
  const [inputValue, setInputValue] = useState('')

  const { data: results = [], isFetching } = useEstadosSearch(inputValue)

  // Always keep the current value in the option list so the label renders
  // correctly on initial display (e.g. when opening an edit dialog).
  const options = useMemo<EstadoDTO[]>(() => {
    if (value && !results.find((e) => e.id === value.id)) {
      return [value, ...results]
    }
    return results
  }, [results, value])

  return (
    <Autocomplete<EstadoDTO>
      options={options}
      value={value}
      inputValue={inputValue}
      loading={isFetching}
      getOptionLabel={(opt) => `${opt.uf} – ${opt.nome}`}
      isOptionEqualToValue={(opt, val) => opt.id === val.id}
      noOptionsText={
        inputValue.trim().length < 2
          ? 'Digite ao menos 2 caracteres'
          : 'Nenhum estado encontrado'
      }
      onChange={(_, opt) => onChange(opt)}
      onInputChange={(_, newInput, reason) => {
        // Avoid re-triggering search when MUI resets the input after a selection.
        if (reason !== 'reset') setInputValue(newInput)
      }}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          error={error}
          helperText={helperText}
          slotProps={{
            input: {
              ...params.InputProps,
              endAdornment: (
                <>
                  {isFetching && <CircularProgress size={16} sx={{ mr: 1 }} />}
                  {params.InputProps.endAdornment}
                </>
              ),
            },
          }}
        />
      )}
      sx={sx}
    />
  )
}
