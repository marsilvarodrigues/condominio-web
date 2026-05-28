package com.pmrodrigues.security.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    // ── prePersist — password without existing password ───────────────────

    @Test
    void prePersist_whenPasswordIsNull_generatesRawAndEncodedPassword() {
        var user = new User();
        user.prePersist();

        assertThat(user.getRawPassword()).isNotBlank();
        assertThat(user.getPassword()).isNotBlank();
        assertThat(user.getPassword()).isNotEqualTo(user.getRawPassword());
        assertThat(new BCryptPasswordEncoder().matches(user.getRawPassword(), user.getPassword())).isTrue();
    }

    @Test
    void prePersist_whenPasswordIsNull_setsActivationToken() {
        var user = new User();
        user.prePersist();

        assertThat(user.getActivationToken()).isNotBlank();
    }

    @Test
    void prePersist_whenPasswordIsNull_setsActivationTokenExpiry24HoursInFuture() {
        var before = LocalDateTime.now();
        var user = new User();
        user.prePersist();
        var after = LocalDateTime.now();

        assertThat(user.getActivationTokenExpiry())
                .isAfterOrEqualTo(before.plusHours(24))
                .isBeforeOrEqualTo(after.plusHours(24));
    }

    @Test
    void prePersist_whenPasswordIsNull_disablesAccount() {
        var user = new User();
        user.setEnabled(true);
        user.prePersist();

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void prePersist_generatesUniqueTokensOnEachCall() {
        var u1 = new User();
        var u2 = new User();
        u1.prePersist();
        u2.prePersist();

        assertThat(u1.getActivationToken()).isNotEqualTo(u2.getActivationToken());
        assertThat(u1.getRawPassword()).isNotEqualTo(u2.getRawPassword());
    }

    // ── prePersist — password already set ─────────────────────────────────

    @Test
    void prePersist_whenPasswordAlreadySet_doesNotOverridePassword() {
        var user = new User();
        user.setPassword("existing-encoded-password");
        user.prePersist();

        assertThat(user.getPassword()).isEqualTo("existing-encoded-password");
        assertThat(user.getRawPassword()).isNull();
    }

    @Test
    void prePersist_whenPasswordAlreadySet_doesNotSetActivationToken() {
        var user = new User();
        user.setPassword("existing-encoded-password");
        user.prePersist();

        assertThat(user.getActivationToken()).isNull();
        assertThat(user.getActivationTokenExpiry()).isNull();
    }

    @Test
    void prePersist_whenPasswordAlreadySet_doesNotChangeEnabled() {
        var user = new User();
        user.setPassword("existing-encoded-password");
        user.setEnabled(true);
        user.prePersist();

        assertThat(user.isEnabled()).isTrue();
    }
}