package com.pmrodrigues.financeiro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.financeiro.dto.CotaUnidadeDTO;
import com.pmrodrigues.financeiro.dto.RateioExecucaoDTO;
import com.pmrodrigues.financeiro.dto.RateioExecucaoFilterDTO;
import com.pmrodrigues.financeiro.dto.RateioLoteResultado;
import com.pmrodrigues.financeiro.dto.SimularRateioRequest;
import com.pmrodrigues.financeiro.dto.SimularRateioResponse;
import com.pmrodrigues.financeiro.mapper.RateioExecucaoMapper;
import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import com.pmrodrigues.financeiro.model.CotaRateio;
import com.pmrodrigues.financeiro.model.Despesa;
import com.pmrodrigues.financeiro.model.RateioExecucao;
import com.pmrodrigues.financeiro.model.StatusExecucaoRateio;
import com.pmrodrigues.financeiro.model.StatusRateio;
import com.pmrodrigues.financeiro.model.TipoExecucaoRateio;
import com.pmrodrigues.financeiro.rateio.EstrategiaRateio;
import com.pmrodrigues.financeiro.repository.CoeficienteRateioRepository;
import com.pmrodrigues.financeiro.repository.CotaRateioRepository;
import com.pmrodrigues.financeiro.repository.DespesaRepository;
import com.pmrodrigues.financeiro.repository.GrupoDespesaRepository;
import com.pmrodrigues.financeiro.repository.RateioExecucaoRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.financeiro.specification.RateioExecucaoSpecification.*;

/**
 * Orchestrates rateio calculations: single-expense ratear, batch processing of pending expenses,
 * and full recalculation.
 *
 * <p>Strategy resolution: Spring injects all {@link EstrategiaRateio} implementations into a
 * {@code Map<String, EstrategiaRateio>} keyed by the Spring bean name, which matches
 * {@link com.pmrodrigues.financeiro.model.TipoRateio#name()}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateioService {

    private final Map<String, EstrategiaRateio> estrategias;
    private final CoeficienteRateioRepository coeficienteRepo;
    private final GrupoDespesaRepository grupoDespesaRepo;
    private final DespesaRepository despesaRepo;
    private final RateioExecucaoRepository execucaoRepo;
    private final CotaRateioRepository cotaRateioRepo;
    private final RateioExecucaoMapper execucaoMapper;
    private final ObjectMapper objectMapper;

    /**
     * Calculates and persists quotas for a single despesa.
     *
     * <p>Clears existing quotas before inserting new ones. Persists a {@link RateioExecucao}
     * record and updates {@link Despesa#getRateioStatus()} on completion.
     *
     * @param despesaId    the expense to rate
     * @param tipoExecucao who or what triggered this run
     * @return the audit record
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "rateio.service.calcular", description = "Rate a single despesa")
    public RateioExecucao ratear(Long despesaId, TipoExecucaoRateio tipoExecucao) {
        log.info("Rateando despesa id={} tipoExecucao={}", despesaId, tipoExecucao);
        var despesa = despesaRepo.findById(despesaId)
                .orElseThrow(() -> notFound("Despesa", despesaId));

        var execucao = novaExecucao(despesa, tipoExecucao);
        calcularEPersistirCotas(despesa, execucao);

        despesaRepo.save(despesa);
        return execucaoRepo.save(execucao);
    }

    /**
     * Triggers a manual rateio for a single expense and returns the mapped DTO.
     * Convenience wrapper used by the controller to avoid exposing the entity type.
     *
     * @param despesaId the expense to rate
     * @return the audit record as DTO
     */
    @Timed(value = "rateio.service.ratearManual", description = "Rate a single despesa manually and return DTO")
    public RateioExecucaoDTO ratearManual(Long despesaId) {
        return execucaoMapper.toDTO(ratear(despesaId, TipoExecucaoRateio.MANUAL));
    }

    /**
     * Rates all PENDENTE or ERRO expenses for the current tenant, each in its own transaction.
     *
     * <p>A failure in one expense does not cancel the remaining ones.
     *
     * @param condominioId the tenant identifier (used only for logging)
     * @param tipoExecucao execution type to record in audit
     * @return batch summary
     */
    @Timed(value = "rateio.service.lote", description = "Rate all pending despesas")
    public RateioLoteResultado ratearPendentes(Long condominioId, TipoExecucaoRateio tipoExecucao) {
        log.info("ratearPendentes condominioId={} tipoExecucao={}", condominioId, tipoExecucao);
        long inicio = System.currentTimeMillis();

        var pendentes = despesaRepo.findPendentesOuErro();
        var resultados = pendentes.stream()
                .map(d -> executarRateioSeguro(d, tipoExecucao))
                .toList();

        int sucesso = (int) resultados.stream().filter(b -> b).count();
        int erro = pendentes.size() - sucesso;
        long duracao = System.currentTimeMillis() - inicio;
        log.info("ratearPendentes concluído: total={} sucesso={} erro={} duracaoMs={}",
                pendentes.size(), sucesso, erro, duracao);
        return new RateioLoteResultado(pendentes.size(), sucesso, erro, duracao);
    }

    /**
     * Marks all expenses in the current tenant as PENDENTE then runs a full recalculation.
     *
     * <p><strong>Warning:</strong> all previously calculated quotas are replaced.
     * Requires ADMIN role (enforced at controller level).
     *
     * @param condominioId the tenant identifier
     * @return batch summary
     */
    @Transactional
    @Timed(value = "rateio.service.recalculo", description = "Force recalculation of all despesas")
    public RateioLoteResultado recalcularTudo(Long condominioId) {
        log.info("recalcularTudo condominioId={}", condominioId);
        despesaRepo.marcarTodosPendente(condominioId);
        return ratearPendentes(condominioId, TipoExecucaoRateio.RECALCULO);
    }

    /**
     * Simulates a rateio without persisting any results.
     *
     * @param request simulation parameters
     * @return per-unit quota breakdown
     */
    @Transactional(readOnly = true)
    @Timed(value = "rateio.service.simular", description = "Simulate rateio without persisting")
    public SimularRateioResponse simular(SimularRateioRequest request) {
        log.info("Simulando rateio: grupoDespesaId={}, total={}",
                request.grupoDespesaId(), request.despesaTotal());

        var grupo = grupoDespesaRepo.findById(request.grupoDespesaId())
                .orElseThrow(() -> notFound("GrupoDespesa", request.grupoDespesaId()));

        var coeficientes = coeficienteRepo.findVigentesPorGrupo(grupo.getId(), LocalDate.now());
        validarCoeficientes(coeficientes, grupo.getId());

        var estrategia = resolverEstrategia(grupo.getTipoRateio().name());
        var parametros = request.parametros() != null
                ? request.parametros()
                : deserializarParametros(grupo.getParametrosJson());

        var cotasCalculadas = estrategia.calcular(coeficientes, request.despesaTotal(), parametros);
        var somaCotas = cotasCalculadas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        var cotas = coeficientes.stream()
                .map(c -> {
                    var aptId = c.getApartamento().getId();
                    var cota = cotasCalculadas.getOrDefault(aptId, BigDecimal.ZERO);
                    return new CotaUnidadeDTO(aptId, c.getApartamento().getNumero(), null, null,
                            cota, percentualCota(cota, request.despesaTotal()));
                })
                .toList();

        log.info("Simulação concluída: {} unidades, somaCotas={}", cotas.size(), somaCotas);
        return new SimularRateioResponse(cotas, somaCotas);
    }

    /**
     * Returns a page of execution audit records, filtered by the given criteria.
     *
     * @param filter   optional filter parameters
     * @param pageable pagination
     * @return page of DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "rateio.service.execucoes", description = "List rateio executions")
    public Page<RateioExecucaoDTO> findExecucoes(RateioExecucaoFilterDTO filter, Pageable pageable) {
        log.info("Listing rateio execucoes: filter={}", filter);
        return execucaoRepo.findAll(
                Specification.allOf(
                        hasStatus(filter.status()),
                        hasTipoExecucao(filter.tipoExecucao()),
                        dataInicio(filter.dataInicio()),
                        dataFim(filter.dataFim())),
                pageable).map(execucaoMapper::toDTO);
    }

    private RateioExecucao novaExecucao(Despesa despesa, TipoExecucaoRateio tipoExecucao) {
        return RateioExecucao.builder()
                .despesa(despesa)
                .grupoDespesaId(despesa.getGrupoDespesa().getId())
                .tipoExecucao(tipoExecucao)
                .despesaTotal(despesa.getValorTotal())
                .dataExecucao(LocalDateTime.now())
                .build();
    }

    private void calcularEPersistirCotas(Despesa despesa, RateioExecucao execucao) {
        try {
            var grupo = despesa.getGrupoDespesa();
            var coeficientes = coeficienteRepo.findVigentesPorGrupo(grupo.getId(), despesa.getCompetencia());
            validarCoeficientes(coeficientes, grupo.getId());

            var estrategia = resolverEstrategia(grupo.getTipoRateio().name());
            var parametros = deserializarParametros(grupo.getParametrosJson());
            var cotasCalculadas = estrategia.calcular(coeficientes, despesa.getValorTotal(), parametros);

            cotaRateioRepo.deleteByDespesaId(despesa.getId());
            var somaCotas = persistirCotas(cotasCalculadas, coeficientes, despesa, execucao);

            marcarSucesso(execucao, despesa, coeficientes.size(), somaCotas);
        } catch (Exception e) {
            marcarErro(execucao, despesa, e);
        }
    }

    private BigDecimal persistirCotas(Map<Long, BigDecimal> cotasCalculadas,
                                       List<CoeficienteRateio> coeficientes,
                                       Despesa despesa, RateioExecucao execucao) {
        var cotas = cotasCalculadas.entrySet().stream()
                .map(entry -> CotaRateio.builder()
                        .despesa(despesa)
                        .apartamento(resolverApartamento(coeficientes, entry.getKey()))
                        .valor(entry.getValue())
                        .rateioExecucao(execucao)
                        .build())
                .toList();
        cotaRateioRepo.saveAll(cotas);
        return cotas.stream().map(CotaRateio::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Apartamento resolverApartamento(List<CoeficienteRateio> coeficientes, Long aptId) {
        return coeficientes.stream()
                .filter(c -> c.getApartamento().getId().equals(aptId))
                .findFirst()
                .orElseThrow()
                .getApartamento();
    }

    private void marcarSucesso(RateioExecucao execucao, Despesa despesa, int totalUnidades, BigDecimal somaCotas) {
        execucao.setTotalUnidades(totalUnidades);
        execucao.setTotalCotas(somaCotas);
        execucao.setStatus(StatusExecucaoRateio.SUCESSO);
        despesa.setRateioStatus(StatusRateio.RATEADA);
        despesa.setDataUltimoRateio(LocalDateTime.now());
        log.info("Despesa id={} rateada com sucesso: {} unidades, total={}",
                despesa.getId(), totalUnidades, somaCotas);
    }

    private void marcarErro(RateioExecucao execucao, Despesa despesa, Exception e) {
        log.error("Erro ao ratear despesa id={}: {}", despesa.getId(), e.getMessage(), e);
        execucao.setStatus(StatusExecucaoRateio.ERRO);
        execucao.setErroMensagem(e.getMessage());
        despesa.setRateioStatus(StatusRateio.ERRO);
        despesa.setDataUltimoRateio(LocalDateTime.now());
    }

    private boolean executarRateioSeguro(Despesa despesa, TipoExecucaoRateio tipoExecucao) {
        try {
            var exec = ratear(despesa.getId(), tipoExecucao);
            return exec.getStatus() == StatusExecucaoRateio.SUCESSO;
        } catch (Exception e) {
            log.error("Falha inesperada ao ratear despesa id={}: {}", despesa.getId(), e.getMessage(), e);
            return false;
        }
    }

    private void validarCoeficientes(List<CoeficienteRateio> coeficientes, Long grupoDespesaId) {
        if (coeficientes.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhum coeficiente vigente para grupoDespesaId=" + grupoDespesaId);
        }
    }

    private BigDecimal percentualCota(BigDecimal cota, BigDecimal despesaTotal) {
        if (despesaTotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return cota.divide(despesaTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private EstrategiaRateio resolverEstrategia(String tipoRateioName) {
        var estrategia = estrategias.get(tipoRateioName);
        if (estrategia == null) {
            throw new IllegalStateException("Estratégia não encontrada para tipo: " + tipoRateioName);
        }
        return estrategia;
    }

    private Map<String, Object> deserializarParametros(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Erro ao desserializar parametrosJson: {}", e.getMessage());
            return Map.of();
        }
    }
}
