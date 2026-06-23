package com.pmrodrigues.condominio.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.morador.model.Pessoa;
import com.pmrodrigues.morador.model.Proprietario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.pmrodrigues.commons.tenant.TenantScoped;

/**
 * JPA entity representing an apartment unit belonging to a building block within a condominium,
 * with soft-delete and multi-tenancy support.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "apartamentos")
@SQLDelete(sql = "UPDATE apartamentos SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@BatchSize(size = 20)
@EntityListeners(AuditingEntityListener.class)
public class Apartamento implements TenantScoped {

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
  @JoinColumn(name = "bloco_id", nullable = false)
  private Bloco bloco;

  @Column(nullable = false, length = 20)
  private String numero;

  @Column(nullable = false)
  private BigDecimal areaConstruida;

  @Column(precision = 10, scale = 6)
  private BigDecimal fracaoIdeal;

  @Column
  private Integer andar;

  @NotAudited
  @BatchSize(size = 20)
  @OneToMany(mappedBy = "apartamento", fetch = FetchType.LAZY)
  @Builder.Default
  private List<Pessoa> moradores = new ArrayList<>();

  @NotAudited
  @ManyToMany(mappedBy = "apartamentos", fetch = FetchType.LAZY)
  @Builder.Default
  private Set<Proprietario> proprietarios = new HashSet<>();

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
   * Sets the {@code condominio} from {@link com.pmrodrigues.commons.tenant.TenantContext} before
   * the entity is first persisted.
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
