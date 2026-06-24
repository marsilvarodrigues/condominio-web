import {
  Box,
  Button,
  CircularProgress,
  Grid,
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableFooter,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import AccountBalanceIcon from '@mui/icons-material/AccountBalance'
import SavingsIcon from '@mui/icons-material/Savings'
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong'
import AssessmentIcon from '@mui/icons-material/Assessment'
import { useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useDashboardSindico } from '@/hooks/useDashboardSindico'
import { contasBancariasApi } from '@/api/financeiro/bancos.api'
import { apiClient } from '@/api/client'
import { StatCard } from '@/pages/dashboard/components/StatCard'
import { SectionCard } from '@/pages/dashboard/components/SectionCard'
import { PageHeader, NoCondominioGuard } from '@/components/common'
import { formatCurrency, formatDateTime, formatPercent } from '@/utils/formatters'
import type { OrcamentoItemDTO, ContaBancariaDTO } from '@/types'

export default function PrestacaoContasPage() {
  const [exporting, setExporting] = useState(false)
  const relatorioRef = useRef<HTMLDivElement>(null)
  const ano = new Date().getFullYear()

  const { saldo, fundo, orcamento, rateio } = useDashboardSindico()

  const { data: contas = [], isLoading: loadingContas } = useQuery<ContaBancariaDTO[]>({
    queryKey: ['relatorio', 'contas-bancarias'],
    queryFn: () => contasBancariasApi.list(),
    staleTime: 5 * 60_000,
  })

  const { data: itensOrcamento = [], isLoading: loadingOrcamento } = useQuery<OrcamentoItemDTO[]>({
    queryKey: ['relatorio', 'orcamento-itens', ano],
    queryFn: () =>
      apiClient
        .get<{ data: OrcamentoItemDTO[] }>('/orcamentos', { params: { ano } })
        .then((r) => r.data.data),
    staleTime: 5 * 60_000,
  })

  const loading = saldo.isLoading || fundo.isLoading || orcamento.isLoading || rateio.isLoading

  const totalPrevisto = itensOrcamento.reduce((s, i) => s + Number(i.valorOrcado), 0)
  const totalRealizado = itensOrcamento.reduce((s, i) => s + Number(i.valorRealizado), 0)
  const resultado = totalPrevisto - totalRealizado
  const totalContas = contas.reduce((s, c) => s + Number(c.saldo), 0)

  const handleExportPdf = async () => {
    if (!relatorioRef.current) return
    setExporting(true)
    try {
      const { default: jsPDF } = await import('jspdf')
      const { default: html2canvas } = await import('html2canvas')
      const canvas = await html2canvas(relatorioRef.current, {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
      })
      const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
      const imgData = canvas.toDataURL('image/png')
      const pdfWidth = pdf.internal.pageSize.getWidth()
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width
      pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight)
      pdf.save(`prestacao-contas-${new Date().toISOString().slice(0, 7)}.pdf`)
    } finally {
      setExporting(false)
    }
  }

  return (
    <Box>
      <NoCondominioGuard />
      <PageHeader
        title="Prestação de Contas"
        subtitle={`Exercício ${ano} — gerado em ${formatDateTime(new Date().toISOString())}`}
        actions={
          <Button
            variant="outlined"
            startIcon={exporting ? <CircularProgress size={16} /> : <DownloadIcon />}
            onClick={handleExportPdf}
            disabled={exporting || loading}
          >
            Exportar PDF
          </Button>
        }
      />

      <Box ref={relatorioRef} sx={{ bgcolor: 'white', p: 1 }}>
        {/* Seção 1: Resumo Executivo */}
        <Grid container spacing={2} mb={3}>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Saldo Total"
              value={saldo.data ? formatCurrency(saldo.data.saldoTotal) : '—'}
              subtitle={`${saldo.data?.quantidadeContas ?? 0} contas bancárias`}
              icon={<AccountBalanceIcon />}
              color="#2E7D32"
              loading={saldo.isLoading}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title={`Orçamento ${ano}`}
              value={orcamento.data ? formatPercent(orcamento.data.percentualExecucao) : '—'}
              subtitle={
                orcamento.data
                  ? `${formatCurrency(orcamento.data.totalRealizado)} de ${formatCurrency(orcamento.data.totalPrevisto)}`
                  : undefined
              }
              icon={<ReceiptLongIcon />}
              color="#1565C0"
              progress={orcamento.data?.percentualExecucao}
              loading={orcamento.isLoading}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Fundo de Reserva"
              value={fundo.data ? formatCurrency(fundo.data.saldoAtual) : '—'}
              subtitle={
                fundo.data
                  ? `${formatPercent(fundo.data.percentualContribuicao)} de contribuição`
                  : undefined
              }
              icon={<SavingsIcon />}
              color="#6A1B9A"
              loading={fundo.isLoading}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Último Rateio"
              value={rateio.data ? formatCurrency(rateio.data.despesaTotal) : '—'}
              subtitle={rateio.data ? formatDateTime(rateio.data.dataExecucao) : undefined}
              icon={<AssessmentIcon />}
              color="#E65100"
              loading={rateio.isLoading}
            />
          </Grid>
        </Grid>

        {/* Seção 2: Detalhamento Orçamentário */}
        <Box mb={3}>
        <SectionCard
          title="Receitas e Despesas por Categoria"
          loading={loadingOrcamento}
          empty={itensOrcamento.length === 0}
          emptyMessage="Nenhum item orçamentário encontrado para este exercício."
        >
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ bgcolor: '#F8FAFC' }}>
                  <TableCell sx={{ fontWeight: 700 }}>CATEGORIA</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>PREVISTO</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>REALIZADO</TableCell>
                  <TableCell sx={{ fontWeight: 700, minWidth: 120 }}>%</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>SALDO</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {[...itensOrcamento]
                  .sort((a, b) => a.contaNome.localeCompare(b.contaNome, 'pt-BR'))
                  .map((item) => {
                    const saldoItem = item.valorOrcado - item.valorRealizado
                    const pct =
                      item.valorOrcado > 0
                        ? Math.min((item.valorRealizado / item.valorOrcado) * 100, 100)
                        : 0
                    return (
                      <TableRow key={item.id} hover>
                        <TableCell>
                          <Typography variant="body2">{item.contaNome}</Typography>
                          {item.grupoDespesaNome && (
                            <Typography variant="caption" color="text.secondary">
                              {item.grupoDespesaNome}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell align="right">
                          <Typography variant="body2">{formatCurrency(item.valorOrcado)}</Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography variant="body2">{formatCurrency(item.valorRealizado)}</Typography>
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <LinearProgress
                              variant="determinate"
                              value={pct}
                              sx={{ flex: 1, height: 6, borderRadius: 3 }}
                            />
                            <Typography variant="caption" sx={{ minWidth: 36 }}>
                              {pct.toFixed(0)}%
                            </Typography>
                          </Box>
                        </TableCell>
                        <TableCell align="right">
                          <Typography
                            variant="body2"
                            color={saldoItem < 0 ? 'error' : 'text.primary'}
                            fontWeight={saldoItem < 0 ? 600 : 400}
                          >
                            {formatCurrency(saldoItem)}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )
                  })}
              </TableBody>
              <TableFooter>
                <TableRow sx={{ bgcolor: '#F8FAFC' }}>
                  <TableCell sx={{ fontWeight: 700 }}>TOTAL</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    {formatCurrency(totalPrevisto)}
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    {formatCurrency(totalRealizado)}
                  </TableCell>
                  <TableCell />
                  <TableCell align="right">
                    <Typography
                      variant="body2"
                      fontWeight={700}
                      color={resultado < 0 ? 'error' : '#2E7D32'}
                    >
                      {formatCurrency(resultado)}
                    </Typography>
                  </TableCell>
                </TableRow>
              </TableFooter>
            </Table>
          </TableContainer>
        </SectionCard>
        </Box>

        {/* Seção 3: Contas Bancárias */}
        <Box mb={3}>
        <SectionCard
          title="Posição Bancária"
          loading={loadingContas}
          empty={contas.length === 0}
          emptyMessage="Nenhuma conta bancária cadastrada."
        >
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ bgcolor: '#F8FAFC' }}>
                  <TableCell sx={{ fontWeight: 700 }}>BANCO</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>CONTA</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>TIPO</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>SALDO</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {contas.map((c) => (
                  <TableRow key={c.id} hover>
                    <TableCell>{c.bancoNome}</TableCell>
                    <TableCell>
                      {c.agencia} / {c.conta}
                      {c.descricao && (
                        <Typography variant="caption" color="text.secondary" display="block">
                          {c.descricao}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{c.tipo}</TableCell>
                    <TableCell align="right">
                      <Typography
                        variant="body2"
                        color={c.saldo < 0 ? 'error' : 'text.primary'}
                        fontWeight={c.saldo < 0 ? 600 : 400}
                      >
                        {formatCurrency(c.saldo)}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
              <TableFooter>
                <TableRow sx={{ bgcolor: '#F8FAFC' }}>
                  <TableCell colSpan={3} sx={{ fontWeight: 700 }}>
                    TOTAL
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>
                    <Typography
                      variant="body2"
                      fontWeight={700}
                      color={totalContas < 0 ? 'error' : '#2E7D32'}
                    >
                      {formatCurrency(totalContas)}
                    </Typography>
                  </TableCell>
                </TableRow>
              </TableFooter>
            </Table>
          </TableContainer>
        </SectionCard>
        </Box>

        {/* Seção 4: Último Rateio */}
        {rateio.data && (
          <Box mb={3}>
          <SectionCard title="Último Rateio Executado">
            <Box sx={{ display: 'flex', gap: 4, flexWrap: 'wrap', py: 1 }}>
              <Box>
                <Typography variant="caption" color="text.secondary">DATA/HORA</Typography>
                <Typography variant="body1" fontWeight={500}>
                  {formatDateTime(rateio.data.dataExecucao)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">TOTAL RATEADO</Typography>
                <Typography variant="body1" fontWeight={500} color="#1565C0">
                  {formatCurrency(rateio.data.despesaTotal)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">TIPO</Typography>
                <Typography variant="body1" fontWeight={500}>
                  {rateio.data.tipoExecucao}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">STATUS</Typography>
                <Typography
                  variant="body1"
                  fontWeight={600}
                  color={rateio.data.status === 'SUCESSO' ? '#2E7D32' : '#C62828'}
                >
                  {rateio.data.status}
                </Typography>
              </Box>
            </Box>
          </SectionCard>
          </Box>
        )}

        {/* Rodapé */}
        <Paper variant="outlined" sx={{ p: 2, bgcolor: '#FAFAFA' }}>
          <Typography variant="caption" color="text.secondary" display="block">
            Relatório gerado automaticamente pelo CondoGest em {formatDateTime(new Date().toISOString())}.
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block">
            Os valores refletem o estado atual do sistema no momento da geração.
          </Typography>
        </Paper>
      </Box>
    </Box>
  )
}
