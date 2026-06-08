package com.pmrodrigues.morador.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * Concrete JPA entity for a property owner that is a legal entity (pessoa jurídica). Stored as a
 * SINGLE_TABLE row in {@code pessoas} with discriminator value {@code PROP_PJ}.
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "proprietario_pj")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("PROP_PJ")
public class ProprietarioPessoaJuridica extends Proprietario {

  /** CNPJ — Brazilian legal-entity tax identifier (up to 18 chars including punctuation). */
  @Column(length = 18)
  @CNPJ
  private String cnpj;

  /** Corporate name (razão social). */
  @Column(length = 255)
  private String razaoSocial;
}
