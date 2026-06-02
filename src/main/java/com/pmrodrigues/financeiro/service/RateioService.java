package com.pmrodrigues.financeiro.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.financeiro.dto.CotaUnidadeDTO;
import com.pmrodrigues.financeiro.dto.RateioExecucaoDTO;
import com.pmrodrigues.financeiro.dto.RateioLoteResultado;
import com.pmrodrigues.financeiro.dto.SimularRateioRequest;
import com.pmrodrigues.financeiro.dto.SimularRateioResponse;
import com.pmrodrigues.financeiro.mapper.RateioExecucaoMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        Despesa despesa = despesaRepo.findById(despesaId)
                .orElseThrow(() -> notFound("Despesa", despesaId));

        var grupo = despesa.getGrupoDespesa();
        var execucao = RateioExecucao.builder()
                .despesa(despesa)
                .grupoDespesaId(grupo.getId())
                .tipoExecucao(tipoExecucao)
                .despesaTotal(despesa.getValorTotal())
                .dataExecucao(LocalDateTime.now())
                .build();

        try {
            var coeficientes = coeficienteRepo.findVigentesPorGrupo(grupo.getId(), despesa.getCompetencia());
            if (coeficientes.isEmpty()) {
                throw new IllegalStateException(
                        "Nenhum coeficiente vigente para grupoDespesaId=" + grupo.getId());
            }

            var estrategia = resolverEstrategia(grupo.getTipoRateio().name());
            Map<String, Object> parametros = deserializarParametros(grupo.getParametrosJson());
            Map<Long, BigDecimal> cotasCalculadas =
                    estrategia.calcular(coeficientes, despesa.getValorTotal(), parametros);

            // replace existing quotas
            cotaRateioRepo.deleteByDespesaId(despesaId);

            BigDecimal somaCotas = BigDecimal.ZERO;
            for (var entry : cotasCalculadas.entrySet()) {
                var cota = CotaRateio.builder()
                        .despesa(despesa)
                        .apartamento(coeficientes.stream()
                                .filter(c -> c.getApartamento().getId().equals(entry.getKey()))
                                .findFirst()
                                .orElseThrow()
                                .getApartamento())
                        .valor(entry.getValue())
                        .rateioExecucao(execucao)
                        .build();
                cotaRateioRepo.save(cota);
                somaCotas = somaCotas.add(entry.getValue());
            }

            execucao.setTotalUnidades(coeficientes.size());
            execucao.setTotalCotas(somaCotas);
            execucao.setStatus(StatusExecucaoRateio.SUCESSO);

            despesa.setRateioStatus(StatusRateio.RATEADA);
            despesa.setDataUltimoRateio(LocalDateTime.now());

            log.info("Despesa id={} rateada com sucesso: {} unidades, total={}",
                    despesaId, coeficientes.size(), somaCotas);
        } catch (Exception e) {
            log.error("Erro ao ratear despesa id={}: {}", despesaId, e.getMessage(), e);
            execucao.setStatus(StatusExecucaoRateio.ERRO);
            execucao.setErroMensagem(e.getMessage());
            despesa.setRateioStatus(StatusRateio.ERRO);
            despesa.setDataUltimoRateio(LocalDateTime.now());
        }

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

        List<Despesa> pendentes = despesaRepo.findPendentesOuErro();
        int sucesso = 0;
        int erro = 0;

        for (Despesa d : pendentes) {
            try {
                RateioExecucao exec = ratear(d.getId(), tipoExecucao);
                if (exec.getStatus() == StatusExecucaoRateio.SUCESSO) {
                    sucesso++;
                } else {
                    erro++;
                }
            } catch (Exception e) {
                log.error("Falha inesperada ao ratear despesa id={}: {}", d.getId(), e.getMessage(), e);
                erro++;
            }
        }

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

        var coeficientes = coeficienteRepo.findVigentesPorGrupo(
                grupo.getId(), java.time.LocalDate.now());

        if (coeficientes.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhum coeficiente vigente para grupoDespesaId=" + grupo.getId());
        }

        var estrategia = resolverEstrategia(grupo.getTipoRateio().name());
        Map<String, Object> parametros = request.parametros() != null
                ? request.parametros()
                : deserializarParametros(grupo.getParametrosJson());

        Map<Long, BigDecimal> cotasCalculadas =
                estrategia.calcular(coeficientes, request.despesaTotal(), parametros);

        BigDecimal somaCotas = cotasCalculadas.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaPesos = somaCotas.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ONE : somaCotas;

        List<CotaUnidadeDTO> cotasDto = new ArrayList<>();
        for (var c : coeficientes) {
            Long aptId = c.getApartamento().getId();
            BigDecimal cota = cotasCalculadas.getOrDefault(aptId, BigDecimal.ZERO);
            BigDecimal pctCota = somaCotas.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : cota.divide(request.despesaTotal(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            cotasDto.add(new CotaUnidadeDTO(aptId, c.getApartamento().getNumero(),
                    null, null, cota, pctCota));
        }

        log.info("Simulação concluída: {} unidades, somaCotas={}", coeficientes.size(), somaCotas);
        return new SimularRateioResponse(cotasDto, somaCotas);
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
    public Page<RateioExecucaoDTO> findExecucoes(
            com.pmrodrigues.financeiro.dto.RateioExecucaoFilterDTO filter, Pageable pageable) {
        log.info("Listing rateio execucoes: filter={}", filter);
        return execucaoRepo.findAll(
                Specification.allOf(
                        hasStatus(filter.status()),
                        hasTipoExecucao(filter.tipoExecucao()),
                        dataInicio(filter.dataInicio()),
                        dataFim(filter.dataFim())),
                pageable).map(execucaoMapper::toDTO);
    }

    private EstrategiaRateio resolverEstrategia(String tipoRateioName) {
        EstrategiaRateio estrategia = estrategias.get(tipoRateioName);
        if (estrategia == null) {
            throw new IllegalStateException("Estratégia não encontrada para tipo: " + tipoRateioName);
        }
        return estrategia;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializarParametros(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Erro ao desserializar parametrosJson: {}", e.getMessage());
            return Map.of();
        }
    }
}
