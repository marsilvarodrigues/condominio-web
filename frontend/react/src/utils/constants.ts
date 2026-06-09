export const TIPO_RATEIO_LABELS: Record<string, string> = {
  IGUALITARIO: 'Igualitário',
  FRACAO_IDEAL: 'Fração Ideal',
  METRAGEM: 'Metragem',
  CONSUMO: 'Consumo (m³)',
}

export const ESCOPO_RATEIO_LABELS: Record<string, string> = {
  TODOS: 'Todos os apartamentos',
  BLOCO: 'Por bloco',
}

export const TIPO_PLANO_CONTA_LABELS: Record<string, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
  TRANSFERENCIA: 'Transferência',
}

export const TIPO_CONTA_LABELS: Record<string, string> = {
  CORRENTE: 'Corrente',
  POUPANCA: 'Poupança',
  INVESTIMENTO: 'Investimento',
}

export const TIPO_MOVIMENTACAO_LABELS: Record<string, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saída',
  RENDIMENTO: 'Rendimento',
  APLICACAO: 'Aplicação',
  RESGATE: 'Resgate',
}

export const STATUS_RATEIO_LABELS: Record<string, string> = {
  PENDENTE: 'Pendente',
  RATEADA: 'Rateada',
  ERRO: 'Erro',
}

export const STATUS_EXECUCAO_LABELS: Record<string, string> = {
  SUCESSO: 'Sucesso',
  ERRO: 'Erro',
  PARCIAL: 'Parcial',
}

export const TIPO_EXECUCAO_LABELS: Record<string, string> = {
  AUTOMATICO: 'Automático',
  MANUAL: 'Manual',
  RECALCULO: 'Recálculo',
}

export const STATUS_CONCILIACAO_LABELS: Record<string, string> = {
  PENDENTE: 'Pendente',
  CONCILIADO: 'Conciliado',
  DIVERGENTE: 'Divergente',
}

export const ROLES = {
  ADMIN:        'ROLE_ADMIN',
  SINDICO:      'ROLE_SINDICO',
  USER:         'ROLE_USER',
  MORADOR:      'ROLE_MORADOR',
  PROPRIETARIO: 'ROLE_PROPRIETARIO',
} as const

export type RoleValue = typeof ROLES[keyof typeof ROLES]

/** Precedência para resolução de dashboard (maior índice = menor prioridade). */
export const ROLE_PRECEDENCIA: RoleValue[] = [
  ROLES.ADMIN,
  ROLES.SINDICO,
  ROLES.PROPRIETARIO,
  ROLES.MORADOR,
  ROLES.USER,
]
