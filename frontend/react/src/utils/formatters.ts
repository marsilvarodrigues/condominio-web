const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const pct = new Intl.NumberFormat('pt-BR', { style: 'percent', minimumFractionDigits: 2 })
const num = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const formatCurrency = (value: number): string => brl.format(value)

export const formatPercent = (value: number): string => pct.format(value / 100)

export const formatNumber = (value: number, decimals = 2): string =>
  new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value)

export const formatDate = (iso: string): string =>
  new Date(iso).toLocaleDateString('pt-BR', { timeZone: 'UTC' })

export const formatDateTime = (iso: string): string =>
  new Date(iso).toLocaleString('pt-BR')

export const formatCnpj = (cnpj: string): string =>
  cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')

export const formatCep = (cep: string): string =>
  cep.replace(/^(\d{5})(\d{3})$/, '$1-$2')

export const parseCurrency = (value: string): number => {
  const clean = value.replace(/[R$\s.]/g, '').replace(',', '.')
  return parseFloat(clean) || 0
}
