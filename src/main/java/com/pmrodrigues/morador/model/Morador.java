package com.pmrodrigues.morador.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.envers.Audited;

/**
 * Concrete JPA entity for a condominium resident (morador), identified by CPF. Stored as a
 * SINGLE_TABLE row in {@code pessoas} with discriminator value {@code MORADOR}.
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Audited
@Entity
@Table(name = "moradores")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("MORADOR")
public class Morador extends Pessoa {

  /** CPF — Brazilian natural-person tax identifier (up to 14 chars including punctuation). */
  @Column(length = 14)
  private String cpf;

  @Override
  @PrePersist
  public void prePersist() {
    super.prePersist();
    getRoles().add("ROLE_MORADOR");
  }
}
