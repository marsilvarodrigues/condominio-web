import { describe, it, expect } from 'vitest'
import {
  formatCurrency,
  formatPercent,
  formatCnpj,
  formatCep,
  formatDate,
  formatDateTime,
} from '@/utils/formatters'

describe('formatCurrency', () => {
  it('formats positive values in BRL', () => {
    expect(formatCurrency(1234.56)).toMatch(/1\.234,56/)
  })

  it('formats zero', () => {
    expect(formatCurrency(0)).toMatch(/0,00/)
  })

  it('formats negative values', () => {
    expect(formatCurrency(-500)).toMatch(/500,00/)
  })
})

describe('formatPercent', () => {
  it('formats percentage', () => {
    expect(formatPercent(42.5)).toMatch(/42,5/)
  })

  it('formats zero percent', () => {
    expect(formatPercent(0)).toMatch(/0/)
  })
})

describe('formatCnpj', () => {
  it('formats 14-digit CNPJ', () => {
    expect(formatCnpj('12345678000195')).toBe('12.345.678/0001-95')
  })

  it('returns raw value if not 14 digits', () => {
    expect(formatCnpj('123')).toBe('123')
  })
})

describe('formatCep', () => {
  it('formats 8-digit CEP', () => {
    expect(formatCep('01310100')).toBe('01310-100')
  })

  it('returns raw value if not 8 digits', () => {
    expect(formatCep('013')).toBe('013')
  })
})

describe('formatDate', () => {
  it('formats ISO date string', () => {
    const result = formatDate('2026-06-02')
    expect(result).toMatch(/02\/06\/2026/)
  })
})

describe('formatDateTime', () => {
  it('formats ISO datetime string', () => {
    const result = formatDateTime('2026-06-02T10:30:00')
    expect(result).toMatch(/02\/06\/2026/)
    expect(result).toMatch(/10:30/)
  })
})
