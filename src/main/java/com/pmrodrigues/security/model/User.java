package com.pmrodrigues.security.model;

import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity representing an application user with soft-delete support and an email-based activation flow.
 * Subclasses (e.g. {@code Pessoa}) use JOINED inheritance to extend this entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Email
    @NotBlank
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @NotBlank
    @NotNull
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> roles = new HashSet<>(Set.of("ROLE_USER"));

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_condominios",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "condominio_id")
    )
    @Builder.Default
    private Set<Condominio> condominios = new HashSet<>();

    @Column(name = "activation_token")
    private String activationToken;

    @Column(name = "activation_token_expiry")
    private LocalDateTime activationTokenExpiry;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String rawPassword;

    /**
     * Generates a temporary password, a UUID activation token valid for 24 hours, and disables the account until it is activated; runs only when no password has been set yet.
     */
    @PrePersist
    public void prePersist() {
        if (this.password == null || this.password.isBlank()) {
            var random = new SecureRandom();
            var bytes = new byte[18]; // 18 random bytes → 24 Base64 URL-safe chars (144 bits of entropy)
            random.nextBytes(bytes);
            String tempPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            this.rawPassword = tempPassword;
            this.password = new BCryptPasswordEncoder().encode(tempPassword);
            this.activationToken = UUID.randomUUID().toString();
            this.activationTokenExpiry = LocalDateTime.now().plusHours(24);
            this.enabled = false;
        }
    }
}
