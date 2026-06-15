package com.pmrodrigues.financeiro.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Records a single credit or debit movement on a FundoReserva, with justification for debits. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "fundo_reserva_movimentacao")
@SQLDelete(sql = "UPDATE fundo_reserva_movimentacao SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class FundoReservaMovimentacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "condominio_id", nullable = false)
  private Condominio condominio;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fundo_reserva_id", nullable = false)
  private FundoReserva fundoReserva;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @Column(columnDefinition = "TEXT")
  private String justificativa;

  @Column(name = "data_movimentacao", nullable = false)
  private LocalDate dataMovimentacao;

  @Column(nullable = false)
  @Builder.Default
  private boolean deleted = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /**
   * Sets the {@code condominio} from {@link TenantContext} before the entity is first persisted.
   */
  @PrePersist
  protected void prePersist() {
    Long condominioId = TenantContext.getCondominioId();
    if (condominioId != null) {
      var c = new Condominio();
      c.setId(condominioId);
      this.condominio = c;
    }
  }
}
