package com.pmrodrigues.cobranca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Per-condominium billing configuration stored in the database.
 *
 * <p>One row per condominium. When no row exists for a given tenant, {@link
 * com.pmrodrigues.cobranca.service.CobrancaService} falls back to the defaults from {@link
 * com.pmrodrigues.cobranca.config.CobrancaProperties}.
 *
 * <p>Not partitioned — the table has one row per condominium and is small enough that partitioning
 * would add no benefit. Tenant isolation is enforced by the unique constraint on {@code
 * condominio_id}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cobranca_configuracoes")
@EntityListeners(AuditingEntityListener.class)
public class CobrancaConfiguracao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The condominium this configuration belongs to. One row per condominium (UNIQUE). */
  @Column(name = "condominio_id", nullable = false, unique = true)
  private Long condominioId;

  /** Default number of days from today used when computing the due date. */
  @Builder.Default
  @Column(name = "vencimento_dias", nullable = false)
  private int vencimentoDias = 10;

  /** Monthly late-interest rate applied after the due date (percentage). */
  @Builder.Default
  @Column(name = "juros_mora_percent", nullable = false, precision = 5, scale = 2)
  private BigDecimal jurosMoraPercent = BigDecimal.valueOf(1.0);

  /** One-time fine percentage applied when a charge becomes overdue (percentage). */
  @Builder.Default
  @Column(name = "multa_percent", nullable = false, precision = 5, scale = 2)
  private BigDecimal multaPercent = BigDecimal.valueOf(2.0);

  /** Default charge description sent to Asaas and displayed in billing emails. */
  @Builder.Default
  @Column(name = "descricao_padrao", nullable = false, length = 255)
  private String descricaoPadrao = "Taxa condominial";

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @CreatedBy
  @Column(name = "created_by", updatable = false)
  private String createdBy;

  @LastModifiedBy
  @Column(name = "updated_by")
  private String updatedBy;
}
