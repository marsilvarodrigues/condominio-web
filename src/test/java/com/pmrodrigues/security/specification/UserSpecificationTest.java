package com.pmrodrigues.security.specification;

import com.pmrodrigues.security.model.User;
import com.pmrodrigues.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UserSpecificationTest {

    @Autowired TestEntityManager em;
    @Autowired UserRepository repository;

    @BeforeEach
    void setUp() {
        em.persist(user("Alice Smith", "alice@test.com", true, Set.of("ROLE_USER")));
        em.persist(user("Bob Jones", "bob@test.com", false, Set.of("ROLE_ADMIN")));
        em.persist(user("Carol Silva", "carol@test.com", true, Set.of("ROLE_USER", "ROLE_ADMIN")));
        em.flush();
        em.clear();
    }

    @Test
    void hasNome_matchesSubstringCaseInsensitive() {
        var results = repository.findAll(UserSpecification.hasNome("alice"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Alice Smith");
    }

    @Test
    void hasNome_withNull_returnsAll() {
        assertThat(repository.findAll(UserSpecification.hasNome(null))).hasSize(3);
    }

    @Test
    void hasNome_withBlank_returnsAll() {
        assertThat(repository.findAll(UserSpecification.hasNome("  "))).hasSize(3);
    }

    @Test
    void hasEmail_matchesSubstringCaseInsensitive() {
        var results = repository.findAll(UserSpecification.hasEmail("BOB"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("bob@test.com");
    }

    @Test
    void hasEmail_withNull_returnsAll() {
        assertThat(repository.findAll(UserSpecification.hasEmail(null))).hasSize(3);
    }

    @Test
    void hasEnabled_true_returnsOnlyEnabledUsers() {
        var results = repository.findAll(UserSpecification.hasEnabled(true));
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(User::isEnabled);
    }

    @Test
    void hasEnabled_false_returnsOnlyDisabledUsers() {
        var results = repository.findAll(UserSpecification.hasEnabled(false));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("bob@test.com");
    }

    @Test
    void hasEnabled_withNull_returnsAll() {
        assertThat(repository.findAll(UserSpecification.hasEnabled(null))).hasSize(3);
    }

    @Test
    void hasRole_matchesUsersWithRole() {
        var results = repository.findAll(UserSpecification.hasRole("ROLE_ADMIN"));
        assertThat(results).hasSize(2);
        assertThat(results).extracting(User::getEmail)
                .containsExactlyInAnyOrder("bob@test.com", "carol@test.com");
    }

    @Test
    void hasRole_withNull_returnsAll() {
        assertThat(repository.findAll(UserSpecification.hasRole(null))).hasSize(3);
    }

    private User user(String name, String email, boolean enabled, Set<String> roles) {
        return new User()
                .setName(name)
                .setEmail(email)
                .setPassword("hashed-password")
                .setEnabled(enabled)
                .setRoles(new HashSet<>(roles));
    }
}
