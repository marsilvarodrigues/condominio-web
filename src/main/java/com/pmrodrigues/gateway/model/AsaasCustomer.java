package com.pmrodrigues.gateway.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Caches the Asaas customer ID for a given {@code Pessoa} (identified by {@code pessoa_id}).
 *
 * <p>Avoids creating duplicate Asaas customers on every charge generation by persisting
 * the mapping locally. This table is not tenant-scoped because customer IDs in Asaas are
 * tied to the condominium account, not to individual units.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "asaas_customers")
public class AsaasCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Primary key of the related {@code Pessoa} entity (cross-module FK by convention). */
    @Column(name = "pessoa_id", nullable = false, unique = true)
    private Long pessoaId;

    /** Asaas customer ID (e.g. {@code cus_000123456789}). */
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
