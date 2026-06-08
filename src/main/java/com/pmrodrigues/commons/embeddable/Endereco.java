package com.pmrodrigues.commons.embeddable;

import com.pmrodrigues.commons.model.Estado;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * JPA embeddable value object representing a Brazilian postal address, embedded inside owning
 * entities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Embeddable
public class Endereco {

  @Column(nullable = false)
  private String logradouro;

  @Column(nullable = false, length = 8)
  private String cep;

  @Column(nullable = false)
  private String cidade;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "estado_id", nullable = false)
  private Estado estado;
}
