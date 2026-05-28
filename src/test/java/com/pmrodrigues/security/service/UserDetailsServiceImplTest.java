package com.pmrodrigues.security.service;

import com.pmrodrigues.security.model.User;
import com.pmrodrigues.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_whenUserExists_returnsUserDetails() {
        var user = buildUser("user@test.com", "hashed-password", true, Set.of("ROLE_USER"));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("user@test.com");

        assertThat(details.getUsername()).isEqualTo("user@test.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_whenUserNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@test.com");
    }

    @Test
    void loadUserByUsername_whenUserIsDisabled_returnsDisabledUserDetails() {
        var user = buildUser("disabled@test.com", "pass", false, Set.of("ROLE_USER"));
        when(userRepository.findByEmail("disabled@test.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("disabled@test.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_withMultipleRoles_mapsAllAuthorities() {
        var user = buildUser("admin@test.com", "pass", true, Set.of("ROLE_USER", "ROLE_ADMIN"));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("admin@test.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_withEmptyRoles_returnsUserDetailsWithNoAuthorities() {
        var user = buildUser("user@test.com", "pass", true, Set.of());
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("user@test.com");

        assertThat(details.getAuthorities()).isEmpty();
    }

    private User buildUser(String email, String password, boolean enabled, Set<String> roles) {
        return new User()
        .setEmail(email)
        .setPassword(password)
        .setName("Test User")
        .setEnabled(enabled)
        .setRoles(roles);

    }
}
