package com.pmrodrigues.morador.service;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.service.CondominioService;
import com.pmrodrigues.morador.dto.CreateProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioFilterDTO;
import com.pmrodrigues.morador.dto.UpdateProprietarioDTO;
import com.pmrodrigues.morador.mapper.ProprietarioMapper;
import com.pmrodrigues.morador.model.Proprietario;
import com.pmrodrigues.morador.repository.ProprietarioRepository;
import com.pmrodrigues.security.mapper.UserMapper;
import com.pmrodrigues.security.repository.PasswordHistoryRepository;
import com.pmrodrigues.security.repository.UserRepository;
import com.pmrodrigues.security.service.UserService;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.morador.specification.ProprietarioSpecification.hasApartamentoId;

/**
 * Application service for managing {@code Proprietario} entities and their apartment associations.
 * Extends {@link UserService} to reuse user-creation ceremony (email uniqueness check, activation email dispatch).
 */
@Slf4j
@Service
public class ProprietarioService extends UserService {

    private final ProprietarioRepository repository;
    private final ProprietarioMapper mapper;

    public ProprietarioService(UserRepository userRepository,
                                MailService mailService,
                                UserMapper userMapper,
                                PasswordHistoryRepository passwordHistoryRepository,
                                CondominioService condominioService,
                                PasswordEncoder passwordEncoder,
                                ProprietarioRepository repository,
                                ProprietarioMapper mapper) {
        super(userRepository, mailService, userMapper, passwordHistoryRepository, condominioService, passwordEncoder);
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Returns a paginated, filtered list of proprietarios.
     *
     * @param filter   optional filter parameters; absent fields are ignored
     * @param pageable pagination and sort parameters
     * @return page of matching proprietario DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "proprietario.service.filterBy", description = "Filter proprietarios")
    public Page<ProprietarioDTO> filterBy(ProprietarioFilterDTO filter, Pageable pageable) {
        log.info("Filtering proprietarios: apartamentoId={}", filter.apartamentoId());
        var spec = Specification.allOf(hasApartamentoId(filter.apartamentoId()));
        var result = repository.findAll(spec, pageable).map(mapper::toDTO);
        log.info("Filter proprietarios: {} results", result.getTotalElements());
        return result;
    }

    /**
     * Looks up a single proprietario by its primary key.
     *
     * @param id proprietario primary key
     * @return the DTO for the found entity
     * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "proprietario.service.findById", description = "Find proprietario by id")
    public ProprietarioDTO findById(Long id) {
        log.info("Looking up proprietario by id: {}", id);
        var result = repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> notFound("Proprietario", id));
        log.info("Proprietario lookup by id {}: found", id);
        return result;
    }

    /**
     * Returns all proprietarios associated with the given apartment.
     *
     * @param apartamentoId apartment primary key
     * @return list of proprietario DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "proprietario.service.findByApartamento", description = "Find proprietarios by apartamento")
    public List<ProprietarioDTO> findByApartamento(Long apartamentoId) {
        log.info("Finding proprietarios for apartamento: {}", apartamentoId);
        var result = repository.findByApartamentosId(apartamentoId)
                .stream()
                .map(mapper::toDTO)
                .toList();
        log.info("Found {} proprietarios for apartamento {}", result.size(), apartamentoId);
        return result;
    }

    /**
     * Persists a new Proprietario of the appropriate sub-type, delegating the user-creation
     * ceremony to {@link #persistUser}.
     *
     * @param dto creation payload with discriminator tipo (PROP_PF or PROP_PJ)
     * @return the persisted proprietario as a DTO
     */
    @Transactional
    @Timed(value = "proprietario.service.create", description = "Create proprietario")
    public ProprietarioDTO create(CreateProprietarioDTO dto) {
        log.info("Creating proprietario: tipo={}", dto.tipo());
        var entity = mapper.toEntity(dto);
        var saved = (Proprietario) persistUser(entity, dto.email(), null);
        log.info("Proprietario created successfully with id: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Updates the mutable fields of an existing proprietario.
     * The tipo discriminator cannot be changed after creation.
     *
     * @param id  proprietario primary key
     * @param dto partial-update payload; null fields are ignored
     * @return updated proprietario DTO
     * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
     */
    @Transactional
    @Timed(value = "proprietario.service.update", description = "Update proprietario")
    public ProprietarioDTO update(Long id, UpdateProprietarioDTO dto) {
        log.info("Updating proprietario with id: {}", id);
        var entity = repository.findById(id)
                .orElseThrow(() -> notFound("Proprietario", id));
        mapper.updateEntity(dto, entity);
        var saved = repository.save(entity);
        log.info("Proprietario updated successfully: {}", id);
        return mapper.toDTO(saved);
    }

    /**
     * Associates the proprietario with the given apartment.
     *
     * @param proprietarioId proprietario primary key
     * @param apartamentoId  apartment primary key
     * @return updated proprietario DTO
     * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
     */
    @Transactional
    @Timed(value = "proprietario.service.associarApartamento", description = "Associate proprietario with apartamento")
    public ProprietarioDTO associarApartamento(Long proprietarioId, Long apartamentoId) {
        log.info("Associating proprietario {} with apartamento {}", proprietarioId, apartamentoId);
        var proprietario = repository.findById(proprietarioId)
                .orElseThrow(() -> notFound("Proprietario", proprietarioId));
        var apartamento = mapper.apartamentoFromId(apartamentoId);
        proprietario.getApartamentos().add(apartamento);
        var saved = repository.save(proprietario);
        log.info("Proprietario {} associated with apartamento {}", proprietarioId, apartamentoId);
        return mapper.toDTO(saved);
    }

    /**
     * Dissociates the proprietario from the given apartment.
     *
     * @param proprietarioId proprietario primary key
     * @param apartamentoId  apartment primary key
     * @return updated proprietario DTO
     * @throws org.springframework.web.server.ResponseStatusException with 404 if the proprietario is not found
     */
    @Transactional
    @Timed(value = "proprietario.service.desassociarApartamento", description = "Dissociate proprietario from apartamento")
    public ProprietarioDTO desassociarApartamento(Long proprietarioId, Long apartamentoId) {
        log.info("Dissociating proprietario {} from apartamento {}", proprietarioId, apartamentoId);
        var proprietario = repository.findById(proprietarioId)
                .orElseThrow(() -> notFound("Proprietario", proprietarioId));
        proprietario.getApartamentos().removeIf(a -> a.getId().equals(apartamentoId));
        var saved = repository.save(proprietario);
        log.info("Proprietario {} dissociated from apartamento {}", proprietarioId, apartamentoId);
        return mapper.toDTO(saved);
    }

    /**
     * Soft-deletes the proprietario with the given id.
     *
     * @param id proprietario primary key
     * @throws org.springframework.web.server.ResponseStatusException with 404 if not found
     */
    @Transactional
    @Timed(value = "proprietario.service.delete", description = "Delete proprietario")
    public void delete(Long id) {
        log.info("Deleting proprietario with id: {}", id);
        Proprietario entity = repository.findById(id)
                .orElseThrow(() -> notFound("Proprietario", id));
        repository.delete(entity);
        log.info("Proprietario soft-deleted successfully: {}", id);
    }
}
