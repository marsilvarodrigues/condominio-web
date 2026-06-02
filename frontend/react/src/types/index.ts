// ── Envelope de resposta da API ────────────────────────────────────────────────

export interface ApiResponse<T> {
  requestId: string
  timestamp: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface ValidationError {
  error: 'validation_error'
  fields: Record<string, string>
}

// ── Auth ──────────────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string
  password: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface RefreshRequest {
  refreshToken: string
}

export interface AuthUser {
  email: string
  roles: string[]
  condominioIds: number[]
}

// ── Usuário ───────────────────────────────────────────────────────────────────

export interface UserDTO {
  id: number
  email: string
  name: string
  enabled: boolean
  roles: string[]
  condominioIds: number[]
  activationToken: string | null
  activationTokenExpiry: string | null
}

export interface CreateUserDTO {
  email: string
  name: string
  roles: string[]
  condominioIds: number[]
}

export interface ChangePasswordDTO {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

// ── Endereço ──────────────────────────────────────────────────────────────────

export interface EnderecoDTO {
  logradouro: string
  cep: string
  cidade: string
  estadoId: number
  estadoNome?: string
  estadoUf?: string
}

export interface EstadoDTO {
  id: number
  nome: string
  uf: string
}

// ── Condomínio ────────────────────────────────────────────────────────────────

export interface CondominioDTO {
  id: number
  nome: string
  cnpj: string
  email: string
  endereco: EnderecoDTO
}

export interface CreateCondominioDTO {
  nome: string
  cnpj: string
  email: string
  endereco: EnderecoDTO
}

// ── Bloco / Apartamento ───────────────────────────────────────────────────────

export interface BlocoDTO {
  id: number
  numero: number
  bloco: string
  condominioId: number
}

export interface CreateBlocoDTO {
  numero: number
  bloco: string
}

export interface ApartamentoDTO {
  id: number
  numero: string
  andar: number
  blocoId: number
  blocoNome: string
  fracaoIdeal: number
  metragem: number
  proprietarioId: number | null
  proprietarioNome: string | null
}

export interface CreateApartamentoDTO {
  numero: string
  andar: number
  blocoId: number
  fracaoIdeal: number
  metragem: number
  proprietarioId?: number
}

// ── Plano de Contas ───────────────────────────────────────────────────────────

export type TipoConta = 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA'

export interface PlanoContaDTO {
  id: number
  codigo: string
  nome: string
  tipo: TipoConta
  contaParenteId: number | null
  contaParenteNome: string | null
  nivel: number
}

export interface CreatePlanoContaDTO {
  codigo: string
  nome: string
  tipo: TipoConta
  contaParenteId?: number
}

// ── Orçamento Anual ───────────────────────────────────────────────────────────

export type StatusRateio = 'PENDENTE' | 'RATEADA' | 'ERRO'

export interface OrcamentoItemDTO {
  id: number
  ano: number
  contaNome: string
  contaId: number
  grupoDespesaId: number | null
  grupoDespesaNome: string | null
  valorOrcado: number
  valorRealizado: number
  statusRateio: string | null
}

export interface AddOrcamentoItemDTO {
  contaId: number
  grupoDespesaId?: number
  valorOrcado: number
}

// ── Fundo de Reserva ──────────────────────────────────────────────────────────

export type TipoMovimentacaoFundo = 'ENTRADA' | 'SAIDA' | 'RENDIMENTO' | 'APLICACAO' | 'RESGATE'

export interface FundoReservaDTO {
  id: number
  saldoAtual: number
  saldoMinimo: number
  percentualContribuicao: number
}

export interface MovimentacaoFundoDTO {
  id: number
  tipo: TipoMovimentacaoFundo
  valor: number
  descricao: string
  data: string
  saldoApos: number
}

export interface CreateMovimentacaoFundoDTO {
  tipo: TipoMovimentacaoFundo
  valor: number
  descricao: string
  data: string
}

// ── Banco / Conta Bancária ────────────────────────────────────────────────────

export interface BancoDTO {
  id: number
  codigo: string
  nome: string
  ativo: boolean
}

export interface CreateBancoDTO {
  codigo: string
  nome: string
}

export type TipoConta2 = 'CORRENTE' | 'POUPANCA' | 'INVESTIMENTO'

export interface ContaBancariaDTO {
  id: number
  agencia: string
  conta: string
  tipo: TipoConta2
  descricao: string
  bancoId: number
  bancoNome: string
  saldo: number
}

export interface CreateContaBancariaDTO {
  agencia: string
  conta: string
  tipo: TipoConta2
  descricao?: string
  bancoId: number
}

export interface LancamentoBancarioDTO {
  id: number
  data: string
  descricao: string
  valor: number
  saldo: number
  tipo: 'CREDITO' | 'DEBITO'
  conciliado: boolean
  contaBancariaId: number
}

// ── Conciliação ───────────────────────────────────────────────────────────────

export type StatusConciliacao = 'PENDENTE' | 'CONCILIADO' | 'DIVERGENTE'

export interface ConciliacaoItemDTO {
  id: number
  data: string
  descricao: string
  valor: number
  status: StatusConciliacao
  lancamentoDescricao: string | null
  sugestaoLancamentoId: number | null
}

export interface AssociacaoRequest {
  lancamentoBancarioId: number
}

// ── Grupos de Despesa / Rateio ────────────────────────────────────────────────

export type TipoRateio = 'IGUALITARIO' | 'FRACAO_IDEAL' | 'METRAGEM' | 'CONSUMO'
export type EscopoRateio = 'TODOS' | 'BLOCO'
export type StatusExecucaoRateio = 'SUCESSO' | 'ERRO' | 'PARCIAL'
export type TipoExecucaoRateio = 'AUTOMATICO' | 'MANUAL' | 'RECALCULO'

export interface GrupoDespesaDTO {
  id: number
  nome: string
  tipoRateio: TipoRateio
  escopo: EscopoRateio
  blocoId: number | null
  planoContasId: number | null
  parametrosJson: string | null
}

export interface CreateGrupoDespesaDTO {
  nome: string
  tipoRateio: TipoRateio
  escopo: EscopoRateio
  blocoId?: number
  planoContasId?: number
  parametrosJson?: string
}

export interface CoeficienteRateioDTO {
  id: number
  grupoDespesaId: number
  apartamentoId: number
  apartamentoNumero: string
  blocoNome: string | null
  coeficiente: number
  vigenciaInicio: string
  vigenciaFim: string | null
}

export interface CreateCoeficienteRateioDTO {
  apartamentoId: number
  coeficiente: number
}

export interface SimulacaoLinhaDTO {
  apartamentoId: number
  apartamentoNumero: string
  blocoNome: string | null
  coeficiente: number
  valorRateado: number
}

export interface SimulacaoRateioDTO {
  grupoNome: string
  totalDespesas: number
  linhas: SimulacaoLinhaDTO[]
}

export interface SimularRateioRequest {
  grupoId: number
  ano: number
}

export interface RateioLancamentoDTO {
  id: number
  apartamentoId: number
  apartamentoNumero: string
  blocoNome: string | null
  coeficiente: number
  valor: number
  status: string
}

export interface RateioExecucaoDTO {
  id: number
  tipo: TipoExecucaoRateio
  dataExecucao: string
  status: StatusExecucaoRateio
  totalRateado: number
  descricao: string | null
  lancamentos?: RateioLancamentoDTO[]
}

export interface RateioLoteResultado {
  total: number
  sucesso: number
  erro: number
  duracaoMs: number
}

export interface RecalcularRateioRequest {
  confirmar: boolean
}

// ── Filtros ───────────────────────────────────────────────────────────────────

export interface RateioExecucaoFilterDTO {
  status?: StatusExecucaoRateio
  tipoExecucao?: TipoExecucaoRateio
  dataInicio?: string
  dataFim?: string
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

export interface DashboardStats {
  totalApartamentos: number
  taxaOcupacao: number
  saldoBancario: number
  despesasMes: number
  inadimplencia: number
  proximoRateio: string | null
}
