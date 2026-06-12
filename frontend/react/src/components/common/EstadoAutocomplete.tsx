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
  // searchInput tracks what the user typed for API queries only.
  // We intentionally do NOT pass this as inputValue to Autocomplete so that
  // MUI can manage the displayed text itself — otherwise, when the form is
  // reset with an existing estado (edit mode), the field stays blank because
  // the 'reset' reason from onInputChange was being suppressed.
  const [searchInput, setSearchInput] = useState('')

  const { data: results = [], isFetching } = useEstadosSearch(searchInput)

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
      loading={isFetching}
      getOptionLabel={(opt) => `${opt.uf} – ${opt.nome}`}
      isOptionEqualToValue={(opt, val) => opt.id === val.id}
      noOptionsText={
        searchInput.trim().length < 2
          ? 'Digite ao menos 2 caracteres'
          : 'Nenhum estado encontrado'
      }
      onChange={(_, opt) => onChange(opt)}
      onInputChange={(_, newInput, reason) => {
        // Only update the search term when the user is actually typing.
        // On 'reset' (option selected / value changed externally) or 'clear',
        // reset the search so stale results don't linger in the dropdown.
        if (reason === 'input') {
          setSearchInput(newInput)
        } else {
          setSearchInput('')
        }
      }}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          error={error}
          helperText={helperText}
          InputProps={{
            ...params.InputProps,
            endAdornment: (
              <>
                {isFetching && <CircularProgress size={16} sx={{ mr: 1 }} />}
                {params.InputProps.endAdornment}
              </>
            ),
          }}
        />
      )}
      sx={sx}
    />
  )
}
