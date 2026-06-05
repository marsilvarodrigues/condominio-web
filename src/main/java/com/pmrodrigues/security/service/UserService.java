package com.pmrodrigues.security.service;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.service.CondominioService;
import com.pmrodrigues.security.dto.ChangePasswordDTO;
import com.pmrodrigues.security.dto.CreateUserDTO;
import com.pmrodrigues.security.dto.UserDTO;
import com.pmrodrigues.security.dto.UserFilterDTO;
import com.pmrodrigues.security.mapper.UserMapper;
import com.pmrodrigues.security.model.PasswordHistory;
import com.pmrodrigues.security.model.User;
import com.pmrodrigues.security.repository.PasswordHistoryRepository;
import com.pmrodrigues.security.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pmrodrigues.condominio.model.Condominio;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.pmrodrigues.security.specification.UserSpecification.hasEmail;
import static com.pmrodrigues.security.specification.UserSpecification.hasEnabled;
import static com.pmrodrigues.security.specification.UserSpecification.hasNome;
import static com.pmrodrigues.security.specification.UserSpecification.hasRole;
import org.springframework.data.jpa.domain.Specification;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Application service for user CRUD operations and the email-based account activation flow.
 * Subclasses ({@link com.pmrodrigues.morador.service.PessoaService},
 * {@link com.pmrodrigues.morador.service.ProprietarioService}) extend this service to reuse
 * the user-creation ceremony via {@link #persistUser}.
 */
@Slf4j
@Service
@Primary
public class UserService {

    protected final UserRepository userRepository;
    protected final MailService mailService;
    protected final UserMapper userMapper;
    protected final PasswordHistoryRepository passwordHistoryRepository;
    protected final CondominioService condominioService;
    protected final PasswordEncoder passwordEncoder;

    @Value("${app.security.password-history-count:3}")
    private int passwordHistoryCount;

    public UserService(UserRepository userRepository,
                       MailService mailService,
                       UserMapper userMapper,
                       PasswordHistoryRepository passwordHistoryRepository,
                       CondominioService condominioService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.userMapper = userMapper;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.condominioService = condominioService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Saves a new user entity, handling email uniqueness, condominium associations, and
     * activation email dispatch. Intended to be called by subclasses in their own {@code create}
     * methods after building the concrete entity.
     *
     * @param entity        new user entity (any subtype of {@link User})
     * @param email         the user's email — checked for uniqueness
     * @param condominioIds optional set of condominium IDs to associate; null means global access
     * @return the saved entity
     * @throws ResponseStatusException 400 if the email is already in use or a condominium does not exist
     */
    protected User persistUser(User entity, String email, Set<Long> condominioIds) {
        log.info("Persisting new user with email: {}", email);
        userRepository.findByEmail(email).ifPresent(existing -> {
            log.error("Email already in use: {}", email);
            throw new ResponseStatusException(BAD_REQUEST, "Não foi possível criar o usuário com os dados informados");
        });
        var condominios = loadCondominioEntities(condominioIds);
        if (entity.getRoles() == null) {
            entity.setRoles(new HashSet<>(Set.of("ROLE_USER")));
        }
        entity.setCondominios(condominios);
        var saved = userRepository.save(entity);
        mailService.sendActivationEmail(saved.getEmail(), saved.getName(), saved.getRawPassword(), saved.getActivationToken());
        log.info("User persisted successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Persists a new user and dispatches an activation email containing the generated temporary password and token.
     *
     * <p>Validates that the email is not already in use (400 if duplicate) and that all referenced
     * condominiums exist (400 if any is not found). An empty or null {@code condominioIds} creates a
     * user with global access.
     *
     * @return the saved user as a DTO (includes the generated activation state)
     * @throws org.springframework.web.server.ResponseStatusException 400 if the email is already in use or any condominium does not exist
     */
    @Transactional
    @Timed(value = "user.service.create", description = "Create user")
    public UserDTO create(CreateUserDTO dto) {
        log.info("Creating new user with email: {}", dto.email());
        var user = userMapper.toEntity(dto);
        var saved = persistUser(user, dto.email(), dto.condominioIds());
        log.info("User created successfully with id: {}", saved.getId());
        return userMapper.toDTO(saved);
    }

    /**
     * Retrieves a user by primary key.
     *
     * @return the user DTO, or empty if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "user.service.findById", description = "Find user by id")
    public Optional<UserDTO> findUserById(Long id) {
        log.info("Looking up user by id: {}", id);
        var result = userRepository.findById(id).map(userMapper::toDTO);
        log.info("User lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Returns users matching the supplied filter criteria. All filter fields are optional; absent fields are ignored.
     *
     * @param dto filter containing optional nome substring, email substring, enabled flag, and role
     * @return matching user DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "user.service.filterBy", description = "Filter users")
    public List<UserDTO> filterBy(UserFilterDTO dto) {
        log.info("Filtering users: nome={}, email={}, enabled={}, role={}", dto.nome(), dto.email(), dto.enabled(), dto.role());
        var users = userRepository.findAll(Specification.allOf(hasNome(dto.nome()), hasEmail(dto.email()), hasEnabled(dto.enabled()), hasRole(dto.role())))
                .stream().map(userMapper::toDTO).toList();
        log.info("Filter users: {} results", users.size());
        return users;
    }

    /**
     * Applies DTO changes to an existing user and persists the result.
     *
     * <p>Admins may update any user. Non-admins may only update their own data;
     * attempting to update another user's data raises a 403 Forbidden.
     *
     * @return the updated user as a DTO
     * @throws com.pmrodrigues.commons.util.Exceptions if the user does not exist
     * @throws org.springframework.web.server.ResponseStatusException 403 if a non-admin tries to update another user
     */
    @Transactional
    @Timed(value = "user.service.update", description = "Update user")
    public UserDTO update(UserDTO dto) {
        log.info("Updating user with id: {}", dto.id());
        var user = userRepository.findById(dto.id())
                .orElseThrow(() -> notFound("User", dto.id()));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !user.getEmail().equals(auth.getName())) {
            log.error("User {} attempted to update data of user id: {}", auth.getName(), dto.id());
            throw new ResponseStatusException(FORBIDDEN, "Acesso negado");
        }

        userMapper.updateEntity(user, dto);
        if (dto.condominioIds() != null) {
            user.setCondominios(loadCondominioEntities(dto.condominioIds()));
        }
        var saved = userRepository.save(user);
        log.info("User updated successfully: {}", dto.id());
        return userMapper.toDTO(saved);
    }

    /**
     * Soft-deletes a user by id.
     *
     * @throws com.pmrodrigues.commons.util.Exceptions if the user does not exist
     */
    @Transactional
    @Timed(value = "user.service.delete", description = "Delete user")
    public void delete(Long id) {
        log.info("Deleting user with id: {}", id);
        var user = userRepository.findById(id)
                .orElseThrow(() -> notFound("User", id));
        userRepository.delete(user);
        log.info("User soft-deleted successfully: {}", id);
    }

    /**
     * Validates the activation token and its expiry, enables the account, and clears the token.
     * The temporary password is kept unchanged; the user must change it after logging in.
     *
     * @param activationToken the one-time token sent to the user's email
     * @return the now-enabled user as a DTO
     * @throws org.springframework.web.server.ResponseStatusException with 404 for an unknown token, or 400 for an expired token
     */
    @Transactional
    @Timed(value = "user.service.activateAccount", description = "Activate user account")
    public UserDTO activateAccount(String activationToken) {
        log.info("Activating account with token");
        var user = userRepository.findByActivationToken(activationToken)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Invalid activation token"));

        if (user.getActivationTokenExpiry().isBefore(LocalDateTime.now())) {
            log.error("Activation token expired for user: {}", user.getEmail());
            throw new ResponseStatusException(BAD_REQUEST, "Activation token has expired");
        }

        user.setEnabled(true);
        user.setActivationToken(null);
        user.setActivationTokenExpiry(null);

        var activated = userRepository.save(user);
        log.info("Account activated successfully for user: {}", activated.getEmail());
        return userMapper.toDTO(activated);
    }

    /**
     * Changes the password for an existing user after verifying the current password,
     * confirming the new password matches its confirmation, and checking that the new
     * password has not been used in the last {@code app.security.password-history-count} changes.
     *
     * @param userId the user's primary key
     * @param dto    contains current, new and confirmation passwords
     * @throws ResponseStatusException 404 if user not found; 400 for any business rule violation
     */
    @Transactional
    @Timed(value = "user.service.changePassword", description = "Change user password")
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        log.info("Changing password for user id: {}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> notFound("User", userId));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !user.getEmail().equals(auth.getName())) {
            log.error("User {} attempted to change password of user id: {}", auth.getName(), userId);
            throw new ResponseStatusException(FORBIDDEN, "Acesso negado");
        }

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            log.error("Invalid current password attempt for user id: {}", userId);
            throw new ResponseStatusException(BAD_REQUEST, "Senha atual inválida");
        }

        if (!dto.newPassword().equals(dto.confirmPassword())) {
            log.error("New password and confirmation mismatch for user id: {}", userId);
            throw new ResponseStatusException(BAD_REQUEST, "Nova senha não confere com a confirmação");
        }

        var history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, passwordHistoryCount));
        boolean reused = history.stream().anyMatch(h -> passwordEncoder.matches(dto.newPassword(), h.getPassword()));
        if (reused) {
            log.error("Password reuse attempt for user id: {}", userId);
            throw new ResponseStatusException(BAD_REQUEST, "Senha já utilizada anteriormente");
        }

        passwordHistoryRepository.save(PasswordHistory.builder()
                .userId(userId)
                .password(user.getPassword())
                .build());

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user id: {}", userId);
    }

    protected Set<Condominio> loadCondominioEntities(Set<Long> condominioIds) {
        if (condominioIds == null || condominioIds.isEmpty()) return new HashSet<>();
        var result = new HashSet<Condominio>();
        for (Long condId : condominioIds) {
            var entity = condominioService.findEntityById(condId).orElseThrow(() -> {
                log.error("Invalid condominioId: {}", condId);
                return new ResponseStatusException(BAD_REQUEST, "Condomínio inválido: " + condId);
            });
            result.add(entity);
        }
        return result;
    }
}
