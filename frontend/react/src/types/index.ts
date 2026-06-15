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
  id: number
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

export interface EstadoDTO {
  id: number
  nome: string
  uf: string
}

/** Read-side address DTO: estado is a full nested object as returned by the API. */
export interface EnderecoDTO {
  logradouro: string
  cep: string
  cidade: string
  estado: EstadoDTO
}

/** Write-side address DTO: estado is just the numeric FK, matching CreateCondominioEnderecoDTO on the backend. */
export interface CreateEnderecoDTO {
  logradouro: string
  cep: string
  cidade: string
  estado: number
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
  endereco: CreateEnderecoDTO
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
  blocoId: number
  blocoNome: string | null
  numero: string
  createdAt: string | null
  updatedAt: string | null
  areaConstruida: number
  fracaoIdeal: number | null
  andar: number | null
  quantidadeMoradores: number
}

export interface CreateApartamentoDTO {
  blocoId: number
  numero: string
  areaConstruida: number
  fracaoIdeal?: number
  andar?: number
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

export type StatusLancamento = 'PENDENTE' | 'CONCILIADO'
export type OrigemLancamento =
  | 'COTA_CONDOMINIO' | 'RESERVA' | 'DESPESA_ORDINARIA' | 'DESPESA_EXTRAORDINARIA'
  | 'TAXA_EXTRA' | 'MULTA' | 'JUROS' | 'MANUAL' | 'IMPORTACAO'

export interface LancamentoBancarioDTO {
  id: number
  contaBancariaId: number
  contaBancariaDescricao: string
  dataLancamento: string
  valor: number
  tipo: 'CREDITO' | 'DEBITO'
  descricao: string
  origem: OrigemLancamento
  referenciaId: number | null
  status: StatusLancamento
  createdAt: string
  updatedAt: string
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

// ── Moradores / Pessoas ───────────────────────────────────────────────────────

/** Discriminator values matching backend SINGLE_TABLE inheritance */
export type TipoPessoa = 'MORADOR' | 'PROP_PF' | 'PROP_PJ'

export interface ApartamentoResumoDTO {
  id: number
  numero: string
  blocoNome: string | null
  condominioId: number
}

export interface PessoaDTO {
  id: number
  nome: string
  /** MORADOR | PROP_PF | PROP_PJ */
  tipo: TipoPessoa
  cpf: string | null
  email: string | null
  telefone: string | null
  apartamentoId: number | null
  apartamentoNumero: string | null
  userId: number | null
  createdAt: string | null
  updatedAt: string | null
}

export interface CreatePessoaDTO {
  nome: string
  email: string
  telefone?: string
  cpf: string
  apartamentoId?: number
}

export interface HistoricoOcupacaoDTO {
  id: number
  apartamentoId: number
  pessoaId: number
  nomeMorador: string | null
  emailMorador: string | null
  cpfMorador: string | null
  dataEntrada: string   // ISO date "yyyy-MM-dd"
  dataSaida: string     // ISO date "yyyy-MM-dd"
  createdAt: string     // ISO datetime
}

export interface UpdatePessoaDTO {
  nome?: string
  email?: string
  telefone?: string
  cpf?: string
}

export interface PessoaFilterDTO {
  nome?: string
  tipo?: TipoPessoa
  cpf?: string
  email?: string
}

export interface ProprietarioDTO {
  id: number
  nome: string
  /** PROP_PF | PROP_PJ */
  tipo: 'PROP_PF' | 'PROP_PJ'
  cpf: string | null
  cnpj: string | null
  razaoSocial: string | null
  email: string | null
  telefone: string | null
  apartamentos: ApartamentoResumoDTO[]
  userId: number | null
  createdAt: string | null
  updatedAt: string | null
}

export interface CreateProprietarioDTO {
  nome: string
  email: string
  telefone?: string
  /** "PROP_PF" or "PROP_PJ" */
  tipo: 'PROP_PF' | 'PROP_PJ'
  cpf?: string
  cnpj?: string
  razaoSocial?: string
}

export interface UpdateProprietarioDTO {
  nome?: string
  email?: string
  telefone?: string
  cpf?: string
  cnpj?: string
  razaoSocial?: string
}

export interface ProprietarioFilterDTO {
  nome?: string
  tipo?: 'PROP_PF' | 'PROP_PJ'
  cpf?: string
  cnpj?: string
}

// ── Cobrança ──────────────────────────────────────────────────────────────────

export type StatusCobranca = 'PENDENTE' | 'ENVIADA' | 'VISUALIZADA' | 'PAGA' | 'VENCIDA' | 'CANCELADA'

export interface CobrancaResumoDTO {
  id: number
  vencimento: string        // ISO date (YYYY-MM-DD)
  valor: number
  status: StatusCobranca
  criadaEm: string          // ISO datetime
  pagoEm: string | null
  emailEnviado: boolean
}

export interface CobrancaDTO extends CobrancaResumoDTO {
  apartamentoId: number
  apartamentoNumero: string
  blocoNome: string | null
  moradorId: number | null
  moradorNome: string | null
  moradorEmail: string | null
  boletoUrl: string | null
  boletoCodBarras: string | null
  pixQrCodeBase64: string | null
  pixCopiaCola: string | null
  emailEnviadoEm: string | null
}

export interface GerarCobrancasDTO {
  execucaoId: number
  vencimento: string
}

export interface CobrancaFilterDTO {
  apartamentoId?: number
  status?: StatusCobranca
  vencimentoDe?: string
  vencimentoAte?: string
  emailEnviado?: boolean
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

export type DashboardPerfil = 'admin' | 'sindico' | 'proprietario' | 'morador' | 'usuario'

export interface ResumoFinanceiro {
  saldoTotal: number
  quantidadeContas: number
}

export interface ResumoOrcamento {
  exercicio: number
  totalPrevisto: number
  totalRealizado: number
  percentualExecucao: number
}

export interface ResumoApartamentos {
  total: number
  ocupados: number
  vagos: number
  taxaOcupacao: number
}

export interface ResumoCobrancas {
  totalPendente: number
  quantidadePendente: number
  totalVencido: number
  quantidadeVencida: number
}

export interface MoradorJwtClaims {
  sub: string
  roles: string[]
  condominio_ids: number[]
  apartamento_id?: number
}

export interface ProprietarioJwtClaims {
  sub: string
  roles: string[]
  condominio_ids: number[]
  user_id?: number
  apartamentos_ids_proprietario?: number[]
}
