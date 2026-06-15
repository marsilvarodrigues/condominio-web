package com.pmrodrigues.cobranca.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA entity representing a condominium billing charge issued for a single apartment unit.
 *
 * <p>Each instance tracks the full lifecycle of a charge: from creation, through Asaas submission,
 * email dispatch, and final payment or cancellation.
 *
 * <p>Partitioned by {@code condominio_id} (HASH, 4 buckets) for query performance. Soft-deletes are
 * implemented via {@link SQLDelete} + {@link SQLRestriction}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "cobrancas")
@SQLDelete(sql = "UPDATE cobrancas SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class Cobranca {

  /** Surrogate primary key — simple BIGSERIAL auto-increment. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Multi-tenancy discriminator. Not directly accessible — set via {@link TenantContext} in {@link
   * #prePersist()}. Getter/setter suppressed to prevent bypassing the tenant mechanism.
   */
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "condominio_id", nullable = false)
  private Condominio condominio;

  /** The apartment unit this charge is issued for. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "apartamento_id", nullable = false)
  private Apartamento apartamento;

  /**
   * Reference to the {@code CotaRateio} that originated this charge. Stored as a plain FK column
   * (not a JPA association) to keep cross-module coupling minimal.
   */
  @Column(name = "cota_rateio_id", nullable = false)
  private Long cotaRateioId;

  /**
   * ID of the {@code Morador} (resident) linked to this charge. May be {@code null} if the
   * apartment has no resident at generation time.
   */
  @Column(name = "morador_id")
  private Long moradorId;

  /** Charge amount in BRL. */
  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  /** Payment due date. */
  @Column(nullable = false)
  private LocalDate vencimento;

  /** Current lifecycle status of this charge. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusCobranca status;

  /** Asaas payment external ID (e.g. {@code pay_000123456789}). */
  @Column(name = "asaas_id", length = 100)
  private String asaasId;

  /** Asaas customer external ID (e.g. {@code cus_000123456789}). */
  @Column(name = "asaas_customer_id", length = 100)
  private String asaasCustomerId;

  /** URL to download the boleto PDF from Asaas. */
  @Column(name = "boleto_url", length = 500)
  private String boletoUrl;

  /** Boleto barcode / nosso número returned by Asaas. */
  @Column(name = "boleto_codigo_barras", length = 100)
  private String boletoCodBarras;

  /** Base64-encoded PNG of the Pix QR Code. */
  @Column(name = "pix_qr_code_base64", columnDefinition = "TEXT")
  private String pixQrCodeBase64;

  /** Pix copia-e-cola (EMV payload string). */
  @Column(name = "pix_copia_cola")
  private String pixCopiaCola;

  /** Whether the billing email has been sent to the resident. */
  @Column(name = "email_enviado", nullable = false)
  @Builder.Default
  private boolean emailEnviado = false;

  /** Timestamp when the billing email was last sent. */
  @Column(name = "email_enviado_em")
  private LocalDateTime emailEnviadoEm;

  /** Timestamp when the payment was confirmed (set by webhook). */
  @Column(name = "pago_em")
  private LocalDateTime pagoEm;

  /** Soft-delete flag — set to {@code true} by {@link SQLDelete}, never by application code. */
  @Column(nullable = false)
  @Builder.Default
  private boolean deleted = false;

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

  /**
   * Sets {@code condominio} from {@link TenantContext} before first persistence, and defaults
   * {@code status} to {@link StatusCobranca#PENDENTE} if not set.
   */
  @PrePersist
  protected void prePersist() {
    Long condominioId = TenantContext.getCondominioId();
    if (condominioId != null) {
      var c = new Condominio();
      c.setId(condominioId);
      this.condominio = c;
    }
    if (this.status == null) {
      this.status = StatusCobranca.PENDENTE;
    }
  }
}
