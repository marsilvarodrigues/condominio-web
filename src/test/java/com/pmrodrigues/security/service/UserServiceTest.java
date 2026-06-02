package com.pmrodrigues.security.service;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.model.Condominio;
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
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock MailService mailService;
    @Mock UserMapper mapper;
    @Mock PasswordHistoryRepository passwordHistoryRepository;
    @Mock CondominioService condominioService;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    UserService userService;

    private static final CondominioDTO TEST_CONDOMINIO =
            new CondominioDTO(1L, "Test Cond", "12.345.678/0001-90", "cond@test.com", null, null, null);

    private static final Condominio TEST_CONDOMINIO_ENTITY;
    static {
        TEST_CONDOMINIO_ENTITY = new Condominio();
        TEST_CONDOMINIO_ENTITY.setId(1L);
    }

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, mailService, mapper, passwordHistoryRepository, condominioService, ENCODER);
        ReflectionTestUtils.setField(userService, "passwordHistoryCount", 3);

        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().when(condominioService.findById(any())).thenReturn(Optional.of(TEST_CONDOMINIO));
        lenient().when(condominioService.findEntityById(any())).thenReturn(Optional.of(TEST_CONDOMINIO_ENTITY));

        lenient().when(mapper.toEntity(any(CreateUserDTO.class))).thenAnswer(inv -> {
            CreateUserDTO d = inv.getArgument(0);
            var u = new User();
            u.setEmail(d.email());
            u.setName(d.name());
            if (d.roles() != null) u.setRoles(new HashSet<>(d.roles()));
            return u;
        });
        lenient().when(mapper.toEntity(any(UserDTO.class))).thenAnswer(inv -> {
            UserDTO d = inv.getArgument(0);
            var u = new User();
            if (d.id() != null) u.setId(d.id());
            u.setEmail(d.email());
            u.setName(d.name());
            u.setEnabled(d.enabled());
            if (d.roles() != null) u.setRoles(new HashSet<>(d.roles()));
            return u;
        });
        lenient().when(mapper.toDTO(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            Set<Long> condIds = u.getCondominios() != null
                    ? u.getCondominios().stream().map(Condominio::getId).collect(java.util.stream.Collectors.toSet())
                    : Set.of();
            return new UserDTO(u.getId(), u.getEmail(), u.getName(), u.isEnabled(),
                    u.getRoles(), condIds, u.getCreatedAt(), u.getUpdatedAt());
        });
        lenient().doAnswer(inv -> {
            User u = inv.getArgument(0);
            UserDTO d = inv.getArgument(1);
            u.setEmail(d.email());
            u.setName(d.name());
            u.setEnabled(d.enabled());
            if (d.roles() != null) u.setRoles(new HashSet<>(d.roles()));
            return null;
        }).when(mapper).updateEntity(any(User.class), any(UserDTO.class));
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_savesUserAndSendsActivationEmail() {
        var dto = new CreateUserDTO("new@test.com", "New User", Set.of("ROLE_USER"), Set.of(1L));

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.prePersist();
            return u;
        });

        var result = userService.create(dto);

        verify(userRepository).save(any(User.class));
        verify(mailService).sendActivationEmail(
                eq("new@test.com"),
                any(String.class),
                any(String.class),
                any(String.class)
        );
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void create_returnsPersistedUserDTO() {
        var dto = new CreateUserDTO("new@test.com", "New User", Set.of("ROLE_USER"), Set.of(1L));

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.prePersist();
            return u;
        });

        var result = userService.create(dto);

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.name()).isEqualTo("New User");
    }

    @Test
    void create_withExistingEmail_throwsBadRequest() {
        var dto = new CreateUserDTO("dup@test.com", "Dup User", Set.of("ROLE_USER"), Set.of(1L));
        when(userRepository.findByEmail("dup@test.com")).thenReturn(Optional.of(userWithId(10L, "dup@test.com")));

        assertThatThrownBy(() -> userService.create(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(BAD_REQUEST.value());

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_withInvalidCondominioId_throwsBadRequest() {
        var dto = new CreateUserDTO("user@test.com", "New User", Set.of("ROLE_USER"), Set.of(99L));
        when(condominioService.findEntityById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(BAD_REQUEST.value());

        verify(userRepository, never()).save(any());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returnsUserDTO() {
        var user = userWithId(1L, "test@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var result = userService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("test@test.com");
    }

    @Test
    void findById_whenNotFound_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        var result = userService.findById(99L);

        assertThat(result).isEmpty();
    }

    // ── filterBy ─────────────────────────────────────────────────────────

    @Test
    void filterBy_withNoFilters_returnsAll() {
        var users = List.of(userWithId(1L, "a@test.com"), userWithId(2L, "b@test.com"));
        when(userRepository.findAll(any(Specification.class))).thenReturn(users);

        var result = userService.filterBy(new UserFilterDTO(null, null, null, null));

        assertThat(result).hasSize(2);
        verify(userRepository).findAll(any(Specification.class));
    }

    @Test
    void filterBy_withEnabledTrue_returnsOnlyActiveUsers() {
        var active = userWithId(1L, "active@test.com");
        active.setEnabled(true);
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(active));

        var result = userService.filterBy(new UserFilterDTO(null, null, true, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).enabled()).isTrue();
    }

    @Test
    void filterBy_withRole_returnsMatchingUsers() {
        var admin = userWithId(1L, "admin@test.com");
        admin.setRoles(Set.of("ROLE_ADMIN"));
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(admin));

        var result = userService.filterBy(new UserFilterDTO(null, null, null, "ROLE_ADMIN"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).roles()).contains("ROLE_ADMIN");
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_whenFound_updatesNameEmailAndRoles() {
        var existing = userWithId(1L, "old@test.com");
        existing.setName("Old Name");
        authenticatedAs("admin@test.com", "ROLE_ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var updates = new UserDTO(1L, "new@test.com", "New Name", false, Set.of("ROLE_ADMIN"), null, null, null);

        var result = userService.update(updates);

        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.enabled()).isFalse();
        assertThat(result.roles()).containsExactly("ROLE_ADMIN");
        verify(userRepository).save(existing);
    }

    @Test
    void update_whenNotFound_throwsNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        var dto = new UserDTO(99L, "x@test.com", "X", false, null, null, null, null);

        assertThatThrownBy(() -> userService.update(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    @Test
    void update_byNonAdminForAnotherUser_throwsForbidden() {
        var targetUser = userWithId(2L, "target@test.com");
        authenticatedAs("attacker@test.com", "ROLE_USER");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        var dto = new UserDTO(2L, "target@test.com", "Hacked Name", true, null, null, null, null);

        assertThatThrownBy(() -> userService.update(dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(FORBIDDEN.value());

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_byNonAdminForOwnData_succeeds() {
        var existing = userWithId(1L, "user@test.com");
        existing.setName("Old Name");
        authenticatedAs("user@test.com", "ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var updates = new UserDTO(1L, "user@test.com", "New Name", true, Set.of("ROLE_USER"), null, null, null);

        var result = userService.update(updates);

        assertThat(result.name()).isEqualTo("New Name");
        verify(userRepository).save(existing);
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_whenFound_callsRepositoryDelete() {
        var user = userWithId(1L, "test@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_whenNotFound_throwsNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    // ── activateAccount ───────────────────────────────────────────────────

    @Test
    void activateAccount_withValidToken_enablesUserAndClearsToken() {
        var user = userWithId(1L, "test@test.com");
        user.setActivationToken("valid-token");
        user.setActivationTokenExpiry(LocalDateTime.now().plusHours(1));
        user.setEnabled(false);

        when(userRepository.findByActivationToken("valid-token")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = userService.activateAccount("valid-token");

        assertThat(result.enabled()).isTrue();
        assertThat(user.getActivationToken()).isNull();
        assertThat(user.getActivationTokenExpiry()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void activateAccount_withExpiredToken_throwsBadRequest() {
        var user = userWithId(1L, "test@test.com");
        user.setActivationToken("expired-token");
        user.setActivationTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByActivationToken("expired-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.activateAccount("expired-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(BAD_REQUEST.value());
    }

    @Test
    void activateAccount_withInvalidToken_throwsNotFoundException() {
        when(userRepository.findByActivationToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateAccount("bad-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    void changePassword_withValidData_updatesPassword() {
        var user = userWithId(1L, "test@test.com");
        String currentHash = ENCODER.encode("CurrentPass@1");
        user.setPassword(currentHash);
        authenticatedAs("test@test.com", "ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new ChangePasswordDTO("CurrentPass@1", "NewPass@123", "NewPass@123");
        userService.changePassword(1L, dto);

        verify(passwordHistoryRepository).save(any(PasswordHistory.class));
        verify(userRepository).save(user);
        assertThat(ENCODER.matches("NewPass@123", user.getPassword())).isTrue();
    }

    @Test
    void changePassword_withWrongCurrentPassword_throwsBadRequest() {
        var user = userWithId(1L, "test@test.com");
        user.setPassword(ENCODER.encode("CorrectPass@1"));
        authenticatedAs("test@test.com", "ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var dto = new ChangePasswordDTO("WrongPass@1", "NewPass@123", "NewPass@123");

        assertThatThrownBy(() -> userService.changePassword(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(BAD_REQUEST.value());
    }

    @Test
    void changePassword_withMismatchedConfirmation_throwsBadRequest() {
        var user = userWithId(1L, "test@test.com");
        user.setPassword(ENCODER.encode("CurrentPass@1"));
        authenticatedAs("test@test.com", "ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var dto = new ChangePasswordDTO("CurrentPass@1", "NewPass@123", "DifferentPass@123");

        assertThatThrownBy(() -> userService.changePassword(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(BAD_REQUEST.value());
    }

    @Test
    void changePassword_withReusedPassword_throwsBadRequest() {
        var user = userWithId(1L, "test@test.com");
        user.setPassword(ENCODER.encode("CurrentPass@1"));
        authenticatedAs("test@test.com", "ROLE_USER");

        String previousHash = ENCODER.encode("NewPass@123");
        var history = List.of(PasswordHistory.builder().userId(1L).password(previousHash).build());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(history);

        var dto = new ChangePasswordDTO("CurrentPass@1", "NewPass@123", "NewPass@123");

        assertThatThrownBy(() -> userService.changePassword(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(BAD_REQUEST.value());
    }

    @Test
    void changePassword_withUserNotFound_throwsNotFoundException() {
        authenticatedAs("other@test.com", "ROLE_USER");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        var dto = new ChangePasswordDTO("any", "NewPass@123", "NewPass@123");

        assertThatThrownBy(() -> userService.changePassword(99L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    @Test
    void changePassword_byNonAdminForAnotherUser_throwsForbidden() {
        var targetUser = userWithId(2L, "target@test.com");
        authenticatedAs("attacker@test.com", "ROLE_USER");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        var dto = new ChangePasswordDTO("any", "NewPass@123", "NewPass@123");

        assertThatThrownBy(() -> userService.changePassword(2L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(FORBIDDEN.value());
    }

    @Test
    void changePassword_byAdminForAnotherUser_succeeds() {
        var targetUser = userWithId(2L, "target@test.com");
        String currentHash = ENCODER.encode("CurrentPass@1");
        targetUser.setPassword(currentHash);
        authenticatedAs("admin@test.com", "ROLE_ADMIN");

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(eq(2L), any())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new ChangePasswordDTO("CurrentPass@1", "NewPass@123", "NewPass@123");
        userService.changePassword(2L, dto);

        verify(userRepository).save(targetUser);
    }

    @Test
    void changePassword_savesCurrentPasswordToHistoryBeforeUpdating() {
        var user = userWithId(1L, "test@test.com");
        String currentHash = ENCODER.encode("CurrentPass@1");
        user.setPassword(currentHash);
        authenticatedAs("test@test.com", "ROLE_USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new ChangePasswordDTO("CurrentPass@1", "NewPass@123", "NewPass@123");
        userService.changePassword(1L, dto);

        verify(passwordHistoryRepository).save(argThat(h ->
                h.getUserId().equals(1L) && h.getPassword().equals(currentHash)));
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void authenticatedAs(String email, String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private User userWithId(Long id, String email) {
        var user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test User");
        user.setPassword("encoded-password");
        user.setEnabled(true);
        return user;
    }
}