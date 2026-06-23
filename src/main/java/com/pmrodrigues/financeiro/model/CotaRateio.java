package com.pmrodrigues.financeiro.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;
import com.pmrodrigues.commons.tenant.TenantScoped;

/**
 * Stores the calculated quota assigned to a single apartment unit for a given {@link Despesa}.
 *
 * <p>This entity has no soft delete and no auditing — it is replaced wholesale whenever a
 * recalculation runs (old records for the same {@code despesa} are deleted before inserting new
 * ones).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "cotas_rateio")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
public class CotaRateio implements TenantScoped {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "condominio_id", nullable = false)
  private Condominio condominio;

  @Override
  public Long getCondominioId() {
    return condominio != null ? condominio.getId() : null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "despesa_id", nullable = false)
  private Despesa despesa;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "apartamento_id", nullable = false)
  private Apartamento apartamento;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rateio_execucao_id", nullable = false)
  private RateioExecucao rateioExecucao;

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
