package com.pmrodrigues.security.repository;

import com.pmrodrigues.security.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UserRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    UserRepository repository;

    @Test
    void findByEmail_whenUserExists_returnsUser() {
        em.persist(user("alice@test.com"));
        em.flush();

        var found = repository.findByEmail("alice@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void findByEmail_whenUserDoesNotExist_returnsEmpty() {
        var found = repository.findByEmail("nobody@test.com");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_isCaseSensitive() {
        em.persist(user("Case@test.com"));
        em.flush();

        assertThat(repository.findByEmail("case@test.com")).isEmpty();
        assertThat(repository.findByEmail("Case@test.com")).isPresent();
    }

    @Test
    void save_persistsAllFields() {
        var user = user("bob@test.com");
        user.setRoles(new HashSet<>(Set.of("ROLE_USER", "ROLE_ADMIN")));

        repository.save(user);
        em.flush();
        em.clear();

        var loaded = repository.findByEmail("bob@test.com").orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Test User");
        assertThat(loaded.isEnabled()).isTrue();
        assertThat(loaded.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void save_withDuplicateEmail_throwsException() {
        em.persist(user("dup@test.com"));
        em.flush();

        var duplicate = user("dup@test.com");
        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class, () -> {
                    repository.saveAndFlush(duplicate);
                });
    }

    @Test
    void findByEmail_whenUserIsDisabled_returnsUser() {
        var user = user("inactive@test.com");
        user.setEnabled(false);
        em.persist(user);
        em.flush();

        var found = repository.findByEmail("inactive@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().isEnabled()).isFalse();
    }

    private User user(String email) {
        return new User()
        .setEmail(email)
        .setPassword("hashed-password")
        .setName("Test User")
        .setEnabled(true)
        .setRoles(new HashSet<>(Set.of("ROLE_USER")));
    }
}
