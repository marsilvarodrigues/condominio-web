package com.pmrodrigues.morador.model;

import com.pmrodrigues.commons.config.TenantFilterAspect;
import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.security.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Filter;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Abstract JPA entity representing a person (natural or legal) residing in a condominium. Extends
 * {@link User} via JOINED inheritance so that every {@code Pessoa} is also a system user. The
 * sub-hierarchy within the {@code pessoas} table uses SINGLE_TABLE with discriminator column {@code
 * pessoa_tipo}.
 *
 * <p>Fields inherited from {@link User} (stored in {@code users} table): {@code id}, {@code email},
 * {@code password}, {@code name}, {@code enabled}, {@code deleted}, {@code roles}, {@code
 * condominios}, {@code createdAt}, {@code updatedAt}.
 *
 * <p>Soft-delete is inherited from {@code User}: {@code @SQLRestriction("deleted = false")} uses
 * the {@code deleted} column in the {@code users} table. The {@link
 * TenantFilterAspect#CONDOMINIO_FILTER} applies to the {@code condominio_id} column in the {@code
 * pessoas} table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "pessoas")
@PrimaryKeyJoinColumn(name = "id")
@Inheritance(strategy = InheritanceType.JOINED)
@Filter(name = TenantFilterAspect.CONDOMINIO_FILTER, condition = "condominio_id = :condominioId")
@EntityListeners(AuditingEntityListener.class)
public abstract class Pessoa extends User {

  /**
   * Multi-tenancy discriminator. Not directly accessible — set via {@link TenantContext} in {@link
   * #prePersist()}. The getter/setter are suppressed to avoid bypassing the tenant mechanism.
   */
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "condominio_id", nullable = false)
  private Condominio condominio;

  /**
   * Discriminator column — set in {@link #prePersist()} from the {@link DiscriminatorValue}
   * annotation of the concrete subclass. Workaround for Hibernate 6 not inserting the value
   * automatically for JOINED inheritance when the parent class is also a JOINED subtype.
   */
  @Column(name = "pessoa_tipo", nullable = false, updatable = false)
  private String pessoaTipo;

  /**
   * Current apartment assignment (morador relationship). Null means the person is not currently
   * assigned to any apartment.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "apartamento_id")
  private Apartamento apartamento;

  /** Phone number. */
  @Column(length = 20)
  private String telefone;

  /** Spring Data audit: username that created this pessoa record. */
  @CreatedBy
  @Column(name = "created_by", updatable = false)
  private String createdBy;

  /** Spring Data audit: username that last modified this pessoa record. */
  @LastModifiedBy
  @Column(name = "updated_by")
  private String updatedBy;

  /**
   * Sets {@code condominio} from {@link TenantContext} and populates {@code pessoaTipo} from the
   * {@link DiscriminatorValue} annotation of the concrete subclass before first persistence.
   */
  @Override
  @PrePersist
  public void prePersist() {
    super.prePersist();
    Long condominioId = TenantContext.getCondominioId();
    if (condominioId != null) {
      var c = new Condominio();
      c.setId(condominioId);
      this.condominio = c;
    }
    DiscriminatorValue dv = this.getClass().getAnnotation(DiscriminatorValue.class);
    if (dv != null) {
      this.pessoaTipo = dv.value();
    }
  }
}
