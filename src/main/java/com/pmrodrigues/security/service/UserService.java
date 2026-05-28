package com.pmrodrigues.security.service;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.service.CondominioService;
import com.pmrodrigues.security.dto.ChangePasswordDTO;
import com.pmrodrigues.security.dto.CreateUserDTO;
import com.pmrodrigues.security.dto.UserDTO;
import com.pmrodrigues.security.mapper.UserMapper;
import com.pmrodrigues.security.model.PasswordHistory;
import com.pmrodrigues.security.repository.PasswordHistoryRepository;
import com.pmrodrigues.security.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.pmrodrigues.commons.util.Exceptions.notFound;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Application service for user CRUD operations and the email-based account activation flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MailService mailService;
    private final UserMapper mapper;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final CondominioService condominioService;

    @Value("${app.security.password-history-count:3}")
    private int passwordHistoryCount;

    /**
     * Persists a new user and dispatches an activation email containing the generated temporary password and token.
     *
     * <p>Validates that the email is not already in use (400 if duplicate) and that the referenced
     * condominium exists (400 if not found).
     *
     * @return the saved user as a DTO (includes the generated activation state)
     * @throws org.springframework.web.server.ResponseStatusException 400 if the email is already in use or the condominium does not exist
     */
    @Transactional
    @Timed(value = "user.service.create", description = "Create user")
    public UserDTO create(CreateUserDTO dto) {
        log.info("Creating new user with email: {}", dto.email());
        userRepository.findByEmail(dto.email()).ifPresent(existing -> {
            log.error("Email already in use: {}", dto.email());
            throw new ResponseStatusException(BAD_REQUEST, "Usuário já existe com o email: " + dto.email());
        });
        condominioService.findById(dto.condominioId()).orElseThrow(() -> {
            log.error("Invalid condominioId: {}", dto.condominioId());
            return new ResponseStatusException(BAD_REQUEST, "Condomínio inválido");
        });
        var user = mapper.toEntity(dto);
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>(Set.of("ROLE_USER")));
        }
        var saved = userRepository.save(user);
        mailService.sendActivationEmail(saved.getEmail(), saved.getName(), saved.getRawPassword(), saved.getActivationToken());
        log.info("User created successfully with id: {}", saved.getId());
        return mapper.toDTO(saved);
    }

    /**
     * Retrieves a user by primary key.
     *
     * @return the user DTO, or empty if not found
     */
    @Transactional(readOnly = true)
    @Timed(value = "user.service.findById", description = "Find user by id")
    public Optional<UserDTO> findById(Long id) {
        log.info("Looking up user by id: {}", id);
        var result = userRepository.findById(id).map(mapper::toDTO);
        log.info("User lookup by id {}: {}", id, result.isPresent() ? "found" : "not found");
        return result;
    }

    /**
     * Returns all non-deleted users.
     *
     * @return list of all active users as DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "user.service.findAll", description = "Find all users")
    public List<UserDTO> findAll() {
        log.info("Retrieving all users");
        var users = userRepository.findAll().stream().map(mapper::toDTO).toList();
        log.info("Retrieved {} users", users.size());
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

        mapper.updateEntity(user, dto);
        var saved = userRepository.save(user);
        log.info("User updated successfully: {}", dto.id());
        return mapper.toDTO(saved);
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
        return mapper.toDTO(activated);
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

        var encoder = new BCryptPasswordEncoder();

        if (!encoder.matches(dto.currentPassword(), user.getPassword())) {
            log.error("Invalid current password attempt for user id: {}", userId);
            throw new ResponseStatusException(BAD_REQUEST, "Senha atual inválida");
        }

        if (!dto.newPassword().equals(dto.confirmPassword())) {
            log.error("New password and confirmation mismatch for user id: {}", userId);
            throw new ResponseStatusException(BAD_REQUEST, "Nova senha não confere com a confirmação");
        }

        var history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, passwordHistoryCount));
        boolean reused = history.stream().anyMatch(h -> encoder.matches(dto.newPassword(), h.getPassword()));
        if (reused) {
            log.error("Password reuse attempt for user id: {}", userId);
            throw new ResponseStatusException(BAD_REQUEST, "Senha já utilizada anteriormente");
        }

        passwordHistoryRepository.save(PasswordHistory.builder()
                .userId(userId)
                .password(user.getPassword())
                .build());

        user.setPassword(encoder.encode(dto.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user id: {}", userId);
    }
}