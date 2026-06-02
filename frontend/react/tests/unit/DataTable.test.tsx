import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { DataTable } from '@/components/common/DataTable'
import type { Column } from '@/components/common/DataTable'

interface Item {
  id: number
  name: string
  value: number
}

const columns: Column<Item>[] = [
  { key: 'name', header: 'Name', render: (r) => r.name },
  { key: 'value', header: 'Value', align: 'right', render: (r) => String(r.value) },
]

const rows: Item[] = [
  { id: 1, name: 'Alpha', value: 100 },
  { id: 2, name: 'Beta', value: 200 },
]

describe('DataTable', () => {
  it('renders column headers', () => {
    render(<DataTable columns={columns} rows={rows} keyField="id" />)
    expect(screen.getByText('Name')).toBeInTheDocument()
    expect(screen.getByText('Value')).toBeInTheDocument()
  })

  it('renders all row data', () => {
    render(<DataTable columns={columns} rows={rows} keyField="id" />)
    expect(screen.getByText('Alpha')).toBeInTheDocument()
    expect(screen.getByText('Beta')).toBeInTheDocument()
    expect(screen.getByText('100')).toBeInTheDocument()
    expect(screen.getByText('200')).toBeInTheDocument()
  })

  it('shows empty message when no rows', () => {
    render(<DataTable columns={columns} rows={[]} keyField="id" emptyMessage="No data." />)
    expect(screen.getByText('No data.')).toBeInTheDocument()
  })

  it('shows loading indicator when loading=true', () => {
    const { container } = render(
      <DataTable columns={columns} rows={[]} keyField="id" loading />,
    )
    expect(container.querySelector('[role="progressbar"]')).toBeInTheDocument()
  })
})
