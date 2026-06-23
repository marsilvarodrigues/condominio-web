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
import com.pmrodrigues.commons.tenant.TenantScoped;

/**
 * Single transaction line from an imported bank statement, partitioned by condominio_id. {@code
 * lancamento} is populated when the item is matched to an existing entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "itens_extrato")
@SQLDelete(sql = "UPDATE itens_extrato SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public class ItemExtrato implements TenantScoped {

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
  @JoinColumn(name = "extrato_importacao_id", nullable = false)
  private ExtratoImportacao extratoImportacao;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lancamento_id")
  private LancamentoBancario lancamento;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "item_orcamento_id")
  private ItemOrcamento itemOrcamento;

  @Column(name = "data_lancamento", nullable = false)
  private LocalDate dataLancamento;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoLancamento tipo;

  @Column(length = 500)
  private String descricao;

  @Column(name = "numero_documento", length = 100)
  private String numeroDocumento;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  @Builder.Default
  private StatusItemExtrato status = StatusItemExtrato.PENDENTE;

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
