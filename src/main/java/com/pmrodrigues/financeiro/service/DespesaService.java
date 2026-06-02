package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.CreateDespesaDTO;
import com.pmrodrigues.financeiro.dto.DespesaDTO;
import com.pmrodrigues.financeiro.mapper.DespesaMapper;
import com.pmrodrigues.financeiro.model.Despesa;
import com.pmrodrigues.financeiro.repository.DespesaRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.pmrodrigues.commons.util.Exceptions.notFound;

/**
 * Application service for managing {@link Despesa} entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository repository;
    private final DespesaMapper mapper;
    private final GrupoDespesaService grupoDespesaService;

    /**
     * Returns a page of despesas for the current tenant.
     *
     * @param pageable pagination parameters
     * @return page of DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "despesa.service.findAll", description = "List despesas")
    public Page<DespesaDTO> findAll(Pageable pageable) {
        log.info("Listing despesas");
        var result = repository.findAll(pageable).map(mapper::toDTO);
        log.info("Listing despesas: {} total", result.getTotalElements());
        return result;
    }

    /**
     * Returns a single despesa by its identifier.
     *
     * @param id the despesa identifier
     * @return the DTO
     * @throws org.springframework.web.server.ResponseStatusException 404 if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "despesa.service.findById", description = "Find despesa by id")
    public DespesaDTO findById(Long id) {
        log.info("Finding despesa id={}", id);
        return repository.findById(id).map(mapper::toDTO).orElseThrow(() -> notFound("Despesa", id));
    }

    /**
     * Returns the entity by id; used internally by the rateio service.
     *
     * @param id the despesa identifier
     * @return the entity
     * @throws org.springframework.web.server.ResponseStatusException 404 if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "despesa.service.findEntityById", description = "Find despesa entity by id")
    public Despesa findEntityById(Long id) {
        log.info("Finding despesa entity id={}", id);
        return repository.findById(id).orElseThrow(() -> notFound("Despesa", id));
    }

    /**
     * Creates a new despesa with {@code rateioStatus = PENDENTE}.
     *
     * @param dto creation payload
     * @return the created DTO
     * @throws org.springframework.web.server.ResponseStatusException 404 if the referenced group does not exist
     */
    @Transactional
    @Timed(value = "despesa.service.create", description = "Create despesa")
    public DespesaDTO create(CreateDespesaDTO dto) {
        log.info("Creating despesa: grupoDespesaId={}, competencia={}", dto.grupoDespesaId(), dto.competencia());
        var grupo = grupoDespesaService.findEntityById(dto.grupoDespesaId())
                .orElseThrow(() -> notFound("GrupoDespesa", dto.grupoDespesaId()));
        var entity = mapper.toEntity(dto);
        entity.setGrupoDespesa(grupo);
        var saved = repository.save(entity);
        log.info("Created despesa id={}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Soft-deletes a despesa.
     *
     * @param id the despesa identifier
     * @throws org.springframework.web.server.ResponseStatusException 404 if not found
     */
    @Transactional
    @Timed(value = "despesa.service.delete", description = "Delete despesa")
    public void delete(Long id) {
        log.info("Deleting despesa id={}", id);
        var entity = repository.findById(id).orElseThrow(() -> notFound("Despesa", id));
        repository.delete(entity);
        log.info("Deleted despesa id={}", id);
    }
}
