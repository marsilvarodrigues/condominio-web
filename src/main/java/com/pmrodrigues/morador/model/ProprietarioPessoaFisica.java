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
import org.hibernate.validator.constraints.br.CPF;

/**
 * Concrete JPA entity for a property owner who is a natural person (pessoa física).
 * Stored as a SINGLE_TABLE row in {@code pessoas} with discriminator value {@code PROP_PF}.
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "proprietario_pf")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("PROP_PF")
public class ProprietarioPessoaFisica extends Proprietario {

    /** CPF — Brazilian natural-person tax identifier (up to 14 chars including punctuation). */
    @Column(length = 14)
    @CPF
    private String cpf;
}
