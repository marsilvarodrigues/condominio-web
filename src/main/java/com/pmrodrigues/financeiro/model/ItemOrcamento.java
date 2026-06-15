package com.pmrodrigues.financeiro.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Condominio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

/**
 * A single budget line item linking a PlanoContas category to an OrcamentoAnual with predicted and
 * realized values.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "item_orcamento")
@SQLDelete(sql = "UPDATE item_orcamento SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class ItemOrcamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "condominio_id", nullable = false)
  private Condominio condominio;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "orcamento_anual_id", nullable = false)
  private OrcamentoAnual orcamentoAnual;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plano_contas_id", nullable = false)
  private PlanoContas planoContas;

  @OneToMany(mappedBy = "itemOrcamento", fetch = FetchType.LAZY)
  @Builder.Default
  private List<ItemExtrato> itensExtrato = new ArrayList<>();

  @Column(name = "valor_previsto", nullable = false, precision = 15, scale = 2)
  private BigDecimal valorPrevisto;

  @Column(name = "valor_realizado", nullable = false, precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal valorRealizado = BigDecimal.ZERO;

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
