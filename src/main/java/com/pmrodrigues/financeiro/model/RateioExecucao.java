package com.pmrodrigues.financeiro.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDateTime;
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
 * Immutable audit record of a single rateio execution for one {@link Despesa}.
 *
 * <p>This entity has no soft delete — once written it is never modified or removed. It is
 * partitioned by {@code condominio_id} for query performance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "rateio_execucoes")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
public class RateioExecucao implements TenantScoped {

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

  @Column(name = "grupo_despesa_id", nullable = false)
  private Long grupoDespesaId;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_execucao", nullable = false, length = 15)
  private TipoExecucaoRateio tipoExecucao;

  @Column(name = "data_execucao", nullable = false)
  @Builder.Default
  private LocalDateTime dataExecucao = LocalDateTime.now();

  @Column(name = "despesa_total", nullable = false, precision = 15, scale = 2)
  private BigDecimal despesaTotal;

  @Column(name = "total_unidades")
  private Integer totalUnidades;

  @Column(name = "total_cotas", precision = 15, scale = 2)
  private BigDecimal totalCotas;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private StatusExecucaoRateio status;

  @Column(name = "erro_mensagem", columnDefinition = "TEXT")
  private String erroMensagem;

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
