import {
  Box,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
  type SxProps,
} from '@mui/material'
import InboxIcon from '@mui/icons-material/Inbox'
import type { ReactNode } from 'react'

export interface Column<T> {
  key: string
  header: string
  width?: number | string
  align?: 'left' | 'center' | 'right'
  render: (row: T) => ReactNode
}

interface Props<T> {
  columns: Column<T>[]
  rows: T[]
  keyField: keyof T
  loading?: boolean
  emptyMessage?: string
  page?: number
  pageSize?: number
  totalCount?: number
  sx?: SxProps
  onPageChange?: (page: number) => void
  onPageSizeChange?: (size: number) => void
  onRowClick?: (row: T) => void
}

export function DataTable<T>({
  columns,
  rows,
  keyField,
  loading = false,
  emptyMessage = 'Nenhum registro encontrado.',
  page = 0,
  pageSize = 20,
  totalCount,
  sx,
  onPageChange,
  onPageSizeChange,
  onRowClick,
}: Props<T>) {
  return (
    <Paper
      variant="outlined"
      sx={{ borderRadius: 2, overflow: 'hidden', border: '1px solid rgba(21,101,192,0.1)', ...sx }}
    >
      <TableContainer>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell
                  key={col.key}
                  align={col.align ?? 'left'}
                  width={col.width}
                >
                  {col.header}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>

          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={columns.length} sx={{ py: 6, textAlign: 'center' }}>
                  <CircularProgress size={32} />
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length}>
                  <Box
                    sx={{
                      py: 6,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 1,
                      color: 'text.secondary',
                    }}
                  >
                    <InboxIcon sx={{ fontSize: 40, opacity: 0.4 }} />
                    <Typography variant="body2">{emptyMessage}</Typography>
                  </Box>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow
                  key={String(row[keyField])}
                  hover={!!onRowClick}
                  onClick={() => onRowClick?.(row)}
                  sx={onRowClick ? { cursor: 'pointer' } : undefined}
                >
                  {columns.map((col) => (
                    <TableCell key={col.key} align={col.align ?? 'left'}>
                      {col.render(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {totalCount !== undefined && onPageChange && (
        <TablePagination
          component="div"
          count={totalCount}
          page={page}
          rowsPerPage={pageSize}
          rowsPerPageOptions={[10, 20, 50]}
          labelRowsPerPage="Linhas por página:"
          labelDisplayedRows={({ from, to, count }) => `${from}–${to} de ${count}`}
          onPageChange={(_, p) => onPageChange(p)}
          onRowsPerPageChange={(e) => onPageSizeChange?.(Number(e.target.value))}
        />
      )}
    </Paper>
  )
}
