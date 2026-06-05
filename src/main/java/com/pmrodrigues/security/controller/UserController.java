package com.pmrodrigues.security.controller;

import com.pmrodrigues.commons.dto.ApiResponse;
import com.pmrodrigues.security.dto.ChangePasswordDTO;
import com.pmrodrigues.security.dto.CreateUserDTO;
import com.pmrodrigues.security.dto.UserDTO;
import com.pmrodrigues.security.dto.UserFilterDTO;
import com.pmrodrigues.security.service.UserService;
import com.pmrodrigues.commons.versioning.ApiVersion;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.pmrodrigues.commons.util.RequestContextHelper.requestId;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller for user CRUD operations. Create and delete require the {@code ADMIN} role;
 * update is open to all authenticated users but the service enforces owner-only access for non-admins.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns users matching optional filter criteria.
     *
     * @param dto optional filters: nome substring, email substring, enabled flag, role
     * @return {@code 200} with a list of {@link UserDTO}
     */
    @GetMapping
    @Timed(value = "user.controller.findAll", description = "Find all users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> findAll(@ModelAttribute UserFilterDTO dto, HttpServletRequest request) {
        log.info("GET /users - filtering users");
        var users = userService.filterBy(dto);
        log.info("GET /users - returning {} users", users.size());
        return ResponseEntity.ok(ApiResponse.of(requestId(request), users));
    }

    /**
     * Returns a single user by primary key.
     *
     * @param id the user's primary key
     * @return {@code 200} with the {@link UserDTO}, or {@code 404} if not found
     */
    @GetMapping("/{id}")
    @Timed(value = "user.controller.findById", description = "Find user by id")
    public ResponseEntity<ApiResponse<UserDTO>> findById(@PathVariable Long id, HttpServletRequest request) {
        log.info("GET /users/{} - finding user by id", id);
        var user = userService.findUserById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found: " + id));
        log.info("GET /users/{} - user found", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), user));
    }

    /**
     * Creates a new user; requires the {@code ADMIN} role.
     *
     * @param dto validated creation data: email, name, roles and condominioId — all mandatory
     * @return {@code 201} with the persisted {@link UserDTO}, or {@code 400} if the email is already in use or the condominium does not exist
     */
    @PostMapping
    @Timed(value = "user.controller.create", description = "Create user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> create(@RequestBody @Valid CreateUserDTO dto, HttpServletRequest request) {
        log.info("POST /users - creating user with email: {}", dto.email());
        var created = userService.create(dto);
        log.info("POST /users - user created with id: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(requestId(request), created));
    }

    /**
     * Updates an existing user. Admins may update any user; non-admins may only update their own data.
     *
     * @param id  the user's primary key, which overrides any {@code id} in the request body
     * @param dto validated replacement user data
     * @return {@code 200} with the updated {@link UserDTO}, or {@code 403} if a non-admin targets another user
     */
    @PutMapping("/{id}")
    @Timed(value = "user.controller.update", description = "Update user")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #dto.email() == authentication.name")
    public ResponseEntity<ApiResponse<UserDTO>> update(@PathVariable Long id,
                                                       @RequestBody @Valid UserDTO dto,
                                                       HttpServletRequest request) {
        log.info("PUT /users/{} - updating user", id);
        var updated = userService.update(new UserDTO(id, dto.email(), dto.name(), dto.enabled(),
                dto.roles(), dto.condominioIds(), dto.createdAt(), dto.updatedAt()));
        log.info("PUT /users/{} - user updated", id);
        return ResponseEntity.ok(ApiResponse.of(requestId(request), updated));
    }

    /**
     * Soft-deletes a user by primary key; requires the {@code ADMIN} role.
     *
     * @param id the user's primary key
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    @Timed(value = "user.controller.delete", description = "Delete user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /users/{} - deleting user", id);
        userService.delete(id);
        log.info("DELETE /users/{} - user deleted", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the authenticated user's password; requires the current password for verification.
     *
     * @param id  the user's primary key
     * @param dto contains current, new and confirmation passwords
     * @return {@code 204 No Content} on success, {@code 400} on any business rule violation
     */
    @PatchMapping("/{id}/password")
    @Timed(value = "user.controller.changePassword", description = "Change user password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @RequestBody @Valid ChangePasswordDTO dto) {
        log.info("PATCH /users/{}/password - changing password", id);
        userService.changePassword(id, dto);
        log.info("PATCH /users/{}/password - password changed successfully", id);
        return ResponseEntity.noContent().build();
    }

}
