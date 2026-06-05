package com.pmrodrigues.morador.service;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.service.CondominioService;
import com.pmrodrigues.morador.dto.CreatePessoaDTO;
import com.pmrodrigues.morador.dto.PessoaDTO;
import com.pmrodrigues.morador.dto.PessoaFilterDTO;
import com.pmrodrigues.morador.dto.UpdatePessoaDTO;
import com.pmrodrigues.morador.mapper.PessoaMapper;
import com.pmrodrigues.morador.model.Pessoa;
import com.pmrodrigues.morador.repository.PessoaRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static com.pmrodrigues.morador.specification.PessoaSpecification.hasCpf;
import static com.pmrodrigues.morador.specification.PessoaSpecification.hasEmail;
import static com.pmrodrigues.morador.specification.PessoaSpecification.hasNome;
import static com.pmrodrigues.morador.specification.PessoaSpecification.hasTipo;

/**
 * Application service for managing {@code Pessoa} entities, with soft-delete and multi-tenancy support.
 * Extends {@link UserService} to reuse user-creation ceremony (email uniqueness check, activation email dispatch).
 */
@Slf4j
@Service
public class PessoaService extends UserService {

    private final PessoaRepository pessoaRepository;
    private final PessoaMapper pessoaMapper;
    private final ProprietarioRepository proprietarioRepository;

    public PessoaService(UserRepository userRepository,
                         MailService mailService,
                         UserMapper userMapper,
                         PasswordHistoryRepository passwordHistoryRepository,
                         CondominioService condominioService,
                         PasswordEncoder passwordEncoder,
                         PessoaRepository pessoaRepository,
                         PessoaMapper pessoaMapper,
                         ProprietarioRepository proprietarioRepository) {
        super(userRepository, mailService, userMapper, passwordHistoryRepository, condominioService, passwordEncoder);
        this.pessoaRepository = pessoaRepository;
        this.pessoaMapper = pessoaMapper;
        this.proprietarioRepository = proprietarioRepository;
    }

    /**
     * Returns a paginated list of pessoas matching the supplied filter criteria.
     *
     * @param filter   optional filter parameters; absent fields are ignored
     * @param pageable pagination and sort parameters
     * @return page of matching pessoa DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "pessoa.service.filterBy", description = "Filter pessoas")
    public Page<PessoaDTO> filterBy(PessoaFilterDTO filter, Pageable pageable) {
        log.info("Filtering pessoas: nome={}, tipo={}, cpf={}, email={}",
                filter.nome(), filter.tipo(), filter.cpf(), filter.email());
        var spec = Specification.allOf(
                hasNome(filter.nome()),
                hasTipo(filter.tipo()),
                hasCpf(filter.cpf()),
                hasEmail(filter.email()));
        var result = pessoaRepository.findAll(spec, pageable).map(pessoaMapper::toDTO);
        log.info("Filter pessoas: {} results (page {}/{})", result.getNumberOfElements(),
                result.getNumber(), result.getTotalPages());
        return result;
    }

    /**
     * Looks up a single pessoa by its primary key.
     *
     * @param id pessoa primary key
     * @return the DTO for the found entity
     * @throws ResponseStatusException with 404 if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "pessoa.service.findById", description = "Find pessoa by id")
    public PessoaDTO findById(Long id) {
        log.info("Looking up pessoa by id: {}", id);
        var result = pessoaRepository.findById(id)
                .map(pessoaMapper::toDTO)
                .orElseThrow(() -> notFound("Pessoa", id));
        log.info("Pessoa lookup by id {}: found", id);
        return result;
    }

    /**
     * Persists a new Morador, delegating the user-creation ceremony to {@link #persistUser}.
     * The Morador entity is built by the mapper and enriched with an optional apartment assignment.
     *
     * @param dto creation payload
     * @return the persisted pessoa as a DTO
     */
    @Transactional
    @Timed(value = "pessoa.service.create", description = "Create pessoa")
    public PessoaDTO create(CreatePessoaDTO dto) {
        log.info("Creating pessoa: nome={}", dto.nome());
        var entity = (Pessoa) pessoaMapper.toEntity(dto);
        entity.setApartamento(pessoaMapper.apartamentoFromId(dto.apartamentoId()));
        var saved = (Pessoa) persistUser(entity, dto.email(), null);
        log.info("Pessoa created successfully with id: {}", saved.getId());
        return pessoaMapper.toDTO(saved);
    }

    /**
     * Applies changes from the DTO to an existing pessoa entity and saves it.
     *
     * @param id  pessoa primary key
     * @param dto updated data
     * @return the updated pessoa as a DTO
     * @throws ResponseStatusException with 404 if not found
     */
    @Transactional
    @Timed(value = "pessoa.service.update", description = "Update pessoa")
    public PessoaDTO update(Long id, UpdatePessoaDTO dto) {
        log.info("Updating pessoa with id: {}", id);
        var entity = pessoaRepository.findById(id)
                .orElseThrow(() -> notFound("Pessoa", id));
        pessoaMapper.updateEntity(entity, dto);
        var saved = pessoaRepository.save(entity);
        log.info("Pessoa updated successfully: {}", id);
        return pessoaMapper.toDTO(saved);
    }

    /**
     * Soft-deletes the pessoa with the given id.
     * Refuses deletion when the person is currently assigned to an apartment or has active ownership.
     *
     * @param id primary key of the pessoa to delete
     * @throws ResponseStatusException with 404 if not found
     * @throws ResponseStatusException with 409 if the person has an active assignment or ownership
     */
    @Transactional
    @Timed(value = "pessoa.service.delete", description = "Delete pessoa")
    public void delete(Long id) {
        log.info("Deleting pessoa with id: {}", id);
        var entity = pessoaRepository.findById(id)
                .orElseThrow(() -> notFound("Pessoa", id));

        if (entity.getApartamento() != null) {
            log.error("Cannot delete pessoa {}: is morador of apartamento {}", id,
                    entity.getApartamento().getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pessoa tem ocupação ativa no apartamento " + entity.getApartamento().getId());
        }

        proprietarioRepository.findByApartamentosId(id).stream().findAny().ifPresent(p -> {
            log.error("Cannot delete pessoa {}: has active ownership", id);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pessoa tem propriedade ativa em um apartamento");
        });

        pessoaRepository.delete(entity);
        log.info("Pessoa soft-deleted successfully: {}", id);
    }

    /**
     * Soft-deletes all pessoas belonging to the given condominio.
     * Used by {@code CondominioService.delete()} to cascade the deletion.
     *
     * @param condominioId condominio primary key
     */
    @Transactional
    @Timed(value = "pessoa.service.softDeleteByCondominioId", description = "Soft delete all pessoas by condominio")
    public void softDeleteByCondominioId(Long condominioId) {
        log.info("Soft-deleting all pessoas for condominio {}", condominioId);
        pessoaRepository.softDeleteByCondominioId(condominioId);
        log.info("Soft-deleted all pessoas for condominio {}", condominioId);
    }

    /**
     * Assigns the pessoa as morador to the given apartment.
     *
     * @param pessoaId      pessoa primary key
     * @param apartamentoId apartment primary key
     * @return updated pessoa DTO
     * @throws ResponseStatusException with 404 if the pessoa is not found
     */
    @Transactional
    @Timed(value = "pessoa.service.assignToApartamento", description = "Assign pessoa to apartamento")
    public PessoaDTO assignToApartamento(Long pessoaId, Long apartamentoId) {
        log.info("Assigning pessoa {} to apartamento {}", pessoaId, apartamentoId);
        var entity = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> notFound("Pessoa", pessoaId));
        entity.setApartamento(pessoaMapper.apartamentoFromId(apartamentoId));
        var saved = pessoaRepository.save(entity);
        log.info("Pessoa {} assigned to apartamento {}", pessoaId, apartamentoId);
        return pessoaMapper.toDTO(saved);
    }

    /**
     * Removes the pessoa from their current apartment assignment.
     *
     * @param pessoaId pessoa primary key
     * @return updated pessoa DTO
     * @throws ResponseStatusException with 404 if the pessoa is not found
     */
    @Transactional
    @Timed(value = "pessoa.service.removeFromApartamento", description = "Remove pessoa from apartamento")
    public PessoaDTO removeFromApartamento(Long pessoaId) {
        log.info("Removing pessoa {} from apartamento", pessoaId);
        var entity = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> notFound("Pessoa", pessoaId));
        entity.setApartamento(null);
        var saved = pessoaRepository.save(entity);
        log.info("Pessoa {} removed from apartamento", pessoaId);
        return pessoaMapper.toDTO(saved);
    }
}
