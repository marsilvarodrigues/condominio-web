package com.pmrodrigues.financeiro.scheduler;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.financeiro.model.TipoExecucaoRateio;
import com.pmrodrigues.financeiro.service.RateioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled component that triggers automatic daily rateio for all active condominiums.
 *
 * <p>Runs every day at 02:00 São Paulo time. Each condominium is processed independently:
 * a failure in one tenant does not affect the others. The {@link TenantContext} is always
 * cleared in a {@code finally} block to prevent tenant leakage across iterations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateioScheduler {

    private final RateioService rateioService;
    private final CondominioRepository condominioRepository;

    /**
     * Processes all pending and failed despesas for every active condominium.
     * Scheduled daily at 02:00 São Paulo time (America/Sao_Paulo).
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "America/Sao_Paulo")
    public void ratearDespesasPendentes() {
        log.info("RateioScheduler iniciado");
        List<Long> condominios = condominioRepository.findAllIds();
        log.info("RateioScheduler: {} condomínios a processar", condominios.size());

        for (Long condominioId : condominios) {
            long inicio = System.currentTimeMillis();
            try {
                TenantContext.setCondominioId(condominioId);
                var resultado = rateioService.ratearPendentes(condominioId, TipoExecucaoRateio.AUTOMATICO);
                log.info("Rateio automático — condominioId={} total={} sucesso={} erro={} duracaoMs={}",
                        condominioId, resultado.total(), resultado.sucesso(), resultado.erro(),
                        System.currentTimeMillis() - inicio);
            } catch (Exception e) {
                log.error("Falha no rateio automático — condominioId={}: {}",
                        condominioId, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("RateioScheduler concluído");
    }
}
