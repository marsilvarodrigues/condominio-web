package com.pmrodrigues.morador.model;

import com.pmrodrigues.condominio.model.Apartamento;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

/**
 * Abstract JPA entity for a property owner (proprietário). Extends {@link Pessoa} and adds a
 * many-to-many relationship with {@link Apartamento} via the {@code proprietario_apartamentos} join
 * table.
 *
 * <p>Concrete subclasses are {@link ProprietarioPessoaFisica} (CPF) and {@link
 * ProprietarioPessoaJuridica} (CNPJ + razão social).
 *
 * <p>The {@code ROLE_PROPRIETARIO} role is assigned at persist time in addition to the {@code
 * ROLE_MORADOR} role already set by {@link Pessoa#prePersist()}.
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "proprietarios")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("PROP")
public abstract class Proprietario extends Pessoa {

  /**
   * Apartments owned by this proprietário (many-to-many through {@code proprietario_apartamentos}).
   */
  @BatchSize(size = 20)
  @ManyToMany
  @JoinTable(
      name = "proprietario_apartamentos",
      joinColumns = @JoinColumn(name = "proprietario_id"),
      inverseJoinColumns = @JoinColumn(name = "apartamento_id"))
  private Set<Apartamento> apartamentos = new HashSet<>();

  /**
   * Adds the {@code ROLE_PROPRIETARIO} role at persist time in addition to the roles already
   * configured by {@link Pessoa#prePersist()}.
   */
  @Override
  @PrePersist
  public void prePersist() {
    super.prePersist();
    getRoles().add("ROLE_PROPRIETARIO");
  }
}
