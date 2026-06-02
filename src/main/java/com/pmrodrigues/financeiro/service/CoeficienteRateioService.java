package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.service.ApartamentoService;
import com.pmrodrigues.financeiro.dto.CoeficienteRateioDTO;
import com.pmrodrigues.financeiro.dto.CreateCoeficienteRateioDTO;
import com.pmrodrigues.financeiro.dto.UpdateConsumoDTO;
import com.pmrodrigues.financeiro.mapper.CoeficienteRateioMapper;
import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import com.pmrodrigues.financeiro.repository.CoeficienteRateioRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pmrodrigues.commons.util.Exceptions.notFound;

/**
 * Application service for managing {@link CoeficienteRateio} records within a group.
 *
 * <p>Uses {@link ApartamentoService} to load {@link Apartamento} entities, respecting the
 * cross-module service rule: financeiro services must not call condominio repositories directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoeficienteRateioService {

    private final CoeficienteRateioRepository repository;
    private final CoeficienteRateioMapper mapper;
    private final GrupoDespesaService grupoDespesaService;
    private final ApartamentoService apartamentoService;

    /**
     * Returns all active coefficient records for the given group, newest vigência first.
     *
     * @param grupoDespesaId the group identifier
     * @return list of DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "coeficiente.rateio.service.findByGrupo", description = "List coeficientes by grupo")
    public List<CoeficienteRateioDTO> findByGrupo(Long grupoDespesaId) {
        log.info("Listing coeficientes for grupoDespesaId={}", grupoDespesaId);
        return repository.findByGrupoDespesaIdOrderByVigenciaDesc(grupoDespesaId)
                .stream().map(mapper::toDTO).toList();
    }

    /**
     * Creates a new coefficient record for a unit within a group.
     *
     * <p>Each call creates a new historical record; existing records are not replaced.
     *
     * @param grupoDespesaId the group identifier
     * @param dto            creation payload
     * @return the created DTO
     */
    @Transactional
    @Timed(value = "coeficiente.rateio.service.create", description = "Create coeficiente de rateio")
    public CoeficienteRateioDTO create(Long grupoDespesaId, CreateCoeficienteRateioDTO dto) {
        log.info("Creating coeficiente for grupoDespesaId={}, apartamentoId={}",
                grupoDespesaId, dto.apartamentoId());
        var grupo = grupoDespesaService.findEntityById(grupoDespesaId)
                .orElseThrow(() -> notFound("GrupoDespesa", grupoDespesaId));
        var apartamento = apartamentoService.findEntityById(dto.apartamentoId())
                .orElseThrow(() -> notFound("Apartamento", dto.apartamentoId()));

        var entity = mapper.toEntity(dto);
        entity.setGrupoDespesa(grupo);
        entity.setApartamento(apartamento);
        var saved = repository.save(entity);
        log.info("Created coeficiente id={}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Updates the monthly consumption reading for a specific coefficient record.
     *
     * @param coefId the coefficient record identifier
     * @param dto    new consumption value
     * @return the updated DTO
     */
    @Transactional
    @Timed(value = "coeficiente.rateio.service.updateConsumo", description = "Update consumo m3")
    public CoeficienteRateioDTO updateConsumo(Long coefId, UpdateConsumoDTO dto) {
        log.info("Updating consumo for coeficienteId={}, consumoM3={}", coefId, dto.consumoM3());
        var entity = repository.findById(coefId)
                .orElseThrow(() -> notFound("CoeficienteRateio", coefId));
        entity.setConsumoM3(dto.consumoM3());
        var saved = repository.save(entity);
        log.info("Updated consumo for coeficienteId={}", coefId);
        return mapper.toDTO(saved);
    }
}
